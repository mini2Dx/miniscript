/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Thomas Cashman
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

import org.luaj.vm2.LuaValue;
import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;
import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

public class LuaEmbeddedScriptInvoker extends EmbeddedScriptInvoker {
	private final LuaScriptExecutorPool luaScriptExecutorPool;

	private LuaScriptExecutor scriptExecutor;

	public LuaEmbeddedScriptInvoker(GameScriptingEngine gameScriptingEngine, LuaScriptExecutorPool luaScriptExecutorPool) {
		super(gameScriptingEngine);
		this.luaScriptExecutorPool = luaScriptExecutorPool;
	}

	@Override
	public void invokeSync(int scriptId) {
		final GameScript<LuaValue> script = luaScriptExecutorPool.getScript(scriptId);
		if(script == null) {
			throw new NoSuchScriptException(scriptId);
		}
		try {
			scriptExecutor.executeEmbedded(parentScriptId, scriptId, script, this, scriptBindings);
		} catch (Exception e) {
			if(e instanceof ScriptSkippedException || e.getCause() instanceof ScriptSkippedException) {
				throw new ScriptSkippedException();
			} else {
				e.printStackTrace();
			}
		}
	}

	public void setScriptExecutor(LuaScriptExecutor scriptExecutor) {
		this.scriptExecutor = scriptExecutor;
	}
}
