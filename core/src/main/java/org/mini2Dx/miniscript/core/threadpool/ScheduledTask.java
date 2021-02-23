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

import org.mini2Dx.miniscript.core.util.ReadWriteArrayQueue;

import java.util.concurrent.TimeUnit;

public class ScheduledTask implements Comparable<ScheduledTask> {
	private static final ReadWriteArrayQueue<ScheduledTask> POOL = new ReadWriteArrayQueue<>();

	private ScheduledTaskFuture future;

	private Runnable runnable;
	private long scheduledStartTime;

	private long repeatInterval;
	private TimeUnit repeatUnit;

	private boolean disposed = false;

	public void dispose() {
		if(disposed) {
			return;
		}

		disposed = true;

		runnable = null;
		scheduledStartTime = 0L;

		repeatUnit = null;
		repeatInterval = 0L;

		future = null;

		POOL.add(this);
	}

	public static ScheduledTask allocate(Runnable runnable, long scheduledStartTime) {
		ScheduledTask task = POOL.poll();
		if(task == null) {
			task = new ScheduledTask();
		}
		task.disposed = false;
		task.runnable = runnable;
		task.scheduledStartTime = scheduledStartTime;
		return task;
	}

	public static ScheduledTask allocate(Runnable runnable, long scheduledStartTime, long repeatInterval, TimeUnit repeatUnit) {
		ScheduledTask task = POOL.poll();
		if(task == null) {
			task = new ScheduledTask();
		}
		task.disposed = false;
		task.runnable = runnable;
		task.scheduledStartTime = scheduledStartTime;
		task.repeatUnit = repeatUnit;
		task.repeatInterval = repeatInterval;
		return task;
	}

	public Runnable getRunnable() {
		return runnable;
	}

	public long getScheduledStartTime() {
		return scheduledStartTime;
	}

	public boolean isRepeating() {
		return repeatUnit != null;
	}

	public long getRepeatInterval() {
		return repeatInterval;
	}

	public TimeUnit getRepeatUnit() {
		return repeatUnit;
	}

	@Override
	public int compareTo(ScheduledTask o) {
		return Long.compare(scheduledStartTime, o.scheduledStartTime);
	}

	public ScheduledTaskFuture getFuture() {
		return future;
	}

	public void setFuture(ScheduledTaskFuture future) {
		this.future = future;
	}
}
