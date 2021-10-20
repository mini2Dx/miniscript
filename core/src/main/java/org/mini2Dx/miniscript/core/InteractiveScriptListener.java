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
	/**
	 * When true sends notifications then allows next interaction script.
	 * Defaults to false which allows next interaction script then notifies.
	 */
	public static boolean NOTIFY_THEN_ALLOW_INTERACTION = false;
	/**
	 * When true waits for game thread notifications to be processed before proceeding
	 */
	public static boolean WAIT_FOR_GAME_THREAD_NOTIFICATIONS = false;

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
		if(scriptId != this.scriptId.get()) {
			return;
		}
		if(invocationListener == null) {
			return;
		}
		if (invocationListener.callOnGameThread()) {
			scriptingEngine.scriptNotifications.offer(
					new ScriptBeginNotification(invocationListener, scriptId));
		} else {
			invocationListener.onScriptBegin(scriptId);
		}
	}

	@Override
	public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
		final ScriptInvocationListener invocationListener = this.invocationListener.get();
		if(scriptId != this.scriptId.get()) {
			return;
		}
		clearInteractionStatusBeforeNotification();

		try {
			if (invocationListener != null) {
				if (invocationListener.callOnGameThread()) {
					final ScriptSuccessNotification notification = new ScriptSuccessNotification(
							invocationListener, scriptId, executionResult);
					scriptingEngine.scriptNotifications.offer(notification);
					if(WAIT_FOR_GAME_THREAD_NOTIFICATIONS) {
						notification.waitForNotification();
					}
				} else {
					invocationListener.onScriptSuccess(scriptId, executionResult);
				}
			}
		} finally {
			clearInteractionStatusAfterNotification();
		}
	}

	@Override
	public void onScriptSkipped(int scriptId) {
		final ScriptInvocationListener invocationListener = this.invocationListener.get();
		if(scriptId != this.scriptId.get()) {
			return;
		}
		clearInteractionStatusBeforeNotification();

		try {
			if (invocationListener != null) {
				if (invocationListener.callOnGameThread()) {
					final ScriptSkippedNotification notification =
							new ScriptSkippedNotification(invocationListener, scriptId);
					scriptingEngine.scriptNotifications.offer(notification);
					if(WAIT_FOR_GAME_THREAD_NOTIFICATIONS) {
						notification.waitForNotification();
					}
				} else {
					invocationListener.onScriptSkipped(scriptId);
				}
			}
		} finally {
			clearInteractionStatusAfterNotification();
		}
	}

	@Override
	public void onScriptException(int scriptId, Exception e) {
		final ScriptInvocationListener invocationListener = this.invocationListener.get();
		if(scriptId != this.scriptId.get()) {
			return;
		}
		clearInteractionStatusBeforeNotification();

		try {
			if (invocationListener != null) {
				if (invocationListener.callOnGameThread()) {
					ScriptExceptionNotification notification =
							new ScriptExceptionNotification(invocationListener, scriptId, e);
					scriptingEngine.scriptNotifications.offer(notification);
					if(WAIT_FOR_GAME_THREAD_NOTIFICATIONS) {
						notification.waitForNotification();
					}
				} else {
					invocationListener.onScriptException(scriptId, e);
				}
			} else {
				e.printStackTrace();
			}
		} finally {
			clearInteractionStatusAfterNotification();
		}
	}

	private void clearInteractionStatusBeforeNotification() {
		if(NOTIFY_THEN_ALLOW_INTERACTION) {
			return;
		}
		invocationQueue.clearInteractiveScriptStatus();
	}

	private void clearInteractionStatusAfterNotification() {
		if(!NOTIFY_THEN_ALLOW_INTERACTION) {
			return;
		}
		invocationQueue.clearInteractiveScriptStatus();
	}

	public ScriptInvocationListener getInvocationListener() {
		return invocationListener.get();
	}

	@Override
	public boolean callOnGameThread() {
		return false;
	}
}
