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

import org.mini2Dx.miniscript.core.ThreadPoolProvider;
import org.mini2Dx.miniscript.core.util.ScheduledTaskQueue;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ThreadPoolProvider} implementation for Kava-based runtimes
 */
public class KavaThreadPoolProvider implements Runnable, ThreadPoolProvider {
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final Thread [] threads;

	private final ScheduledTaskQueue scheduledTaskQueue = new ScheduledTaskQueue();

	public KavaThreadPoolProvider() {
		this(Runtime.getRuntime().availableProcessors() + 1);
	}

	public KavaThreadPoolProvider(int maxConcurrentScripts) {
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
		new Thread(() -> {
			for (int i = 0; i < threads.length; i++) {
				//NativeAOT workaround
				try {
					threads[i].interrupt();
					Thread.sleep(10);
				} catch(Exception e) {}
			}
		}).start();
	}

	@Override
	public void run() {
		final Thread currentThread = Thread.currentThread();
		while(running.get()) {
			final ScheduledTask scheduledTask;
			try {
				scheduledTask = scheduledTaskQueue.take();
				if(scheduledTask.getScheduledStartTimeNanos() > System.nanoTime()) {

				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
			if(scheduledTask == null) {
				continue;
			}
			try {
				final ScheduledTaskFuture future = scheduledTask.getFuture();
				future.setExecutingThread(currentThread);
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
							scheduledTask.getScheduledStartTimeNanos() + scheduledTask.getRepeatUnit().toNanos(scheduledTask.getRepeatInterval()),
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
