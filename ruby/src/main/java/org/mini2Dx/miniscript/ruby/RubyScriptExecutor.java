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
import org.mini2Dx.miniscript.core.GameScript;
import org.mini2Dx.miniscript.core.PerThreadGameScript;
import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionResult;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.ScriptInvocationListener;
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
	public ScriptExecutionResult execute(GameScript<EmbedEvalUnit> s, ScriptBindings bindings,
			boolean returnResult) throws Exception {
		PerThreadGameScript<EmbedEvalUnit> script = (PerThreadGameScript<EmbedEvalUnit>) s;

		ScriptingContainer scriptingContainer = executorPool.getLocalScriptingContainer();

		scriptingContainer.getVarMap().putAll(bindings);

		if (!script.hasLocalScript()) {
			script.putLocalScript(scriptingContainer.parse(script.getContent()));
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

		if (!returnResult) {
			scriptingContainer.clear();
			return null;
		}		
		ScriptExecutionResult executionResult = new ScriptExecutionResult(scriptingContainer.getVarMap().getMap());
		scriptingContainer.clear();
		return executionResult;
	}

	@Override
	public void release() {
		executorPool.release(this);
	}

}
