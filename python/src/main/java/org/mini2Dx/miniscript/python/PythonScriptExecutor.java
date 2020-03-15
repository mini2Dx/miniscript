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
import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;
import org.python.core.*;
import org.python.util.InteractiveInterpreter;

/**
 * An implementation of {@link ScriptExecutor} for Python-based scripts
 */
public class PythonScriptExecutor implements ScriptExecutor<PyCode> {
	private final PythonScriptExecutorPool executorPool;
	private final InteractiveInterpreter pythonInterpreter;

	public PythonScriptExecutor(PythonScriptExecutorPool executorPool) {
		this.executorPool = executorPool;

		pythonInterpreter = new InteractiveInterpreter();
		pythonInterpreter.setErr(System.err);
		pythonInterpreter.setOut(System.out);
	}

	@Override
	public GameScript<PyCode> compile(String script) {
		return new GlobalGameScript<PyCode>(pythonInterpreter.compile(script));
	}

	@Override
	public ScriptExecutionResult execute(int scriptId, GameScript<PyCode> script, ScriptBindings bindings, boolean returnResult) throws Exception {
		final PyCode pythonScript = script.getScript();

		final PythonEmbeddedScriptInvoker embeddedScriptInvoker = executorPool.getEmbeddedScriptInvokerPool().allocate();
		embeddedScriptInvoker.setScriptBindings(bindings);
		embeddedScriptInvoker.setScriptExecutor(this);
		embeddedScriptInvoker.setParentScriptId(scriptId);

		for (String variableName : bindings.keySet()) {
			pythonInterpreter.set(variableName, bindings.get(variableName));
		}
		pythonInterpreter.set(ScriptBindings.SCRIPT_PARENT_ID_VAR, -1);
		pythonInterpreter.set(ScriptBindings.SCRIPT_ID_VAR, scriptId);
		pythonInterpreter.set(ScriptBindings.SCRIPT_INVOKE_VAR, embeddedScriptInvoker);

		try {
			pythonInterpreter.exec(pythonScript);
		} catch (PyException e) {
			if(e.getCause() instanceof ScriptSkippedException) {
				throw new ScriptSkippedException();
			} else {
				throw e;
			}
		}

		executorPool.getEmbeddedScriptInvokerPool().release(embeddedScriptInvoker);
		
		if(!returnResult) {
			return null;
		}
		final ScriptExecutionResult executionResult = new ScriptExecutionResult(null);

		final PyStringMap  locals = (PyStringMap) pythonInterpreter.getLocals();
		for(Object key : locals.keys()) {
			executionResult.put(key.toString(), pythonInterpreter.get(key.toString(), Object.class));
		}
		return executionResult;
	}

	@Override
	public void executeEmbedded(int parentScriptId, int scriptId, GameScript<PyCode> script,
								EmbeddedScriptInvoker embeddedScriptInvoker, ScriptBindings bindings) throws Exception {
		final PyCode pythonScript = script.getScript();

		pythonInterpreter.set(ScriptBindings.SCRIPT_PARENT_ID_VAR, parentScriptId);
		pythonInterpreter.set(ScriptBindings.SCRIPT_ID_VAR, scriptId);
		embeddedScriptInvoker.setParentScriptId(scriptId);

		try {
			pythonInterpreter.exec(pythonScript);
		} catch (PyException e) {
			if(e.getCause() instanceof ScriptSkippedException) {
				throw new ScriptSkippedException();
			} else {
				throw e;
			}
		}

		pythonInterpreter.set(ScriptBindings.SCRIPT_ID_VAR, parentScriptId);
		embeddedScriptInvoker.setParentScriptId(parentScriptId);

		final PyStringMap  locals = (PyStringMap) pythonInterpreter.getLocals();
		for(Object key : locals.keys()) {
			bindings.put(key.toString(), pythonInterpreter.get(key.toString(), Object.class));
		}
	}

	@Override
	public void release() {
		try {
			pythonInterpreter.cleanup();
		} catch (Exception e) {
			e.printStackTrace();
		}
		executorPool.release(this);
	}

}
