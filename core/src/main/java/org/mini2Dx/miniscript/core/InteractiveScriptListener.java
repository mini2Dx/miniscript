/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core;

import org.mini2Dx.miniscript.core.notification.ScriptBeginNotification;
import org.mini2Dx.miniscript.core.notification.ScriptExceptionNotification;
import org.mini2Dx.miniscript.core.notification.ScriptSkippedNotification;
import org.mini2Dx.miniscript.core.notification.ScriptSuccessNotification;
import org.mini2Dx.miniscript.core.util.ScriptInvocationQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class InteractiveScriptListener implements ScriptInvocationListener {
	private final AtomicInteger scriptId = new AtomicInteger();
	private final AtomicReference<ScriptInvocationListener> invocationListener = new AtomicReference<>();

	private final GameScriptingEngine scriptingEngine;
	private final ScriptInvocationQueue invocationQueue;

	public InteractiveScriptListener(GameScriptingEngine scriptingEngine, ScriptInvocationQueue invocationQueue) {
		this.scriptingEngine = scriptingEngine;
		this.invocationQueue = invocationQueue;
	}

	public void track(int scriptId, ScriptInvocationListener invocationListener) {
		this.scriptId.set(scriptId);
		this.invocationListener.set(invocationListener);
	}

	@Override
	public void onScriptBegin(int scriptId) {
		final ScriptInvocationListener invocationListener = this.invocationListener.get();
		if(scriptId == this.scriptId.get()) {
			if(invocationListener != null) {
				if (invocationListener.callOnGameThread()) {
					scriptingEngine.scriptNotifications.offer(
							new ScriptBeginNotification(invocationListener, scriptId));
				} else {
					invocationListener.onScriptBegin(scriptId);
				}
			}
		}
	}

	@Override
	public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
		final ScriptInvocationListener invocationListener = this.invocationListener.get();
		if(scriptId == this.scriptId.get()) {
			invocationQueue.clearInteractiveScriptStatus();

			if (invocationListener != null) {
				if (invocationListener.callOnGameThread()) {
					scriptingEngine.scriptNotifications.offer(
							new ScriptSuccessNotification(invocationListener, scriptId, executionResult));
				} else {
					invocationListener.onScriptSuccess(scriptId, executionResult);
				}
			}
		}
	}

	@Override
	public void onScriptSkipped(int scriptId) {
		final ScriptInvocationListener invocationListener = this.invocationListener.get();
		if(scriptId == this.scriptId.get()) {
			invocationQueue.clearInteractiveScriptStatus();

			if (invocationListener != null) {
				if (invocationListener.callOnGameThread()) {
					scriptingEngine.scriptNotifications
							.offer(new ScriptSkippedNotification(invocationListener, scriptId));
				} else {
					invocationListener.onScriptSkipped(scriptId);
				}
			}
		}
	}

	@Override
	public void onScriptException(int scriptId, Exception e) {
		final ScriptInvocationListener invocationListener = this.invocationListener.get();
		if(scriptId == this.scriptId.get()) {
			invocationQueue.clearInteractiveScriptStatus();

			if (invocationListener != null) {
				if (invocationListener.callOnGameThread()) {
					scriptingEngine.scriptNotifications
							.offer(new ScriptExceptionNotification(invocationListener, scriptId, e));
				} else {
					invocationListener.onScriptException(scriptId, e);
				}
			} else {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean callOnGameThread() {
		return false;
	}
}
