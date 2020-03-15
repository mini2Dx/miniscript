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

import org.mini2Dx.miniscript.core.*;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * An implementation of {@link ScriptExecutor} for Groovy-based scripts
 */
public class GroovyScriptExecutor implements ScriptExecutor<Script> {
	private final GroovyScriptExecutorPool executorPool;
	private final GroovyShell groovyShell = new GroovyShell();

	private Script lastScript;
	
	public GroovyScriptExecutor(GroovyScriptExecutorPool executorPool) {
		this.executorPool = executorPool;
	}

	@Override
	public GameScript<Script> compile(String script) {
		return new GlobalGameScript<Script>(groovyShell.parse(script));
	}
	
	@Override
	public ScriptExecutionResult execute(int scriptId, GameScript<Script> script, ScriptBindings bindings, boolean returnResult) throws Exception {
		final Script groovyScript = script.getScript();
		this.lastScript = groovyScript;

		final GroovyEmbeddedScriptInvoker embeddedScriptInvoker = executorPool.getEmbeddedScriptInvokerPool().allocate();
		embeddedScriptInvoker.setScriptBindings(bindings);
		embeddedScriptInvoker.setScriptExecutor(this);
		embeddedScriptInvoker.setParentScriptId(scriptId);

		final Binding binding = new Binding(bindings);
		binding.setVariable(ScriptBindings.SCRIPT_PARENT_ID_VAR, -1);
		binding.setVariable(ScriptBindings.SCRIPT_ID_VAR, scriptId);
		binding.setVariable(ScriptBindings.SCRIPT_INVOKE_VAR, embeddedScriptInvoker);
		groovyScript.setBinding(binding);
		groovyScript.run();

		executorPool.getEmbeddedScriptInvokerPool().release(embeddedScriptInvoker);
		
		return returnResult ? new ScriptExecutionResult(groovyScript.getBinding().getVariables()) : null;
	}

	@Override
	public void executeEmbedded(int parentScriptId, int scriptId, GameScript<Script> script,
								EmbeddedScriptInvoker embeddedScriptInvoker, ScriptBindings bindings) throws Exception {
		final Script previousScript = lastScript;
		final Script groovyScript = script.getScript();
		this.lastScript = groovyScript;
		embeddedScriptInvoker.setParentScriptId(scriptId);

		final Binding binding = new Binding(previousScript.getBinding().getVariables());
		binding.setVariable(ScriptBindings.SCRIPT_PARENT_ID_VAR, parentScriptId);
		binding.setVariable(ScriptBindings.SCRIPT_ID_VAR, scriptId);
		binding.setVariable(ScriptBindings.SCRIPT_INVOKE_VAR, embeddedScriptInvoker);
		groovyScript.setBinding(binding);
		groovyScript.run();

		this.lastScript = previousScript;
		lastScript.getBinding().setVariable(ScriptBindings.SCRIPT_ID_VAR, parentScriptId);
		embeddedScriptInvoker.setParentScriptId(parentScriptId);

		for(Object variableName : groovyScript.getBinding().getVariables().keySet()) {
			this.lastScript.getBinding().setVariable((String) variableName, groovyScript.getBinding().getVariables().get(variableName));
		}
	}

	@Override
	public void release() {
		executorPool.release(this);
	}
}
