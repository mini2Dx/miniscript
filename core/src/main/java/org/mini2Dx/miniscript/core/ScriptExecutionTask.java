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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;
import org.mini2Dx.miniscript.core.notification.ScriptExceptionNotification;
import org.mini2Dx.miniscript.core.notification.ScriptSkippedNotification;
import org.mini2Dx.miniscript.core.notification.ScriptSuccessNotification;

/**
 * Executes a script
 */
public class ScriptExecutionTask<S> implements Runnable {
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

	private final int id;
	private final GameScriptingEngine scriptingEngine;
	private final ScriptExecutor<S> executor;
	private final GameScript<S> script;
	private final ScriptBindings scriptBindings;
	private final ScriptInvocationListener scriptInvocationListener;

	private final AtomicBoolean completed = new AtomicBoolean(false);
	private Future<?> taskFuture;

	public ScriptExecutionTask(GameScriptingEngine gameScriptingEngine, ScriptExecutor<S> executor,
			GameScript<S> script, ScriptBindings scriptBindings, ScriptInvocationListener scriptInvocationListener) {
		this.scriptingEngine = gameScriptingEngine;
		this.executor = executor;
		this.script = script;
		this.scriptBindings = scriptBindings;
		this.scriptInvocationListener = scriptInvocationListener;

		id = ID_GENERATOR.incrementAndGet();
	}

	@Override
	public void run() {
		try {
			ScriptExecutionResult executionResult = executor.execute(script, scriptBindings,
					scriptInvocationListener != null);
			if (scriptInvocationListener != null) {
				if (scriptInvocationListener.callOnGameThread()) {
					scriptingEngine.scriptNotifications.offer(
							new ScriptSuccessNotification(scriptInvocationListener, script.getId(), executionResult));
				} else {
					scriptInvocationListener.onScriptSuccess(script.getId(), executionResult);
				}
			}
		} catch (InterruptedException | ScriptSkippedException e) {
			if (scriptInvocationListener != null) {
				if (scriptInvocationListener.callOnGameThread()) {
					scriptingEngine.scriptNotifications
							.offer(new ScriptSkippedNotification(scriptInvocationListener, script.getId()));
				} else {
					scriptInvocationListener.onScriptSkipped(script.getId());
				}
			}
		} catch (Exception e) {
			if (scriptInvocationListener != null) {
				if (scriptInvocationListener.callOnGameThread()) {
					scriptingEngine.scriptNotifications
							.offer(new ScriptExceptionNotification(scriptInvocationListener, script.getId(), e));
				} else {
					scriptInvocationListener.onScriptException(script.getId(), e);
				}
			} else {
				e.printStackTrace();
			}
		}
		// Clear interrupted bit
		Thread.interrupted();
		completed.set(true);
	}

	public void skipScript() {
		if (taskFuture.isDone()) {
			return;
		}
		if (!taskFuture.cancel(true)) {

		}
	}

	public boolean isFinished() {
		return completed.get();
	}

	public void cleanup() {
		executor.release();
	}

	public int getTaskId() {
		return id;
	}

	public int getScriptId() {
		return script.getId();
	}

	public void setTaskFuture(Future<?> taskFuture) {
		this.taskFuture = taskFuture;
	}
}
