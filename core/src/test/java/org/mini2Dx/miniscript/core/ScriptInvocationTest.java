/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class ScriptInvocationTest {
	private final PriorityBlockingQueue<ScriptInvocation> queue = new PriorityBlockingQueue<>();

	@Test
	public void testPriority1() {
		final ScriptInvocation invocation1 = new ScriptInvocation(null);
		final ScriptInvocation invocation2 = new ScriptInvocation(null);

		invocation1.setPriority(0);
		invocation2.setPriority(1000);

		queue.offer(invocation1);
		queue.offer(invocation2);

		Assert.assertEquals(invocation2, queue.poll());
		Assert.assertEquals(invocation1, queue.poll());

		queue.offer(invocation2);
		queue.offer(invocation1);

		Assert.assertEquals(invocation2, queue.poll());
		Assert.assertEquals(invocation1, queue.poll());
	}

	@Test
	public void testPriority2() {
		final ScriptInvocation invocation1 = new ScriptInvocation(null);
		final ScriptInvocation invocation2 = new ScriptInvocation(null);

		invocation1.setPriority(9999);
		invocation2.setPriority(100);

		queue.offer(invocation1);
		queue.offer(invocation2);

		Assert.assertEquals(invocation1, queue.poll());
		Assert.assertEquals(invocation2, queue.poll());

		queue.offer(invocation2);
		queue.offer(invocation1);

		Assert.assertEquals(invocation1, queue.poll());
		Assert.assertEquals(invocation2, queue.poll());
	}

	@Test
	public void testSamePriority() {
		final ScriptInvocation invocation1 = new ScriptInvocation(null);
		final ScriptInvocation invocation2 = new ScriptInvocation(null);

		invocation1.setPriority(0);
		invocation2.setPriority(0);

		queue.offer(invocation1);
		queue.offer(invocation2);

		Assert.assertEquals(invocation1, queue.poll());
		Assert.assertEquals(invocation2, queue.poll());

		queue.offer(invocation2);
		queue.offer(invocation1);

		Assert.assertEquals(invocation2, queue.poll());
		Assert.assertEquals(invocation1, queue.poll());
	}

	@Test
	public void testPriority() {
		final Random random = new Random();
		final List<Integer> unordered = new ArrayList<>();

		for(int i = 0; i < 1000; i++) {
			final int priority = random.nextInt();
			final ScriptInvocation invocation = new ScriptInvocation(null);
			invocation.setPriority(priority);
			unordered.add(priority);

			queue.offer(invocation);
		}

		final List<Integer> ordered = new ArrayList<>(unordered);
		Collections.sort(ordered);
		Collections.reverse(ordered);

		for(int i = 0; i < 1000; i++) {
			final int expectedPriority = ordered.get(i);
			final ScriptInvocation result = queue.poll();
			Assert.assertEquals(expectedPriority, result.getPriority());
		}
	}
}
