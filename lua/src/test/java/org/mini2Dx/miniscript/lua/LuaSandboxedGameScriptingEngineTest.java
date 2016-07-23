/**
 * Copyright 2016 Thomas Cashman
 */
package org.mini2Dx.miniscript.lua;

import java.io.InputStream;

import org.mini2Dx.miniscript.core.GameScriptingEngine;
import org.mini2Dx.miniscript.core.SandboxedGameScriptingEngineTest;

/**
 * UATs for {@link LuaGameScriptingEngine} with sandboxing
 */
public class LuaSandboxedGameScriptingEngineTest extends SandboxedGameScriptingEngineTest {

	@Override
	protected GameScriptingEngine createScriptingEngineWithSandboxing() {
		return new LuaGameScriptingEngine(true);
	}

	@Override
	protected InputStream getSandboxScriptWithIllegalLines() {
		return LuaGameScriptingEngineTest.class.getResourceAsStream("/illegalSandbox.lua");
	}

	@Override
	protected InputStream getSandboxScript() {
		return LuaGameScriptingEngineTest.class.getResourceAsStream("/legalSandbox.lua");
	}

}
