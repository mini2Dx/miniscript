package org.mini2Dx.miniscript.lua;

import org.junit.Assert;
import org.junit.Test;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.dummy.DummyGameScriptingEngine;
import org.mini2Dx.miniscript.lua.LuaScriptExecutorPool;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2019 Viridian Software Ltd.
 */

public class LuaScriptExecutorPoolTest implements ScriptInvocationListener {

	@Test
	public void testScriptIds() {

		DummyGameScriptingEngine dummyGameScriptingEngine = new DummyGameScriptingEngine();
		GeneratedClasspathScriptProvider classpathScriptProvider = new GeneratedClasspathScriptProvider() {
			@Override
			public Map getGeneratedScripts() {
				final HashMap results = new HashMap();
				results.put("script1", JsePlatform.standardGlobals().load("print(\"script1\")"));
				results.put("script2", JsePlatform.standardGlobals().load("print(\"script2\")"));
				results.put("script3", JsePlatform.standardGlobals().load("print(\"script3\")"));
				results.put("script4", JsePlatform.standardGlobals().load("print(\"script4\")"));
				return results;
			}
		};

		LuaScriptExecutorPool luaScriptExecutorPool = new LuaScriptExecutorPool(dummyGameScriptingEngine, classpathScriptProvider, 4, false);

		ScriptBindings scriptBindings = new ScriptBindings();

		ScriptExecutionTask scriptExecutionTask = luaScriptExecutorPool.execute(0, scriptBindings, this);
		Assert.assertEquals(0, scriptExecutionTask.getScriptId());
		scriptExecutionTask = luaScriptExecutorPool.execute(1, scriptBindings, this);
		Assert.assertEquals(1, scriptExecutionTask.getScriptId());
		scriptExecutionTask = luaScriptExecutorPool.execute(2, scriptBindings, this);
		Assert.assertEquals(2, scriptExecutionTask.getScriptId());
		scriptExecutionTask = luaScriptExecutorPool.execute(3, scriptBindings, this);
		Assert.assertEquals(3, scriptExecutionTask.getScriptId());
	}

	@Override
	public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
	}

	@Override
	public void onScriptSkipped(int scriptId) {
	}

	@Override
	public void onScriptException(int scriptId, Exception e) {
	}

	@Override
	public boolean callOnGameThread() {
		return false;
	}
}
