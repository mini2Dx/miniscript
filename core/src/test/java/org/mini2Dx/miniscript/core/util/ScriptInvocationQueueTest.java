/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import org.junit.Assert;
import org.junit.Test;
import org.mini2Dx.miniscript.core.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ScriptInvocationQueueTest implements ScriptInvocationListener {
	private static final int INTERACTIVE_SCRIPT_ID = 1;

	private final ScriptInvocationQueue invocationQueue = new ScriptInvocationQueue();
	private final ScriptInvocationPool scriptInvocationPool = new ScriptInvocationPool();
	private final AtomicInteger interactiveScriptsRunning = new AtomicInteger();

	@Test
	public void testOnlyOneInteractiveScriptPolled() {
		final Thread [] threads = new Thread[4];

		for(int i = 0; i < 1000; i++) {
			invocationQueue.offer(createInvocation(i % 10 == 0 ? INTERACTIVE_SCRIPT_ID : i, i % 10 == 0));
		}

		final CountDownLatch countDownLatch = new CountDownLatch(threads.length);

		for(int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				} catch (Exception e) {}

				while(!invocationQueue.isEmpty()) {
					ScriptInvocation scriptInvocation = invocationQueue.poll();
					if(scriptInvocation == null) {
						continue;
					}
					if(scriptInvocation.isInteractive()) {
						interactiveScriptsRunning.incrementAndGet();
						Assert.assertTrue(invocationQueue.isInteractiveScriptRunnung());
					}
					try {
						Thread.sleep(10);
					} catch (Exception e) {}
					scriptInvocation.getInvocationListener().onScriptSkipped(scriptInvocation.getScriptId());
					scriptInvocationPool.release(scriptInvocation);
				}
			});
		}
		for(int i = 0; i < threads.length; i++) {
			threads[i].start();
		}
		while(!invocationQueue.isEmpty()) {
			Assert.assertTrue(interactiveScriptsRunning.get() <= 1);
		}
		for(int i = 0; i < threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {}
		}
		Assert.assertFalse(invocationQueue.isInteractiveScriptRunnung());
	}

	private ScriptInvocation createInvocation(int scriptId, boolean interactive) {
		return scriptInvocationPool.allocate(scriptId, new ScriptBindings(), this,  0, interactive);
	}

	@Override
	public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
		if(scriptId == INTERACTIVE_SCRIPT_ID) {
			interactiveScriptsRunning.decrementAndGet();
			invocationQueue.clearInteractiveScriptStatus();
		}
	}

	@Override
	public void onScriptSkipped(int scriptId) {
		if(scriptId == INTERACTIVE_SCRIPT_ID) {
			interactiveScriptsRunning.decrementAndGet();
			invocationQueue.clearInteractiveScriptStatus();
		}
	}

	@Override
	public void onScriptException(int scriptId, Exception e) {
		if(scriptId == INTERACTIVE_SCRIPT_ID) {
			interactiveScriptsRunning.decrementAndGet();
			invocationQueue.clearInteractiveScriptStatus();
		}
	}

	@Override
	public boolean callOnGameThread() {
		return false;
	}
}
