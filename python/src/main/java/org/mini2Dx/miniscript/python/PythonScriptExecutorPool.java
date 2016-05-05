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

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionTask;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.ScriptExecutorPool;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.InsufficientExecutorsException;
import org.python.core.PyCode;

/**
 * An implementation of {@link ScriptExecutorPool} for Python-based scripts
 */
public class PythonScriptExecutorPool implements ScriptExecutorPool<PyCode> {
	private static final AtomicInteger SCRIPT_ID_GENERATOR = new AtomicInteger(0);

	private final Map<Integer, PyCode> scripts = new ConcurrentHashMap<Integer, PyCode>();
	private final BlockingQueue<ScriptExecutor<PyCode>> executors;
	
	public PythonScriptExecutorPool(int poolSize) {
		executors = new ArrayBlockingQueue<ScriptExecutor<PyCode>>(poolSize);
		
		for(int i = 0; i < poolSize; i++) {
			executors.offer(new PythonScriptExecutor(this));
		}
	}

	@Override
	public int preCompileScript(String scriptContent) throws InsufficientCompilersException {
		ScriptExecutor<PyCode> executor = allocateExecutor();
		if(executor == null) {
			throw new InsufficientCompilersException();
		}
		int scriptId = SCRIPT_ID_GENERATOR.incrementAndGet();
		PyCode script = executor.compile(scriptContent);
		scripts.put(scriptId, script);
		return scriptId;
	}

	@Override
	public ScriptExecutionTask<?> execute(int scriptId, ScriptBindings scriptBindings)
			throws InsufficientExecutorsException {
		ScriptExecutor<PyCode> executor = allocateExecutor();
		if(executor == null) {
			throw new InsufficientExecutorsException(scriptId);
		}
		return new ScriptExecutionTask<PyCode>(executor, scripts.get(scriptId), scriptBindings);
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
		return executors.poll();
	}
}
