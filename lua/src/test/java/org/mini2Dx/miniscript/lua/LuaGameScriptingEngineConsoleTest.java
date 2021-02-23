/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.lua;

import org.mini2Dx.miniscript.core.GameScriptingEngine;
import org.mini2Dx.miniscript.core.threadpool.ConsoleThreadPoolProvider;

public class LuaGameScriptingEngineConsoleTest extends LuaGameScriptingEngineTest {

	@Override
	protected GameScriptingEngine createScriptingEngine() {
		return new LuaGameScriptingEngine(false, new ConsoleThreadPoolProvider());
	}
}
