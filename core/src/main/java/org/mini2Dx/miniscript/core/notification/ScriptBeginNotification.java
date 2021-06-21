/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.notification;

import org.mini2Dx.miniscript.core.ScriptInvocationListener;

import java.util.concurrent.atomic.AtomicBoolean;

public class ScriptBeginNotification implements ScriptNotification {
	private final AtomicBoolean processed = new AtomicBoolean(false);
	private final ScriptInvocationListener invocationListener;
	private final int scriptId;

	public ScriptBeginNotification(ScriptInvocationListener invocationListener, int scriptId) {
		this.invocationListener = invocationListener;
		this.scriptId = scriptId;
	}
	@Override
	public void process() {
		invocationListener.onScriptBegin(scriptId);
		processed.set(true);

		synchronized(this) {
			this.notifyAll();
		}
	}

	public boolean isProcessed() {
		return processed.get();
	}
}
