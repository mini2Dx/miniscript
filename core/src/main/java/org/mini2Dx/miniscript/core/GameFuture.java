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

import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a task that will complete in-game at a future time
 */
public abstract class GameFuture {
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
	
	private final int futureId;
	private final AtomicBoolean futureSkipped = new AtomicBoolean(false);
	private final AtomicBoolean scriptSkipped = new AtomicBoolean(false);
	private final AtomicBoolean completed = new AtomicBoolean(false);
	private final AtomicBoolean readyForGC = new AtomicBoolean(false);

	/**
	 * Constructor using {@link GameScriptingEngine#MOST_RECENT_INSTANCE}
	 */
	public GameFuture() {
		this(GameScriptingEngine.MOST_RECENT_INSTANCE);
	}

	/**
	 * Constructor
	 * 
	 * @param gameScriptingEngine
	 *            The {@link GameScriptingEngine} this future belongs to
	 */
	public GameFuture(GameScriptingEngine gameScriptingEngine) {
		if(gameScriptingEngine == null) {
			throw new RuntimeException("Cannot pass null scripting engine to " + GameFuture.class.getSimpleName());
		}
		futureId = ID_GENERATOR.incrementAndGet();
		gameScriptingEngine.submitGameFuture(this);

		if (Thread.interrupted()) {
			scriptSkipped.set(true);
			throw new ScriptSkippedException();
		}
	}

	/**
	 * Update the {@link GameFuture}
	 * 
	 * @param delta
	 *            The amount of time (in seconds) since the last frame
	 * @return True if the {@link GameFuture} has completed
	 */
	protected abstract boolean update(float delta);

	/**
	 * Called when the {@link GameFuture} is skipped
	 */
	protected abstract void onFutureSkipped();

	/**
	 * Called when the entire script is skipped
	 */
	protected abstract void onScriptSkipped();

	void evaluate(float delta) {
		if(readyForGC.get()) {
			return;
		}
		if(scriptSkipped.get()) {
			onScriptSkipped();
			readyForGC.set(true);
			return;
		}
		if(futureSkipped.get()) {
			onFutureSkipped();
			readyForGC.set(true);
			return;
		}
		if (update(delta)) {
			complete();
			readyForGC.set(true);
		}
	}

	protected void complete() {
		if(completed.getAndSet(true)) {
			return;
		}
		synchronized (this) {
			notifyAll();
		}
	}

	public void skipFuture() {
		futureSkipped.set(true);
		synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Can be called in a script to wait for this {@link GameFuture} to complete
	 * 
	 * @throws ScriptSkippedException
	 *             Thrown when the script is skipped
	 */
	public void waitForCompletion() throws ScriptSkippedException {
		while (!completed.get()) {
			if (futureSkipped.get()) {
				return;
			}
			if (scriptSkipped.get()) {
				throw new ScriptSkippedException();
			}
			if (Thread.interrupted()) {
				scriptSkipped.set(true);
				throw new ScriptSkippedException();
			}
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				scriptSkipped.set(true);
				throw new ScriptSkippedException();
			}
		}
	}

	/**
	 * Alias to {@link #waitForCompletion()}
	 * @throws ScriptSkippedException
	 */
	public void wfc() throws ScriptSkippedException {
		waitForCompletion();
	}

	/**
	 * Returns if this {@link GameFuture} is complete
	 * 
	 * @return True if this completed without being skipped
	 */
	public boolean isCompleted() {
		return completed.get();
	}

	/**
	 * Returns if this {@link GameFuture} was skipped
	 * 
	 * @return True if this {@link GameFuture} was skipped
	 */
	public boolean isFutureSkipped() {
		return futureSkipped.get();
	}

	/**
	 * Returns if the script was skipped
	 * 
	 * @return True if this {@link GameFuture} was waiting for completion when
	 *         the script was skipped
	 */
	public boolean isScriptSkipped() {
		return scriptSkipped.get();
	}
	
	/**
	 * Returns if the {@link GameFuture} is ready for cleanup
	 * @return True if ready for cleanup
	 */
	public boolean isReadyForGC() {
		return readyForGC.get();
	}

	/**
	 * Returns this unique id of this {@link GameFuture} instance
	 * @return
	 */
	public int getFutureId() {
		return futureId;
	}
}
