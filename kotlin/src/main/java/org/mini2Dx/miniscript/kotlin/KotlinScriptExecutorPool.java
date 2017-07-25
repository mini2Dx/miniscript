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
package org.mini2Dx.miniscript.kotlin;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.mini2Dx.miniscript.core.GameScript;
import org.mini2Dx.miniscript.core.GameScriptingEngine;
import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionTask;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.ScriptExecutorPool;
import org.mini2Dx.miniscript.core.ScriptInvocationListener;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.ScriptExecutorUnavailableException;
import sun.font.Script;

import javax.script.ScriptException;

//import groovy.lang.Script;

/**
 * An implementation of {@link ScriptExecutorPool} for Kotlin-based scripts
 */
public class KotlinScriptExecutorPool implements ScriptExecutorPool<Object> {
	private final Map<Integer, GameScript<Object>> scripts = new ConcurrentHashMap<Integer, GameScript<Object>>();
	private final BlockingQueue<ScriptExecutor<Object>> executors;
	private final GameScriptingEngine gameScriptingEngine;

	public KotlinScriptExecutorPool(GameScriptingEngine gameScriptingEngine, int poolSize) {
		this.gameScriptingEngine = gameScriptingEngine;
		executors = new ArrayBlockingQueue<ScriptExecutor<Object>>(poolSize);

		for (int i = 0; i < poolSize; i++) {
			executors.offer(new KotlinScriptExecutor(this));
		}
	}

	@Override
	public void release(ScriptExecutor<Object> executor) {
		try {
			executors.put(executor);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int preCompileScript(String scriptContent) throws InsufficientCompilersException, ScriptException {
		ScriptExecutor<Object> executor = executors.poll();
		if (executor == null) {
			throw new InsufficientCompilersException();
		}
		GameScript<Object> script = executor.compile(scriptContent);
		executor.release();
		scripts.put(script.getId(), script);
		return script.getId();
	}

	@Override
	public ScriptExecutionTask<?> execute(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		ScriptExecutor<Object> executor = allocateExecutor();
		if (executor == null) {
			throw new ScriptExecutorUnavailableException(scriptId);
		}
		return new ScriptExecutionTask<Object>(gameScriptingEngine, executor, scripts.get(scriptId), scriptBindings,
				invocationListener);
	}

	private ScriptExecutor<Object> allocateExecutor() {
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
}
