/**
 * Copyright 2016 Thomas Cashman
 */
package org.mini2Dx.miniscript.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.mini2Dx.miniscript.core.GameScript;
import org.mini2Dx.miniscript.core.PerThreadGameScript;
import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionResult;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

/**
 *
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
	public ScriptExecutionResult execute(GameScript<LuaValue> s, ScriptBindings bindings, boolean returnResult)
			throws Exception {
		PerThreadGameScript<LuaValue> script = (PerThreadGameScript<LuaValue>) s;

		Globals globals = executorPool.getLocalGlobals();
		
		for (String variableName : bindings.keySet()) {
			globals.set(variableName, CoerceJavaToLua.coerce(bindings.get(variableName)));
		}
		if (!script.hasLocalScript()) {
			script.putLocalScript(globals.load(script.getContent()));
		}
		
		try {
			script.getScript().call();
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
