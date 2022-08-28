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

	private static final int STATE_NONE = 0;
	private static final int STATE_FUTURE_SKIPPED = 1;
	private static final int STATE_FUTURE_SKIPPED_GC_READY = STATE_FUTURE_SKIPPED * 10;
	private static final int STATE_SCRIPT_SKIPPED = 2;
	private static final int STATE_SCRIPT_SKIPPED_GC_READY = STATE_SCRIPT_SKIPPED * 10;
	private static final int STATE_COMPLETED = 3;
	private static final int STATE_COMPLETED_GC_READY = STATE_COMPLETED * 10;

	private final int futureId;
	private final AtomicInteger state = new AtomicInteger(STATE_NONE);

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
			state.compareAndSet(STATE_NONE, STATE_SCRIPT_SKIPPED);
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
		if(isReadyForGC()) {
			return;
		}
		if(isScriptSkipped()) {
			onScriptSkipped();
			state.compareAndSet(STATE_SCRIPT_SKIPPED, STATE_SCRIPT_SKIPPED_GC_READY);
			return;
		}
		if(isFutureSkipped()) {
			onFutureSkipped();
			state.compareAndSet(STATE_FUTURE_SKIPPED, STATE_FUTURE_SKIPPED_GC_READY);
			return;
		}
		if (update(delta)) {
			complete();
			state.compareAndSet(STATE_COMPLETED, STATE_COMPLETED_GC_READY);
		}
	}

	protected void complete() {
		if(!state.compareAndSet(STATE_NONE, STATE_COMPLETED)) {
			return;
		}
		synchronized (this) {
			notifyAll();
		}
	}

	public void skipFuture() {
		if(!state.compareAndSet(STATE_NONE, STATE_FUTURE_SKIPPED)) {
			return;
		}
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
		while (!isCompleted()) {
			if (isFutureSkipped()) {
				return;
			}
			if (isScriptSkipped()) {
				throw new ScriptSkippedException();
			}
			if (Thread.interrupted()) {
				state.compareAndSet(STATE_NONE, STATE_SCRIPT_SKIPPED);
				throw new ScriptSkippedException();
			}
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				state.compareAndSet(STATE_NONE, STATE_SCRIPT_SKIPPED);
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
		switch (state.get()) {
		case STATE_COMPLETED:
		case STATE_COMPLETED_GC_READY:
			return true;
		}
		return false;
	}

	/**
	 * Returns if this {@link GameFuture} was skipped
	 *
	 * @return True if this {@link GameFuture} was skipped
	 */
	public boolean isFutureSkipped() {
		switch (state.get()) {
		case STATE_FUTURE_SKIPPED:
		case STATE_FUTURE_SKIPPED_GC_READY:
			return true;
		}
		return false;
	}

	/**
	 * Returns if the script was skipped
	 *
	 * @return True if this {@link GameFuture} was waiting for completion when
	 *         the script was skipped
	 */
	public boolean isScriptSkipped() {
		switch (state.get()) {
		case STATE_SCRIPT_SKIPPED:
		case STATE_SCRIPT_SKIPPED_GC_READY:
			return true;
		}
		return false;
	}

	/**
	 * Returns if the {@link GameFuture} is ready for cleanup
	 * @return True if ready for cleanup
	 */
	public boolean isReadyForGC() {
		switch (state.get()) {
		case STATE_COMPLETED_GC_READY:
		case STATE_FUTURE_SKIPPED_GC_READY:
		case STATE_SCRIPT_SKIPPED_GC_READY:
			return true;
		}
		return false;
	}

	/**
	 * Returns this unique id of this {@link GameFuture} instance
	 * @return
	 */
	public int getFutureId() {
		return futureId;
	}
}
