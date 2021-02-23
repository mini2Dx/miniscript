/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.threadpool;

import org.mini2Dx.miniscript.core.ThreadPoolProvider;
import org.mini2Dx.miniscript.core.util.ReadWritePriorityQueue;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsoleThreadPoolProvider implements Runnable, ThreadPoolProvider {
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final Thread [] threads;

	private final ReadWritePriorityQueue<ScheduledTask> scheduledTaskQueue = new ReadWritePriorityQueue<>();

	public ConsoleThreadPoolProvider() {
		this(Runtime.getRuntime().availableProcessors() + 1);
	}

	public ConsoleThreadPoolProvider(int maxConcurrentScripts) {
		threads = new Thread[maxConcurrentScripts];
		for(int i = 0; i < maxConcurrentScripts; i++) {
			threads[i] = new Thread(this);
		}
		for(int i = 0; i < maxConcurrentScripts; i++) {
			threads[i].start();
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		return schedule(task, 0L, TimeUnit.MILLISECONDS);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		final ScheduledTask scheduledTask = ScheduledTask.allocate(command, System.nanoTime() + unit.toNanos(initialDelay),
				period, unit);
		final ScheduledTaskFuture future = new ScheduledTaskFuture(scheduledTaskQueue);

		scheduledTask.setFuture(future);
		future.setScheduledTask(scheduledTask);

		scheduledTaskQueue.offer(scheduledTask);
		return future;
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		final ScheduledTask scheduledTask = ScheduledTask.allocate(command, System.nanoTime() + unit.toNanos(delay));
		scheduledTaskQueue.offer(scheduledTask);

		final ScheduledTaskFuture future = new ScheduledTaskFuture(scheduledTaskQueue);

		scheduledTask.setFuture(future);
		future.setScheduledTask(scheduledTask);

		scheduledTaskQueue.offer(scheduledTask);
		return future;
	}

	@Override
	public void shutdown(boolean interruptThreads) {
		running.set(false);

		if (!interruptThreads) {
			return;
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i].interrupt();
		}
	}

	@Override
	public void run() {
		while(running.get()) {
			final ScheduledTask scheduledTask;
			try {
				scheduledTask = scheduledTaskQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
			if(scheduledTask == null) {
				continue;
			}
			try {
				final ScheduledTaskFuture future = scheduledTask.getFuture();
				future.setExecutingThread(Thread.currentThread());
				final Runnable runnable = scheduledTask.getRunnable();
				if(runnable != null && !future.isCancelled()) {
					runnable.run();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				final ScheduledTaskFuture future = scheduledTask.getFuture();
				if(scheduledTask.isRepeating() && !future.isCancelled()) {
					final ScheduledTask nextTask = ScheduledTask.allocate(scheduledTask.getRunnable(),
							System.nanoTime() + scheduledTask.getRepeatUnit().toNanos(scheduledTask.getRepeatInterval()),
							scheduledTask.getRepeatInterval(), scheduledTask.getRepeatUnit());

					nextTask.setFuture(future);
					future.setScheduledTask(nextTask);

					scheduledTaskQueue.offer(nextTask);
				} else {
					future.markDone();
				}
				scheduledTask.dispose();
			}
		}
	}
}
