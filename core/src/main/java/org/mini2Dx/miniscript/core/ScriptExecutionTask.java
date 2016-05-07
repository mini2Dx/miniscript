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
import java.util.concurrent.atomic.AtomicInteger;

import org.mini2Dx.miniscript.core.exception.ScriptSkippedException;

/**
 * Executes a script
 */
public class ScriptExecutionTask<S> implements Runnable {
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

	private final int id;
	private final ScriptExecutor<S> executor;
	private final GameScript<S> script;
	private final ScriptBindings scriptBindings;
	private final ScriptInvocationListener scriptInvocationListener;

	private Future<?> taskFuture;

	public ScriptExecutionTask(ScriptExecutor<S> executor, GameScript<S> script, ScriptBindings scriptBindings,
			ScriptInvocationListener scriptInvocationListener) {
		this.executor = executor;
		this.script = script;
		this.scriptBindings = scriptBindings;
		this.scriptInvocationListener = scriptInvocationListener;

		id = ID_GENERATOR.incrementAndGet();
	}

	@Override
	public void run() {
		try {
			executor.execute(script, scriptBindings, scriptInvocationListener);
		} catch (InterruptedException | ScriptSkippedException e) {
			if (scriptInvocationListener != null) {
				scriptInvocationListener.onScriptSkipped(script.getId());
			}
		} catch (Exception e) {
			if (scriptInvocationListener != null) {
				scriptInvocationListener.onScriptException(script.getId(), e);
			} else {
				e.printStackTrace();
			}
		}
		// Clear interrupted bit
		Thread.interrupted();
	}

	public void skipScript() {
		if (!taskFuture.cancel(true)) {

		}
	}

	public boolean isFinished() {
		return taskFuture != null && taskFuture.isDone();
	}

	public void cleanup() {
		executor.release();
	}

	public int getTaskId() {
		return id;
	}

	public void setTaskFuture(Future<?> taskFuture) {
		this.taskFuture = taskFuture;
	}
}
