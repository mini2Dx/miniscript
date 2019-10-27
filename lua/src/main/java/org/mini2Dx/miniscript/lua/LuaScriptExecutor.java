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
package org.mini2Dx.miniscript.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.mini2Dx.miniscript.core.GameScript;
import org.mini2Dx.miniscript.core.PerThreadGameScript;
import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionResult;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

/**
 * An implementation of {@link ScriptExecutor} for Lua scripts
 */
public class LuaScriptExecutor implements ScriptExecutor<LuaValue> {
	private final LuaScriptExecutorPool executorPool;

	public LuaScriptExecutor(LuaScriptExecutorPool executorPool) {
		this.executorPool = executorPool;
	}

	@Override
	public GameScript<LuaValue> compile(String script) {
		return new PerThreadGameScript<LuaValue>(script);
	}

	@Override
	public ScriptExecutionResult execute(int scriptId, GameScript<LuaValue> script, ScriptBindings bindings, boolean returnResult)
			throws Exception {
		Globals globals = executorPool.getLocalGlobals();
		
		for (String variableName : bindings.keySet()) {
			globals.set(variableName, CoerceJavaToLua.coerce(bindings.get(variableName)));
		}
		globals.set(ScriptBindings.SCRIPT_ID_VAR, CoerceJavaToLua.coerce(scriptId));

		if (!script.hasScript()) {
			script.setScript(executorPool.compileWithGlobals(globals, script));
		}
		
		try {
			script.getScript().invoke();
		} catch (Exception e) {
			if(e.getCause() instanceof ScriptSkippedException) {
				throw new ScriptSkippedException();
			} else {
				throw e;
			}
		}

		if (!returnResult) {
			for (String variableName : bindings.keySet()) {
				globals.set(variableName, CoerceJavaToLua.coerce(null));
			}
			return null;
		}		
		ScriptExecutionResult executionResult = new ScriptExecutionResult(null);
		
		LuaValue [] keys = globals.keys();
		for(int i = 0; i < keys.length; i++) {
			LuaValue key = keys[i];
			LuaValue value = globals.get(key);
			switch ( value.type() ) {
			case LuaValue.TNIL:
				continue;
			case LuaValue.TSTRING:
				executionResult.put(key.tojstring(), value.tojstring());
				continue;
			case LuaValue.TNUMBER:
				if(value.isinttype()) {
					executionResult.put(key.tojstring(), value.toint());
				} else {
					executionResult.put(key.tojstring(), value.todouble());
				}
				continue;
			case LuaValue.TBOOLEAN:
				executionResult.put(key.tojstring(), value.toboolean());
				continue;
			case LuaValue.TUSERDATA:
				if(bindings.containsKey(key.tojstring())) {
					executionResult.put(key.tojstring(), value.checkuserdata(Object.class));
				}
				continue;
			}
		}
		
		for (String variableName : bindings.keySet()) {
			globals.set(variableName, CoerceJavaToLua.coerce(null));
		}
		return executionResult;
	}

	@Override
	public void release() {
		executorPool.release(this);
	}

}
