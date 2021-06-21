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
package org.mini2Dx.miniscript.core;

/**
 * Interface for script execution listeners.
 * 
 * Note: All callbacks occur on a different thread from the game thread
 */
public interface ScriptInvocationListener {
	/**
	 * Called just before a script begins execution. Note: If callOnGameThread() is true,
	 * the script will not begin until this callback is processed on the game thread
	 * @param scriptId The script id
	 */
	public default void onScriptBegin(int scriptId) {

	}

	/**
	 * Called when a script successfully completes
	 * 
	 * @param scriptId
	 *            The script id
	 * @param executionResult
	 *            The variable bindings after execution
	 */
	public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult);

	/**
	 * Called when a script is skipped
	 * 
	 * @param scriptId
	 *            The script id
	 */
	public void onScriptSkipped(int scriptId);

	/**
	 * Called when an exception occurs during script execution
	 * 
	 * @param scriptId
	 *            The script id
	 * @param e
	 *            The exception that occurred
	 */
	public void onScriptException(int scriptId, Exception e);

	/**
	 * Returns if this {@link ScriptInvocationListener} should be notified on
	 * the game thread
	 * 
	 * @return False if this should be notified on the
	 *         {@link GameScriptingEngine} thread pool
	 */
	public boolean callOnGameThread();
}
