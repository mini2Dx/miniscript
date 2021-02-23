/**
 * Copyright 2020 Viridian Software Ltd.
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
