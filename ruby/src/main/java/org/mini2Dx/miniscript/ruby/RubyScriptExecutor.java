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
package org.mini2Dx.miniscript.ruby;

import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.ScriptingContainer;
import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

/**
 * An implementation of {@link ScriptExecutor} for Ruby-based scripts
 */
public class RubyScriptExecutor implements ScriptExecutor<EmbedEvalUnit> {
	private final RubyScriptExecutorPool executorPool;

	public RubyScriptExecutor(RubyScriptExecutorPool executorPool) {
		this.executorPool = executorPool;
	}

	@Override
	public GameScript<EmbedEvalUnit> compile(String script) {
		return new PerThreadGameScript<EmbedEvalUnit>(script);
	}

	@Override
	public ScriptExecutionResult execute(int scriptId, GameScript<EmbedEvalUnit> s, ScriptBindings bindings,
			boolean returnResult) throws Exception {
		final PerThreadGameScript<EmbedEvalUnit> script = (PerThreadGameScript<EmbedEvalUnit>) s;

		final ScriptingContainer scriptingContainer = executorPool.getLocalScriptingContainer();

		final RubyEmbeddedScriptInvoker embeddedScriptInvoker = executorPool.getEmbeddedScriptInvokerPool().allocate();
		embeddedScriptInvoker.setScriptBindings(bindings);
		embeddedScriptInvoker.setScriptExecutor(this);
		embeddedScriptInvoker.setParentScriptId(scriptId);

		scriptingContainer.getVarMap().putAll(bindings);
		scriptingContainer.getVarMap().put(ScriptBindings.SCRIPT_PARENT_ID_VAR, -1);
		scriptingContainer.getVarMap().put(ScriptBindings.SCRIPT_ID_VAR, scriptId);
		scriptingContainer.getVarMap().put(ScriptBindings.SCRIPT_INVOKE_VAR, embeddedScriptInvoker);

		if (!script.hasScript()) {
			script.setScript(scriptingContainer.parse(script.getContent()));
		}
		
		try {
			EmbedEvalUnit embedEvalUnit = script.getScript();
			embedEvalUnit.run();
		} catch (Exception e) {
			if(e instanceof ScriptSkippedException || e.getCause() instanceof ScriptSkippedException) {
				throw new ScriptSkippedException();
			} else {
				throw e;
			}
		}

		executorPool.getEmbeddedScriptInvokerPool().release(embeddedScriptInvoker);

		if (!returnResult) {
			scriptingContainer.clear();
			return null;
		}		
		ScriptExecutionResult executionResult = new ScriptExecutionResult(scriptingContainer.getVarMap().getMap());
		scriptingContainer.clear();
		return executionResult;
	}

	@Override
	public void executeEmbedded(int parentScriptId, int scriptId, GameScript<EmbedEvalUnit> s,
								EmbeddedScriptInvoker embeddedScriptInvoker, ScriptBindings bindings) throws Exception {
		throw new RuntimeException("Embedded synchronous script invokes not supported in Ruby.");
		/*final PerThreadGameScript<EmbedEvalUnit> script = (PerThreadGameScript<EmbedEvalUnit>) s;

		final ScriptingContainer scriptingContainer = executorPool.getLocalScriptingContainer();
		scriptingContainer.getVarMap().put(ScriptBindings.SCRIPT_PARENT_ID_VAR, parentScriptId);
		scriptingContainer.getVarMap().put(ScriptBindings.SCRIPT_ID_VAR, scriptId);
		embeddedScriptInvoker.setParentScriptId(scriptId);

		if (!script.hasScript()) {
			script.setScript(scriptingContainer.parse(script.getContent()));
		}

		try {
			EmbedEvalUnit embedEvalUnit = script.getScript();
			embedEvalUnit.run();
		} catch (Exception e) {
			if(e.getCause() instanceof ScriptSkippedException) {
				throw new ScriptSkippedException();
			} else {
				throw e;
			}
		}

		scriptingContainer.getVarMap().put(ScriptBindings.SCRIPT_ID_VAR, parentScriptId);
		embeddedScriptInvoker.setParentScriptId(parentScriptId);*/
	}

	@Override
	public void release() {
		executorPool.release(this);
	}

}
