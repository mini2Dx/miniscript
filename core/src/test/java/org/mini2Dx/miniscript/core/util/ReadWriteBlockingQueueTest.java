/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class ReadWriteBlockingQueueTest {
	private static final int MAX_CAPACITY = 5;

	@Test
	public void testPut() {
		final ReadWriteBlockingQueue<String> queue = new ReadWriteBlockingQueue<>(MAX_CAPACITY);
		for(int i = 0; i < MAX_CAPACITY; i++) {
			queue.offer("test" + i);
		}

		final Thread thread1 = new Thread(() -> {
			for(int i = 0; i < MAX_CAPACITY; i++) {
				try {
					queue.put("test" + (MAX_CAPACITY + i));
				} catch (InterruptedException e) {}
			}
		});

		final Thread thread2 = new Thread(() -> {
			for(int i = 0; i < MAX_CAPACITY; i++) {
				queue.poll();
			}
		});

		thread1.start();
		Assert.assertEquals(MAX_CAPACITY, queue.size());

		try {
			Thread.sleep(250);
		} catch (Exception e) {}

		Assert.assertEquals(MAX_CAPACITY, queue.size());
		thread2.start();

		try {
			thread1.join();
			thread2.join();
		} catch (Exception e) {}

		Assert.assertEquals(MAX_CAPACITY, queue.size());
	}

	@Test
	public void testTake() {
		final ReadWriteBlockingQueue<String> queue = new ReadWriteBlockingQueue<>(MAX_CAPACITY);
		final Thread thread1 = new Thread(() -> {
			for(int i = 0; i < MAX_CAPACITY; i++) {
				try {
					queue.take();
				} catch (InterruptedException e) {}
			}
		});
		thread1.start();
		Assert.assertEquals(0, queue.size());

		try {
			Thread.sleep(100);
		} catch (Exception e) {}

		final Thread thread2 = new Thread(() -> {
			for(int i = 0; i < MAX_CAPACITY; i++) {
				queue.offer("test" + i);

				try {
					Thread.sleep(10);
				} catch (Exception e) {}
			}
		});

		thread2.start();
		try {
			thread1.join();
			thread2.join();
		} catch (Exception e) {}

		Assert.assertEquals(0, queue.size());
	}

	@Test
	public void testMultiplePutTake() {
		final int totalThreads = 8;
		final Thread[] threads = new Thread[totalThreads];
		final ReadWriteBlockingQueue<String> queue = new ReadWriteBlockingQueue<>(MAX_CAPACITY);

		for(int i = 0; i < totalThreads; i++) {
			if(i % 2 == 0) {
				threads[i] = new Thread(() -> {
					for(int ii = 0; ii < 100; ii++) {
						try {
							queue.put("test" + ii);
						} catch (InterruptedException e) {}
					}
				});
			} else {
				threads[i] = new Thread(() -> {
					for(int ii = 0; ii < 100; ii++) {
						try {
							queue.take();
						} catch (InterruptedException e) {}
					}
				});
			}
		}

		for(int i = 0; i < totalThreads; i++) {
			threads[i].start();
		}
		for(int i = 0; i < totalThreads; i++) {
			try {
				threads[i].join();
			} catch (Exception e) {}
		}

		Assert.assertEquals(0, queue.size());
	}
}
