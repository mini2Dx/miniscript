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
package org.mini2Dx.miniscript.python;

import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;
import org.mini2Dx.miniscript.core.exception.ScriptExecutorUnavailableException;
import org.python.core.PyCode;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of {@link ScriptExecutorPool} for Python-based scripts
 */
public class PythonScriptExecutorPool implements ScriptExecutorPool<PyCode> {
	private final Map<Integer, GameScript<PyCode>> scripts = new ConcurrentHashMap<Integer, GameScript<PyCode>>();
	private final Map<String, Integer> filepathToScriptId = new ConcurrentHashMap<String, Integer>();
	private final BlockingQueue<ScriptExecutor<PyCode>> executors;
	private final GameScriptingEngine gameScriptingEngine;
	private final SynchronizedObjectPool<PythonEmbeddedScriptInvoker> embeddedScriptInvokerPool = new SynchronizedObjectPool<PythonEmbeddedScriptInvoker>() {
		@Override
		protected PythonEmbeddedScriptInvoker construct() {
			return new PythonEmbeddedScriptInvoker(gameScriptingEngine, PythonScriptExecutorPool.this);
		}
	};

	public PythonScriptExecutorPool(GameScriptingEngine gameScriptingEngine, int poolSize) {
		this.gameScriptingEngine = gameScriptingEngine;
		executors = new ArrayBlockingQueue<ScriptExecutor<PyCode>>(poolSize);

		for (int i = 0; i < poolSize; i++) {
			executors.offer(new PythonScriptExecutor(this));
		}
	}

	@Override
	public int getCompiledScriptId(String filepath) {
		return filepathToScriptId.getOrDefault(filepath, -1);
	}

	@Override
	public int preCompileScript(String filepath, String scriptContent) throws InsufficientCompilersException {
		ScriptExecutor<PyCode> executor = executors.poll();
		if (executor == null) {
			throw new InsufficientCompilersException();
		}
		GameScript<PyCode> script = executor.compile(scriptContent);
		executor.release();
		scripts.put(script.getId(), script);
		filepathToScriptId.put(filepath, script.getId());
		return script.getId();
	}

	@Override
	public ScriptExecutionTask<?> execute(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		ScriptExecutor<PyCode> executor = allocateExecutor();
		if (executor == null) {
			throw new ScriptExecutorUnavailableException(scriptId);
		}
		if(!scripts.containsKey(scriptId)) {
			executor.release();
			throw new NoSuchScriptException(scriptId);
		}
		return new ScriptExecutionTask<PyCode>(gameScriptingEngine, executor, scriptId, scripts.get(scriptId), scriptBindings,
				invocationListener);
	}

	@Override
	public void release(ScriptExecutor<PyCode> executor) {
		try {
			executors.put(executor);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private ScriptExecutor<PyCode> allocateExecutor() {
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

	GameScript<PyCode> getScript(int scriptId) {
		return scripts.get(scriptId);
	}

	public SynchronizedObjectPool<PythonEmbeddedScriptInvoker> getEmbeddedScriptInvokerPool() {
		return embeddedScriptInvokerPool;
	}
}
