/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016 Thomas Cashman
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.mini2Dx.miniscript.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;
import org.mini2Dx.miniscript.core.exception.ScriptExecutorUnavailableException;
import org.mini2Dx.miniscript.core.util.ReadWriteBlockingQueue;
import org.mini2Dx.miniscript.core.util.ReadWriteMap;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * An implementation of {@link ScriptExecutorPool} for Lua scripts
 */
public class LuaScriptExecutorPool implements ScriptExecutorPool<LuaValue> {
	private final Map<Long, Globals> threadCompilers = new ReadWriteMap<>();
	private final Map<Integer, GameScript<LuaValue>> scripts = new ReadWriteMap<Integer, GameScript<LuaValue>>();
	private final Map<String, Integer> filepathToScriptId = new ReadWriteMap<String, Integer>();
	private final BlockingQueue<ScriptExecutor<LuaValue>> executors;
	private final GameScriptingEngine gameScriptingEngine;
	private final ClasspathScriptProvider classpathScriptProvider;
	private final boolean sandboxed;
	private final SynchronizedObjectPool<LuaEmbeddedScriptInvoker> embeddedScriptInvokerPool = new SynchronizedObjectPool<LuaEmbeddedScriptInvoker>() {
		@Override
		protected LuaEmbeddedScriptInvoker construct() {
			return new LuaEmbeddedScriptInvoker(gameScriptingEngine, LuaScriptExecutorPool.this);
		}
	};
	
	private Globals sandboxedGlobals;

	public LuaScriptExecutorPool(GameScriptingEngine gameScriptingEngine,
	                             ClasspathScriptProvider classpathScriptProvider,
	                             int poolSize, boolean sandboxed) {
		this.gameScriptingEngine = gameScriptingEngine;
		this.classpathScriptProvider = classpathScriptProvider;
		this.sandboxed = sandboxed;

		if(classpathScriptProvider.getTotalScripts() > 0) {
			for(int i = 0; i < classpathScriptProvider.getTotalScripts(); i++) {
				GameScript.offsetIds(i - 1);
				PerThreadClasspathGameScript testing = new PerThreadClasspathGameScript<LuaValue>((LuaValue) classpathScriptProvider.getClasspathScript(i));
				int key = testing.getId();
				scripts.put(key, testing);
				filepathToScriptId.put(classpathScriptProvider.getFilepath(key), key);
			}
		}
		
		executors = new ReadWriteBlockingQueue<>(poolSize);

		for (int i = 0; i < poolSize; i++) {
			executors.offer(new LuaScriptExecutor(this));
		}
		
		if(sandboxed) {
			//Create sandboxed compiler
			sandboxedGlobals = new Globals();
			sandboxedGlobals.load(new JseBaseLib());
			sandboxedGlobals.load(new PackageLib());
			sandboxedGlobals.load(new StringLib());

			// To load scripts, we occasionally need a math library in addition to compiler support.
			// To limit scripts using the debug library, they must be closures, so we only install LuaC.
			sandboxedGlobals.load(new JseMathLib());
			LoadState.install(sandboxedGlobals);
			LuaC.install(sandboxedGlobals);
		}
	}

	@Override
	public int getCompiledScriptId(String filepath) {
		return filepathToScriptId.getOrDefault(filepath, -1);
	}

	@Override
	public int preCompileScript(String filepath, String scriptContent) throws InsufficientCompilersException {
		PerThreadGameScript<LuaValue> script = new PerThreadGameScript<LuaValue>(scriptContent);
		scripts.put(script.getId(), script);
		filepathToScriptId.put(filepath, script.getId());
		return script.getId();
	}

	@Override
	public ScriptExecutionTask<?> execute(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		ScriptExecutor<LuaValue> executor = allocateExecutor();
		if (executor == null) {
			throw new ScriptExecutorUnavailableException(scriptId);
		}
		if(!scripts.containsKey(scriptId)) {
			executor.release();
			throw new NoSuchScriptException(scriptId);
		}
		return new ScriptExecutionTask<LuaValue>(gameScriptingEngine, executor, scriptId, scripts.get(scriptId),
				scriptBindings, invocationListener);
	}

	@Override
	public void release(ScriptExecutor<LuaValue> executor) {
		try {
			executors.put(executor);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public GameScriptingEngine getGameScriptingEngine() {
		return gameScriptingEngine;
	}

	GameScript<LuaValue> getScript(int id) {
		return scripts.get(id);
	}
	
	private ScriptExecutor<LuaValue> allocateExecutor() {
		try {
			return executors.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Globals getLocalGlobals() {
		long threadId = Thread.currentThread().getId();
		if (!threadCompilers.containsKey(threadId)) {
			if(sandboxed) {
				threadCompilers.put(threadId, createSandboxedGlobals());
			} else {
				threadCompilers.put(threadId, JsePlatform.standardGlobals());
			}
		}
		return threadCompilers.get(threadId);
	}

	public LuaValue compileWithGlobals(Globals globals, GameScript<LuaValue> gameScript) {
		if(gameScript instanceof GlobalGameScript) {
			return gameScript.getScript();
		} else if(gameScript instanceof PerThreadClasspathGameScript) {
			return compileClassPathGameScript(globals, (PerThreadClasspathGameScript) gameScript);
		} else {
			return compilePerThreadGameScriptWithGlobals(globals, (PerThreadGameScript) gameScript);
		}
	}

	private LuaValue compileClassPathGameScript(Globals globals, PerThreadClasspathGameScript<LuaValue> perThreadClasspathGameScript) {
		final LuaValue result = perThreadClasspathGameScript.compileInstance();
		result.initupvalue1(globals);
		return result;
	}

	private LuaValue compilePerThreadGameScriptWithGlobals(Globals globals, PerThreadGameScript<LuaValue> perThreadGameScript) {
		if(sandboxed) {
			return sandboxedGlobals.load(perThreadGameScript.getContent(), "main", globals);
		} else {
			return globals.load(perThreadGameScript.getContent());
		}
	}
	
	private Globals createSandboxedGlobals() {
		Globals result = new Globals();
		result.load(new JseBaseLib());
		result.load(new PackageLib());
		result.load(new Bit32Lib());
		result.load(new TableLib());
		result.load(new StringLib());
		result.load(new JseMathLib());
		result.load(new DebugLib());
		result.set("debug", LuaValue.NIL);
		return result;
	}

	public SynchronizedObjectPool<LuaEmbeddedScriptInvoker> getEmbeddedScriptInvokerPool() {
		return embeddedScriptInvokerPool;
	}
}
