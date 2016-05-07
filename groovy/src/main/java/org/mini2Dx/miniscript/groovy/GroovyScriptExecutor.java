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

import org.mini2Dx.miniscript.core.GameScript;
import org.mini2Dx.miniscript.core.GlobalGameScript;
import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionResult;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.ScriptInvocationListener;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * An implementation of {@link ScriptExecutor} for Groovy-based scripts
 */
public class GroovyScriptExecutor implements ScriptExecutor<Script> {
	private final GroovyScriptExecutorPool executorPool;
	private final GroovyShell groovyShell = new GroovyShell();
	
	public GroovyScriptExecutor(GroovyScriptExecutorPool executorPool) {
		this.executorPool = executorPool;
	}

	@Override
	public GameScript<Script> compile(String script) {
		return new GlobalGameScript<Script>(groovyShell.parse(script));
	}
	
	@Override
	public void execute(GameScript<Script> script, ScriptBindings bindings, ScriptInvocationListener invocationListener) throws Exception {
		Script groovyScript = script.getScript();
		groovyScript.setBinding(new Binding(bindings));
		groovyScript.run();
		
		if(invocationListener == null) {
			return;
		}
		ScriptExecutionResult executionResult = new ScriptExecutionResult(groovyScript.getBinding().getVariables());
		invocationListener.onScriptSuccess(script.getId(), executionResult);
	}

	@Override
	public void release() {
		executorPool.release(this);
	}
}
