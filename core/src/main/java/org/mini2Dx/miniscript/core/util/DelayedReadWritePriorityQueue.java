/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import org.mini2Dx.miniscript.core.threadpool.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class DelayedReadWritePriorityQueue extends ReadWritePriorityQueue<ScheduledTask> {

	public DelayedReadWritePriorityQueue() {
		this(Integer.MAX_VALUE);
	}

	public DelayedReadWritePriorityQueue(int maxCapacity) {
		super(maxCapacity);
	}

	@Override
	public ScheduledTask take() throws InterruptedException {
		ScheduledTask head = super.peek();
		while(head == null || head.getScheduledStartTimeNanos() > System.nanoTime()) {
			if(head == null) {
				synchronized(waitingForItemsMonitor) {
					waitingForItemsMonitor.wait();
				}
			} else {
				long currentTimeNanos = System.nanoTime();
				long delayMillis = Math.max (0, (head.getScheduledStartTimeNanos() - currentTimeNanos) / TimeUnit.MILLISECONDS.toNanos(1));
				int delayNanos = Math.max (0, (int) ((head.getScheduledStartTimeNanos() - currentTimeNanos) % TimeUnit.MILLISECONDS.toNanos(1)));

				synchronized(waitingForItemsMonitor) {
					waitingForItemsMonitor.wait(delayMillis, delayNanos);
				}
			}

			head = super.peek();
		}

		lock.lockWrite();
		ScheduledTask result = super.peek();
		while (isEmpty() || result == null || result.getScheduledStartTimeNanos() > System.nanoTime()) {
			lock.unlockWrite();
			synchronized(waitingForItemsMonitor) {
				if(result == null) {
					waitingForItemsMonitor.wait();
				} else {
					long currentTimeNanos = System.nanoTime();
					long delayMillis = Math.max (0, (head.getScheduledStartTimeNanos() - currentTimeNanos) / TimeUnit.MILLISECONDS.toNanos(1));
					int delayNanos = Math.max (0, (int) ((head.getScheduledStartTimeNanos() - currentTimeNanos) % TimeUnit.MILLISECONDS.toNanos(1)));
					waitingForItemsMonitor.wait(delayMillis, delayNanos);
				}
			}
			lock.lockWrite();
			result = super.peek();
		}
		result = super.poll();
		lock.unlockWrite();

		synchronized(waitingForRemovalMonitor) {
			waitingForRemovalMonitor.notify();
		}
		return result;
	}
}
