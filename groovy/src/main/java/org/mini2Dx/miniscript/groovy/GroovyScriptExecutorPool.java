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
package org.mini2Dx.miniscript.groovy;

import groovy.lang.Script;
import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;
import org.mini2Dx.miniscript.core.exception.ScriptExecutorUnavailableException;
import org.mini2Dx.miniscript.core.util.ReadWriteBlockingQueue;
import org.mini2Dx.miniscript.core.util.ReadWriteMap;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * An implementation of {@link ScriptExecutorPool} for Groovy-based scripts
 */
public class GroovyScriptExecutorPool implements ScriptExecutorPool<Script> {
	private final Map<Integer, GameScript<Script>> scripts = new ReadWriteMap<>();
	private final Map<String, Integer> filepathToScriptId = new ReadWriteMap<String, Integer>();
	private final Map<Integer, String> scriptIdToFilepath = new ReadWriteMap<Integer, String>();
	private final BlockingQueue<ScriptExecutor<Script>> executors;
	private final GameScriptingEngine gameScriptingEngine;
	private final SynchronizedObjectPool<GroovyEmbeddedScriptInvoker> embeddedScriptInvokerPool = new SynchronizedObjectPool<GroovyEmbeddedScriptInvoker>() {
		@Override
		protected GroovyEmbeddedScriptInvoker construct() {
			return new GroovyEmbeddedScriptInvoker(gameScriptingEngine, GroovyScriptExecutorPool.this);
		}
	};

	public GroovyScriptExecutorPool(GameScriptingEngine gameScriptingEngine, int poolSize) {
		this.gameScriptingEngine = gameScriptingEngine;
		executors = new ReadWriteBlockingQueue<>(poolSize);

		for (int i = 0; i < poolSize; i++) {
			executors.offer(new GroovyScriptExecutor(this));
		}
	}

	@Override
	public void release(ScriptExecutor<Script> executor) {
		try {
			executors.put(executor);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getCompiledScriptId(String filepath) {
		return filepathToScriptId.getOrDefault(filepath, -1);
	}

	@Override
	public String getCompiledScriptPath(int scriptId) {
		return scriptIdToFilepath.get(scriptId);
	}

	@Override
	public int preCompileScript(String filepath, String scriptContent) throws InsufficientCompilersException {
		ScriptExecutor<Script> executor = executors.poll();
		if (executor == null) {
			throw new InsufficientCompilersException();
		}
		GameScript<Script> script = executor.compile(scriptContent);
		executor.release();
		scripts.put(script.getId(), script);
		filepathToScriptId.put(filepath, script.getId());
		scriptIdToFilepath.put(script.getId(), filepath);
		return script.getId();
	}

	@Override
	public ScriptExecutionTask<?> execute(int taskId, int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener, boolean syncCall) {
		ScriptExecutor<Script> executor = allocateExecutor();
		if (executor == null) {
			throw new ScriptExecutorUnavailableException(scriptId);
		}
		if(!scripts.containsKey(scriptId)) {
			executor.release();
			throw new NoSuchScriptException(scriptId);
		}
		return new ScriptExecutionTask<Script>(taskId, gameScriptingEngine, executor, scriptId,
				scripts.get(scriptId), scriptBindings, invocationListener, syncCall);
	}

	private ScriptExecutor<Script> allocateExecutor() {
		try {
			return executors.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public GameScriptingEngine getGameScriptingEngine() {
		return gameScriptingEngine;
	}

	public SynchronizedObjectPool<GroovyEmbeddedScriptInvoker> getEmbeddedScriptInvokerPool() {
		return embeddedScriptInvokerPool;
	}

	GameScript<Script> getScript(int scriptId) {
		return scripts.get(scriptId);
	}
}
