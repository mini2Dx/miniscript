/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Thomas Cashman
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadPoolProvider implements ThreadPoolProvider {
	private static final String THREAD_NAME_PREFIX = "miniscript-thread-";
	private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

	private final ScheduledExecutorService executorService;

	public DefaultThreadPoolProvider() {
		this(Runtime.getRuntime().availableProcessors() + 1);
	}

	public DefaultThreadPoolProvider(int maxConcurrentScripts) {
		executorService = Executors.newScheduledThreadPool(
				Math.min(maxConcurrentScripts + 1, Runtime.getRuntime().availableProcessors() * 2),
				new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, THREAD_NAME_PREFIX + THREAD_ID.getAndIncrement());
					}
				});
	}

	@Override
	public Future<?> submit(Runnable task) {
		return executorService.submit(task);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return executorService.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return executorService.schedule(command, delay, unit);
	}

	@Override
	public void shutdown(boolean interruptThreads) {
		if(interruptThreads) {
			executorService.shutdownNow();
		} else {
			executorService.shutdown();
		}
	}
}
