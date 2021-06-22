/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.notification;

import org.mini2Dx.miniscript.core.ScriptInvocationListener;

public class ScriptCancelledNotification implements ScriptNotification {
	private final ScriptInvocationListener invocationListener;
	private final int scriptId;

	public ScriptCancelledNotification(ScriptInvocationListener invocationListener, int scriptId) {
		this.invocationListener = invocationListener;
		this.scriptId = scriptId;
	}

	@Override
	public void process() {
		invocationListener.onScriptCancelled(scriptId);
	}
}
