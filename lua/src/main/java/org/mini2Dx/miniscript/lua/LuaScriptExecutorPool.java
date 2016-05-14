/**
 * Copyright 2016 Thomas Cashman
 */
package org.mini2Dx.miniscript.lua;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.mini2Dx.miniscript.core.GameScriptingEngine;
import org.mini2Dx.miniscript.core.PerThreadGameScript;
import org.mini2Dx.miniscript.core.ScriptBindings;
import org.mini2Dx.miniscript.core.ScriptExecutionTask;
import org.mini2Dx.miniscript.core.ScriptExecutor;
import org.mini2Dx.miniscript.core.ScriptExecutorPool;
import org.mini2Dx.miniscript.core.ScriptInvocationListener;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.ScriptExecutorUnavailableException;

/**
 *
 */
public class LuaScriptExecutorPool implements ScriptExecutorPool<LuaValue> {
	private final Map<Long, Globals> threadCompilers = new ConcurrentHashMap<Long, Globals>();
	private final Map<Integer, PerThreadGameScript<LuaValue>> scripts = new ConcurrentHashMap<Integer, PerThreadGameScript<LuaValue>>();
	private final BlockingQueue<ScriptExecutor<LuaValue>> executors;
	private final GameScriptingEngine gameScriptingEngine;

	public LuaScriptExecutorPool(GameScriptingEngine gameScriptingEngine, int poolSize) {
		this.gameScriptingEngine = gameScriptingEngine;
		executors = new ArrayBlockingQueue<ScriptExecutor<LuaValue>>(poolSize);

		for (int i = 0; i < poolSize; i++) {
			executors.offer(new LuaScriptExecutor(this));
		}
	}

	@Override
	public int preCompileScript(String scriptContent) throws InsufficientCompilersException {
		PerThreadGameScript<LuaValue> script = new PerThreadGameScript<LuaValue>(scriptContent);
		scripts.put(script.getId(), script);
		return script.getId();
	}

	@Override
	public ScriptExecutionTask<?> execute(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		ScriptExecutor<LuaValue> executor = allocateExecutor();
		if (executor == null) {
			throw new ScriptExecutorUnavailableException(scriptId);
		}
		return new ScriptExecutionTask<LuaValue>(gameScriptingEngine, executor, scripts.get(scriptId),
				scriptBindings, invocationListener);
	}

	@Override
	public void release(ScriptExecutor<LuaValue> executor) {
		try {
			executors.put(executor);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public GameScriptingEngine getGameScriptingEngine() {
		return gameScriptingEngine;
	}
	
	private ScriptExecutor<LuaValue> allocateExecutor() {
		try {
			return executors.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Globals getLocalGlobals() {
		long threadId = Thread.currentThread().getId();
		if (!threadCompilers.containsKey(threadId)) {
			threadCompilers.put(threadId, JsePlatform.standardGlobals());
		}
		return threadCompilers.get(threadId);
	}
}
