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
package org.mini2Dx.miniscript.core.dummy;

import org.mini2Dx.miniscript.core.GameFuture;
import org.mini2Dx.miniscript.core.GameScriptingEngine;
import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

/**
 * A {@link GameFuture} implementation for unit tests
 */
public class DummyGameFuture extends GameFuture {
	private boolean updated = false;
	private boolean futureSkipped = false;
	private boolean scriptSkipped = false;
	private boolean futureCompleted = false;
	private boolean waitOccurred = false;
	private int updateCount = 0;
	
	public DummyGameFuture(GameScriptingEngine gameScriptingEngine) {
		super(gameScriptingEngine);
	}

	@Override
	protected boolean update(float delta) {
		updated = true;
		updateCount++;
		return futureCompleted;
	}

	@Override
	protected void onFutureSkipped() {
		futureSkipped = true;
	}

	@Override
	protected void onScriptSkipped() {
		scriptSkipped = true;
	}
	
	@Override
	public void waitForCompletion() throws ScriptSkippedException {
		waitOccurred = true;
		super.waitForCompletion();
	}

	public boolean isFutureCompleted() {
		return futureCompleted;
	}

	public void setFutureCompleted(boolean futureCompleted) {
		this.futureCompleted = futureCompleted;
	}

	public boolean isUpdated() {
		return updated;
	}

	public boolean isFutureSkippedCalled() {
		return futureSkipped;
	}

	public boolean isScriptSkippedCalled() {
		return scriptSkipped;
	}
	
	public boolean waitOccurred() {
		return waitOccurred;
	}

	public int getUpdateCount() {
		return updateCount;
	}
}
