/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020 Thomas Cashman
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
package org.mini2Dx.miniscript.core;

import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;

/**
 * Utility class for invoking scripts inside of other scripts
 */
public abstract class EmbeddedScriptInvoker {
	protected final GameScriptingEngine gameScriptingEngine;

	protected int parentScriptId;
	protected ScriptBindings scriptBindings;

	public EmbeddedScriptInvoker(GameScriptingEngine gameScriptingEngine) {
		this.gameScriptingEngine = gameScriptingEngine;
	}

	public int getScriptId(String filepath) {
		return gameScriptingEngine.getCompiledScriptId(filepath);
	}

	/**
	 * Invokes a script during the execution of the current script. Equivalent to calling a function.
	 * @param scriptId The script ID to invoke
	 */
	public abstract void invokeSync(int scriptId);

	/**
	 * Invokes a script during the execution of the current script. Equivalent to calling a function.
	 * @param filepath The filepath of the script to invoke
	 */
	public void invokeSync(String filepath) {
		final int scriptId = gameScriptingEngine.getCompiledScriptId(filepath);
		if (scriptId < 0) {
			throw new NoSuchScriptException(filepath);
		}
		invokeSync(scriptId);
	}

	/**
	 * Queues a script for invocation in the game engine. Depending on the number of script threads
	 * and currently excuting scripts, this may invoke simultaneously to the current script.
	 * @param scriptId The script ID to invoke
	 */
	public void invokeAsync(int scriptId) {
		gameScriptingEngine.invokeCompiledScript(scriptId, scriptBindings.duplicate());
	}

	/**
	 * Queues a script for invocation in the game engine. Depending on the number of script threads
	 * and currently excuting scripts, this may invoke simultaneously to the current script.
	 * @param filepath The filepath of the script to invoke
	 */
	public void invokeAsync(String filepath) {
		final int scriptId = gameScriptingEngine.getCompiledScriptId(filepath);
		if (scriptId < 0) {
			throw new NoSuchScriptException(filepath);
		}
		invokeAsync(scriptId);
	}

	public void setScriptBindings(ScriptBindings scriptBindings) {
		this.scriptBindings = scriptBindings;
	}

	public void setParentScriptId(int parentScriptId) {
		this.parentScriptId = parentScriptId;
	}
}
