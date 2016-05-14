/**
 * Copyright 2016 Thomas Cashman
 */
package org.mini2Dx.miniscript.lua;

import org.mini2Dx.miniscript.core.GameScriptingEngine;
import org.mini2Dx.miniscript.core.ScriptExecutorPool;

/**
 *
 */
public class LuaGameScriptingEngine extends GameScriptingEngine {
	/**
	 * Constructs a scripting engine backed by a thread pool with the maximum
	 * amount of concurrent scripts set to the amount of processors + 1;
	 */
	public LuaGameScriptingEngine() {
		super();
	}

	/**
	 * Constructs a scripting engine backed by a thread pool.
	 * 
	 * @param maxConcurrentScripts
	 *            The maximum amount of concurrently running scripts. Note this
	 *            is a 'requested' amount and may be less due to the amount of
	 *            available processors on the player's machine.
	 */
	public LuaGameScriptingEngine(int maxConcurrentScripts) {
		super(maxConcurrentScripts);
	}

	@Override
	protected ScriptExecutorPool<?> createScriptExecutorPool(int poolSize) {
		return new LuaScriptExecutorPool(this, poolSize);
	}
}
