/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2017 Thomas Cashman
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
package org.mini2Dx.miniscript.kotlin;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase.CompiledKotlinScript;
import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;
import org.mini2Dx.miniscript.core.exception.ScriptExecutorUnavailableException;
import org.mini2Dx.miniscript.core.util.ReadWriteBlockingQueue;
import org.mini2Dx.miniscript.core.util.ReadWriteMap;

/**
 * An implementation of {@link ScriptExecutorPool} for Kotlin-based scripts
 */
public class KotlinScriptExecutorPool implements ScriptExecutorPool<CompiledKotlinScript> {
	private final Map<Integer, GameScript<CompiledKotlinScript>> scripts = new ReadWriteMap<>();
	private final Map<String, Integer> filepathToScriptId = new ReadWriteMap<String, Integer>();
	private final BlockingQueue<ScriptExecutor<CompiledKotlinScript>> executors;
	private final GameScriptingEngine gameScriptingEngine;
	private final SynchronizedObjectPool<KotlinEmbeddedScriptInvoker> embeddedScriptInvokerPool = new SynchronizedObjectPool<KotlinEmbeddedScriptInvoker>() {
		@Override
		protected KotlinEmbeddedScriptInvoker construct() {
			return new KotlinEmbeddedScriptInvoker(gameScriptingEngine, KotlinScriptExecutorPool.this);
		}
	};

	public KotlinScriptExecutorPool(GameScriptingEngine gameScriptingEngine, int poolSize) {
		this.gameScriptingEngine = gameScriptingEngine;
		executors = new ReadWriteBlockingQueue<>(poolSize);

		for (int i = 0; i < poolSize; i++) {
			executors.offer(new KotlinScriptExecutor(this));
		}
	}

	@Override
	public void release(ScriptExecutor<CompiledKotlinScript> executor) {
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

	GameScript<CompiledKotlinScript> getScript(int id) {
		return scripts.get(id);
	}

	@Override
	public int preCompileScript(String filepath, String scriptContent) throws InsufficientCompilersException {
		ScriptExecutor<CompiledKotlinScript> executor = executors.poll();
		if (executor == null) {
			throw new InsufficientCompilersException();
		}
		GameScript<CompiledKotlinScript> script = executor.compile(scriptContent);
		executor.release();
		scripts.put(script.getId(), script);
		filepathToScriptId.put(filepath, script.getId());
		return script.getId();
	}

	@Override
	public ScriptExecutionTask<?> execute(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		ScriptExecutor<CompiledKotlinScript> executor = allocateExecutor();
		if (executor == null) {
			throw new ScriptExecutorUnavailableException(scriptId);
		}
		if(!scripts.containsKey(scriptId)) {
			executor.release();
			throw new NoSuchScriptException(scriptId);
		}
		return new ScriptExecutionTask<CompiledKotlinScript>(gameScriptingEngine, executor,
				scriptId, scripts.get(scriptId), scriptBindings, invocationListener);
	}

	private ScriptExecutor<CompiledKotlinScript> allocateExecutor() {
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

	public SynchronizedObjectPool<KotlinEmbeddedScriptInvoker> getEmbeddedScriptInvokerPool() {
		return embeddedScriptInvokerPool;
	}
}
