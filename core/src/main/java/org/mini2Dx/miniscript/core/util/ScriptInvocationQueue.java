/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import org.mini2Dx.lockprovider.ReadWriteLock;
import org.mini2Dx.lockprovider.ReentrantLock;
import org.mini2Dx.miniscript.core.GameScriptingEngine;
import org.mini2Dx.miniscript.core.ScriptInvocation;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class ScriptInvocationQueue extends AbstractConcurrentBlockingQueue<ScriptInvocation> {
	private final AtomicBoolean interactiveScriptRunning = new AtomicBoolean(false);

	protected final ReadWriteLock interactiveScriptLock = GameScriptingEngine.LOCK_PROVIDER.newReadWriteLock();
	protected final Queue<ScriptInvocation> interactiveScriptQueue = new PriorityQueue<>();;

	public ScriptInvocationQueue() {
		super(Integer.MAX_VALUE, new PriorityQueue<ScriptInvocation>());
	}

	@Override
	public ScriptInvocation poll() {
		ScriptInvocation result = null;
		if(!interactiveScriptRunning.get()) {
			result = pollInteractiveScript();
		}
		if(result == null) {
			result = super.poll();
		}
		return result;
	}

	private ScriptInvocation pollInteractiveScript() {
		interactiveScriptLock.lockRead();
		boolean scriptRunning = interactiveScriptRunning.get();
		boolean queueEmpty = interactiveScriptQueue.isEmpty();
		interactiveScriptLock.unlockRead();

		if(scriptRunning) {
			return null;
		}
		if(queueEmpty) {
			return null;
		}

		ScriptInvocation result = null;
		interactiveScriptLock.lockWrite();
		if(!interactiveScriptRunning.get()) {
			result = interactiveScriptQueue.poll();
			if(result != null) {
				interactiveScriptRunning.set(true);
			}
		}
		interactiveScriptLock.unlockWrite();
		return result;
	}

	@Override
	public boolean offer(ScriptInvocation scriptInvocation) {
		if(scriptInvocation.isInteractive()) {
			interactiveScriptLock.lockWrite();
			boolean result = interactiveScriptQueue.offer(scriptInvocation);
			interactiveScriptLock.unlockWrite();
			return result;
		}
		return super.offer(scriptInvocation);
	}

	@Override
	public int size() {
		interactiveScriptLock.lockRead();
		final int result = interactiveScriptQueue.size();
		interactiveScriptLock.unlockRead();
		return result + super.size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public void clear() {
		interactiveScriptLock.lockWrite();
		interactiveScriptQueue.clear();
		interactiveScriptLock.unlockWrite();
		super.clear();
	}

	public void cancelByScriptId(int scriptId) {
		interactiveScriptLock.lockWrite();
		interactiveScriptQueue.removeIf(new Predicate<ScriptInvocation>() {
			@Override
			public boolean test(ScriptInvocation scriptInvocation) {
				return scriptInvocation.getScriptId() == scriptId;
			}
		});
		interactiveScriptLock.unlockWrite();

		lock.lockWrite();
		internalQueue.removeIf(new Predicate<ScriptInvocation>() {
			@Override
			public boolean test(ScriptInvocation scriptInvocation) {
				return scriptInvocation.getScriptId() == scriptId;
			}
		});
		lock.unlockWrite();
	}

	public void cancelByTaskId(int taskId) {
		interactiveScriptLock.lockWrite();
		interactiveScriptQueue.removeIf(new Predicate<ScriptInvocation>() {
			@Override
			public boolean test(ScriptInvocation scriptInvocation) {
				return scriptInvocation.getTaskId() == taskId;
			}
		});
		interactiveScriptLock.unlockWrite();

		lock.lockWrite();
		internalQueue.removeIf(new Predicate<ScriptInvocation>() {
			@Override
			public boolean test(ScriptInvocation scriptInvocation) {
				return scriptInvocation.getTaskId() == taskId;
			}
		});
		lock.unlockWrite();
	}

	public void clearInteractiveScriptQueue() {
		interactiveScriptLock.lockWrite();
		interactiveScriptQueue.clear();
		interactiveScriptLock.unlockWrite();
	}

	public void clearNonInteractiveScriptQueue() {
		lock.lockWrite();
		internalQueue.clear();
		lock.unlockWrite();
	}

	public void clearInteractiveScriptStatus() {
		interactiveScriptLock.lockWrite();
		interactiveScriptRunning.set(false);
		interactiveScriptLock.unlockWrite();
	}

	public boolean isInteractiveScriptRunnung() {
		return interactiveScriptRunning.get();
	}

	public int getInteractiveScriptsQueued() {
		interactiveScriptLock.lockRead();
		final int result = interactiveScriptQueue.size();
		interactiveScriptLock.unlockRead();
		return result;
	}

	public int getNonInteractiveScriptsQueued() {
		return super.size();
	}
}
