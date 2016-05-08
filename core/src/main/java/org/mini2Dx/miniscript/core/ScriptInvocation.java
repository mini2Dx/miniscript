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
 * Wraps a script invocation so it can be queued concurrently
 */
public class ScriptInvocation {
	private final ScriptInvocationPool invocationPool;
	
	private int scriptId;
	private ScriptBindings scriptBindings;
	private ScriptInvocationListener invocationListener;
	
	public ScriptInvocation(ScriptInvocationPool invocationPool) {
		this.invocationPool = invocationPool;
	}

	public int getScriptId() {
		return scriptId;
	}
	
	public void setScriptId(int scriptId) {
		this.scriptId = scriptId;
	}
	
	public ScriptBindings getScriptBindings() {
		return scriptBindings;
	}

	public void setScriptBindings(ScriptBindings scriptBindings) {
		this.scriptBindings = scriptBindings;
	}

	public ScriptInvocationListener getInvocationListener() {
		return invocationListener;
	}
	
	public void setInvocationListener(ScriptInvocationListener invocationListener) {
		this.invocationListener = invocationListener;
	}
	
	public void release() {
		invocationPool.release(this);
	}
}
