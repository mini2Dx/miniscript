/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Thomas Cashman
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
package org.mini2Dx.miniscript.core.threadpool;

import org.mini2Dx.miniscript.core.util.ReadWritePriorityQueue;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledTaskFuture<T extends Object> implements ScheduledFuture<T> {
	private final Object monitor = new Object();
	private final AtomicBoolean done = new AtomicBoolean(false);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private final AtomicReference<Thread> executingThread = new AtomicReference<>(null);

	private final ReadWritePriorityQueue<ScheduledTask> scheduledTaskQueue;

	private ScheduledTask scheduledTask;
	private long scheduledTimeNanos;


	public ScheduledTaskFuture(ReadWritePriorityQueue<ScheduledTask> scheduledTaskQueue) {
		this.scheduledTaskQueue = scheduledTaskQueue;
	}

	public void markDone() {
		executingThread.set(null);
		done.set(true);

		synchronized(monitor) {
			monitor.notifyAll();
		}
	}

	@Override
	public long getDelay(TimeUnit unit) {
		final long delayNanos = System.nanoTime() - scheduledTimeNanos;
		if(delayNanos < 0) {
			return 0L;
		}
		return unit.convert(delayNanos, TimeUnit.NANOSECONDS);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		cancelled.set(true);

		if(scheduledTask != null) {
			scheduledTaskQueue.remove(scheduledTask);
		}

		if(mayInterruptIfRunning) {
			final Thread thread = executingThread.getAndSet(null);
			if(thread != null) {
				thread.interrupt();
			}
		}

		synchronized(monitor) {
			monitor.notifyAll();
		}
		return false;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		while(!done.get() && !cancelled.get()) {
			synchronized(monitor) {
				monitor.wait(unit.toMillis(timeout));
			}
		}
		return null;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		try {
			return get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new ExecutionException(e);
		}
	}

	@Override
	public int compareTo(Delayed o) {
		return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
	}

	@Override
	public boolean isCancelled() {
		return cancelled.get();
	}

	@Override
	public boolean isDone() {
		return done.get();
	}

	public void setScheduledTask(ScheduledTask scheduledTask) {
		this.scheduledTask = scheduledTask;
		this.scheduledTimeNanos = scheduledTask.getScheduledStartTimeNanos();
	}

	public void setExecutingThread(Thread thread) {
		executingThread.set(thread);
	}
}
