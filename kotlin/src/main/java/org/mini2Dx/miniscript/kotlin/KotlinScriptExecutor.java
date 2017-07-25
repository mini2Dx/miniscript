/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016 Thomas Cashman
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.mini2Dx.miniscript.kotlin;

import org.mini2Dx.miniscript.core.GameScript;
import org.mini2Dx.miniscript.core.GlobalGameScript;
import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionResult;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.ScriptInvocationListener;
import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

//import groovy.lang.Binding;
//import groovy.lang.GroovyShell;
//import groovy.lang.Script;

import javax.script.*;


/**
 * An implementation of {@link ScriptExecutor} for Kotlin-based scripts
 */
public class KotlinScriptExecutor implements ScriptExecutor<Object> {
    private final KotlinScriptExecutorPool executorPool;
    private final ScriptEngine engine;

    public KotlinScriptExecutor(KotlinScriptExecutorPool executorPool) {
        this.executorPool = executorPool;

        engine = new ScriptEngineManager().getEngineByName("kts");
        // todo do we use the javax.script for kotlin?
    }

    @Override
    public GameScript<Object> compile(String script) throws ScriptException {
        return new GlobalGameScript<>(engine.eval(script, new SimpleScriptContext()));
    }

    @Override
    public ScriptExecutionResult execute(GameScript<Object> script, ScriptBindings bindings, boolean returnResult) throws Exception {
        Object kotlinScript = script.getScript();
        ScriptContext newContext = new SimpleScriptContext();
        engine.setBindings(new SimpleBindings(bindings), ScriptContext.ENGINE_SCOPE);

        for (String variableName : bindings.keySet()) {
            engine.put(variableName, bindings.get(variableName));
        }
        try {
            // todo is this .toString() appropriate?
            engine.eval(kotlinScript.toString(), newContext);
        } catch (ScriptException e) { // todo remove redundant from pythonScriptExecutor
            throw e;
        }

        if (!returnResult) {
            return null;
        }
        //TODO: Find way to extract all variables
        ScriptExecutionResult executionResult = new ScriptExecutionResult(null);
        for (String variableName : bindings.keySet()) {
            executionResult.put(variableName, engine.get(variableName));
        }
        return executionResult;
    }

    @Override
    public void release() {
        executorPool.release(this);
    }
}
