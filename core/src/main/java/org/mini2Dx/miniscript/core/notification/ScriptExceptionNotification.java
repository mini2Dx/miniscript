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
package org.mini2Dx.miniscript.core.notification;

import org.mini2Dx.miniscript.core.ScriptInvocationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class ScriptExceptionNotification implements ScriptNotification {
	private final ScriptInvocationListener invocationListener;
	private final int scriptId;
	private final Exception exception;
	private final AtomicBoolean notified = new AtomicBoolean(false);

	public ScriptExceptionNotification(ScriptInvocationListener invocationListener, int scriptId, Exception exception) {
		this.invocationListener = invocationListener;
		this.scriptId = scriptId;
		this.exception = exception;
	}

	@Override
	public void process() {
		try {
			invocationListener.onScriptException(scriptId, exception);
		} finally {
			notified.set(true);
		}

		synchronized(this) {
			notifyAll();
		}
	}

	public void waitForNotification() {
		while(!notified.get()) {
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException e) {}
			}
		}
	}
}
