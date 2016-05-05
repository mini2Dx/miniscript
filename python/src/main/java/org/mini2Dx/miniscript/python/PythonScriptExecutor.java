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

import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.python.core.PyCode;
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
	public PyCode compile(String script) {
		return pythonInterpreter.compile(script);
	}

	@Override
	public void execute(PyCode script, ScriptBindings bindings) throws Exception {
		for (String variableName : bindings.keySet()) {
			pythonInterpreter.set(variableName, bindings.get(variableName));
		}
		pythonInterpreter.exec(script);
		;
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
