/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import org.junit.Assert;
import org.junit.Test;
import org.mini2Dx.miniscript.core.threadpool.ScheduledTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DelayedReadWritePriorityQueueTest {
	private static final int MAX_CAPACITY = 5;

	@Test
	public void testDelay() {
		final DelayedReadWritePriorityQueue queue = new DelayedReadWritePriorityQueue(MAX_CAPACITY);
		final long expectedStartTimeNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
		queue.offer(ScheduledTask.allocate(() -> {

		}, expectedStartTimeNanos));

		final AtomicLong result = new AtomicLong(-1L);
		Thread thread = new Thread(() -> {
			try {
				final ScheduledTask task = queue.take();
				result.set(System.nanoTime());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		thread.start();
		try {
			thread.join();
		} catch (Exception e) {}

		Assert.assertTrue(result.get() >= expectedStartTimeNanos);
	}

	@Test
	public void testDelayWithEmptyQueue() {
		final DelayedReadWritePriorityQueue queue = new DelayedReadWritePriorityQueue(MAX_CAPACITY);
		final long expectedStartTimeNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);

		final AtomicLong result = new AtomicLong(-1L);
		Thread thread = new Thread(() -> {
			try {
				final ScheduledTask task = queue.take();
				result.set(System.nanoTime());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		thread.start();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}

		queue.offer(ScheduledTask.allocate(() -> {

		}, expectedStartTimeNanos));

		try {
			thread.join();
		} catch (Exception e) {}

		Assert.assertTrue(result.get() >= expectedStartTimeNanos);
	}
}
