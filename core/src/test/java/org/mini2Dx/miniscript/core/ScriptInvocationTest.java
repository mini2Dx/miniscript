/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core;

import org.junit.Assert;
import org.junit.Test;

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
}
