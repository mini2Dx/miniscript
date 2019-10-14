/**
 * Copyright 2019 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface ThreadPoolProvider {

	Future<?> submit(Runnable task);

	ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

	void shutdown();

}
