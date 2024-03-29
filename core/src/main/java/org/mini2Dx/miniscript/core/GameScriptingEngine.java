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
package org.mini2Dx.miniscript.core;

import org.mini2Dx.lockprovider.Locks;
import org.mini2Dx.lockprovider.jvm.JvmLocks;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;
import org.mini2Dx.miniscript.core.notification.ScriptCancelledNotification;
import org.mini2Dx.miniscript.core.notification.ScriptNotification;
import org.mini2Dx.miniscript.core.notification.ScriptSkippedNotification;
import org.mini2Dx.miniscript.core.threadpool.DefaultThreadPoolProvider;
import org.mini2Dx.miniscript.core.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides scripting functionality to your game
 * 
 * Note that this is a base class for each scripting language implementation.
 */
public abstract class GameScriptingEngine implements Runnable {
	/**
	 * Returns the most recently created {@link GameScriptingEngine}
	 */
	public static GameScriptingEngine MOST_RECENT_INSTANCE = null;

	public static Locks LOCK_PROVIDER = new JvmLocks();

	private static final int DEFAULT_MAX_CONCURRENT_SCRIPTS = 2;

	private final ScriptInvocationPool scriptInvocationPool = new ScriptInvocationPool();
	private final ScriptInvocationQueue scriptInvocationQueue = new ScriptInvocationQueue();
	final Queue<ScriptNotification> scriptNotifications = new ReadWriteArrayQueue<ScriptNotification>();
	private final InteractiveScriptListener interactiveScriptListener;

	final ReadWriteArrayQueue<GameFuture> queuedFutures = new ReadWriteArrayQueue<>();
	final ReadWriteIntMap<GameFuture> runningFutures = new ReadWriteIntMap<GameFuture>();
	private final ReadWriteIntMap<ScriptExecutionTask<?>> runningScripts = new ReadWriteIntMap<ScriptExecutionTask<?>>();
	private final IntSet completedFutures = new IntSet();
	private final IntSet completedScripts = new IntSet();

	private final List<String> tmpRunningScripts = new ArrayList<>();

	private final ThreadPoolProvider threadPoolProvider;
	private final ScriptExecutorPool<?> scriptExecutorPool;

	private ScheduledFuture cleanupTask;
	private boolean cancelReallocatedFutures = true;

	private Thread gameThread = null;

	private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

	/**
	 * Constructs a scripting engine backed by a thread pool with the maximum
	 * amount of concurrent scripts set to 2.
	 * Sandboxing is enabled if the implementation supports it.
	 */
	public GameScriptingEngine() {
		this(DEFAULT_MAX_CONCURRENT_SCRIPTS);
	}

	public GameScriptingEngine(ThreadPoolProvider threadPoolProvider) {
		this(DEFAULT_MAX_CONCURRENT_SCRIPTS, threadPoolProvider);
	}

	/**
	 * Constructs a scripting engine backed by a thread pool. Sandboxing is
	 * enabled if the implementation supports it.
	 * 
	 * @param maxConcurrentScripts
	 *            The maximum amount of concurrently running scripts. WARNING:
	 *            this is a 'requested' amount and may be less due to the amount
	 *            of available processors on the player's machine.
	 */
	public GameScriptingEngine(int maxConcurrentScripts) {
		this(new NoopClasspathScriptProvider(), maxConcurrentScripts);
	}

	public GameScriptingEngine(int maxConcurrentScripts, ThreadPoolProvider threadPoolProvider) {
		this(new NoopClasspathScriptProvider(), maxConcurrentScripts, threadPoolProvider);
	}

	/**
	 * Constructs a scripting engine backed by a thread pool. Sandboxing is
	 * enabled if the implementation supports it.
	 * @param classpathScriptProvider The auto-generated {@link ClasspathScriptProvider} for the game
	 * @param maxConcurrentScripts The maximum amount of concurrently running scripts. WARNING:
	 *                                this is a 'requested' amount and may be less due to the amount
	 *                                of available processors on the player's machine.
	 */
	public GameScriptingEngine(ClasspathScriptProvider classpathScriptProvider, int maxConcurrentScripts) {
		super();
		interactiveScriptListener = new InteractiveScriptListener(this, scriptInvocationQueue);
		scriptExecutorPool = createScriptExecutorPool(classpathScriptProvider, maxConcurrentScripts, isSandboxingSupported());

		threadPoolProvider = new DefaultThreadPoolProvider(maxConcurrentScripts + 1);
		init(maxConcurrentScripts);
	}

	public GameScriptingEngine(ClasspathScriptProvider classpathScriptProvider, int maxConcurrentScripts, ThreadPoolProvider threadPoolProvider) {
		super();
		interactiveScriptListener = new InteractiveScriptListener(this, scriptInvocationQueue);
		scriptExecutorPool = createScriptExecutorPool(classpathScriptProvider, maxConcurrentScripts, isSandboxingSupported());

		this.threadPoolProvider = threadPoolProvider;
		init(maxConcurrentScripts);
	}

	/**
	 * Constructs a scripting engine backed by a thread pool with the maximum
	 * amount of concurrent scripts set to the amount of processors + 1.
	 * 
	 * @param sandboxed
	 *            True if script sandboxing should be enabled
	 */
	public GameScriptingEngine(boolean sandboxed) {
		this(new NoopClasspathScriptProvider(), sandboxed);
	}

	public GameScriptingEngine(ThreadPoolProvider threadPoolProvider, boolean sandboxed) {
		this(new NoopClasspathScriptProvider(), threadPoolProvider, sandboxed);
	}

	/**
	 * Constructs a scripting engine backed by a thread pool with the maximum
	 * amount of concurrent scripts set to 2.
	 * @param classpathScriptProvider The auto-generated {@link ClasspathScriptProvider} for the game
	 * @param sandboxed True if script sandboxing should be enabled
	 */
	public GameScriptingEngine(ClasspathScriptProvider classpathScriptProvider, boolean sandboxed) {
		this(classpathScriptProvider,DEFAULT_MAX_CONCURRENT_SCRIPTS, sandboxed);
	}

	public GameScriptingEngine(ClasspathScriptProvider classpathScriptProvider, ThreadPoolProvider threadPoolProvider, boolean sandboxed) {
		this(classpathScriptProvider,DEFAULT_MAX_CONCURRENT_SCRIPTS, threadPoolProvider, sandboxed);
	}

	/**
	 * Constructs a scripting engine backed by a thread pool.
	 * 
	 * @param maxConcurrentScripts
	 *            The maximum amount of concurrently running scripts. WARNING:
	 *            this is a 'requested' amount and may be less due to the amount
	 *            of available processors on the player's machine.
	 * @param sandboxed
	 *            True if script sandboxing should be enabled
	 */
	public GameScriptingEngine(int maxConcurrentScripts, boolean sandboxed) {
		this(new NoopClasspathScriptProvider(), maxConcurrentScripts, sandboxed);
	}

	public GameScriptingEngine(int maxConcurrentScripts, ThreadPoolProvider threadPoolProvider, boolean sandboxed) {
		this(new NoopClasspathScriptProvider(), maxConcurrentScripts, threadPoolProvider, sandboxed);
	}

	/**
	 * Constructs a scripting engine backed by a thread pool.
	 * @param classpathScriptProvider The auto-generated {@link ClasspathScriptProvider} for the game
	 * @param maxConcurrentScripts The maximum amount of concurrently running scripts. WARNING:
	 *                                this is a 'requested' amount and may be less due to the amount
	 *                                of available processors on the player's machine.
	 * @param sandboxed True if script sandboxing should be enabled
	 */
	public GameScriptingEngine(ClasspathScriptProvider classpathScriptProvider,
	                           int maxConcurrentScripts, boolean sandboxed) {
		super();
		interactiveScriptListener = new InteractiveScriptListener(this, scriptInvocationQueue);
		scriptExecutorPool = createScriptExecutorPool(classpathScriptProvider, maxConcurrentScripts, sandboxed);

		threadPoolProvider = new DefaultThreadPoolProvider(maxConcurrentScripts + 1);
		init(maxConcurrentScripts);
	}

	public GameScriptingEngine(ClasspathScriptProvider classpathScriptProvider,
	                           int maxConcurrentScripts, ThreadPoolProvider threadPoolProvider, boolean sandboxed) {
		super();
		interactiveScriptListener = new InteractiveScriptListener(this, scriptInvocationQueue);
		scriptExecutorPool = createScriptExecutorPool(classpathScriptProvider, maxConcurrentScripts, sandboxed);

		this.threadPoolProvider = threadPoolProvider;
		init(maxConcurrentScripts);
	}

	private void init(int maxConcurrentScripts) {
		for(int i = 0; i < maxConcurrentScripts; i++) {
			threadPoolProvider.scheduleAtFixedRate(this, 16L, 16L, TimeUnit.MILLISECONDS);
		}
		threadPoolProvider.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					cleanupCompletedFutures();
					cleanupCompletedScripts();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 0L, 1L, TimeUnit.SECONDS);
		MOST_RECENT_INSTANCE = this;
	}

	/**
	 * Shuts down the thread pool and cleans up resources
	 */
	public void dispose() {
		dispose(false);
	}

	/**
	 * Shuts down the thread pool and cleans up resources
	 * @param interruptScripts True if running scripts should be interrupted
	 */
	public void dispose(boolean interruptScripts) {
		shuttingDown.set(true);

		if(cleanupTask != null) {
			cleanupTask.cancel(false);
			cleanupTask = null;
		}
		threadPoolProvider.shutdown(interruptScripts);

		if(!interruptScripts) {
			return;
		}
		cancelAllQueuedScripts();
		skipAllQueuedGameFutures();
		skipAllScripts();
	}

	/**
	 * Checks if sandboxing is supported by the {@link GameScriptingEngine}
	 * implementation
	 * 
	 * @return True if sandboxing is supported
	 */
	public abstract boolean isSandboxingSupported();

	/**
	 * Checks if scripts can be synchronously run within other scripts
	 *
	 * @return True if scripts can be invoke synchronously within scripts
	 */
	public abstract boolean isEmbeddedSynchronousScriptSupported();

	protected abstract ScriptExecutorPool<?> createScriptExecutorPool(ClasspathScriptProvider classpathScriptProvider,
	                                                                  int poolSize, boolean sandboxing);

	/**
	 * Updates all {@link GameFuture}s
	 * 
	 * @param delta
	 *            The time (in seconds) since the last frame update
	 */
	public void update(float delta) {
		if(gameThread == null) {
			gameThread = Thread.currentThread();
		}

		for (GameFuture gameFuture : runningFutures.values()) {
			if(gameFuture == null) {
				continue;
			}
			gameFuture.evaluate(delta);
		}

		while (!queuedFutures.isEmpty()) {
			GameFuture nextFuture = queuedFutures.poll();
			GameFuture previousFuture = runningFutures.put(nextFuture.getFutureId(), nextFuture);
			if (previousFuture == null) {
				continue;
			}
			if (!cancelReallocatedFutures) {
				continue;
			}
			try {
				previousFuture.skipFuture();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		while (!scriptNotifications.isEmpty()) {
			scriptNotifications.poll().process();
		}
	}

	/**
	 * This should not be invoked by the developer. Call {@link #update(float)}
	 * instead.
	 */
	@Override
	public void run() {
		if(shuttingDown.get()) {
			return;
		}
		long startTime = System.currentTimeMillis();
		ScriptInvocation scriptInvocation = null;
		try {
			while ((scriptInvocation = scriptInvocationQueue.poll()) != null) {
				if(shuttingDown.get()) {
					continue;
				}
				final ScriptInvocationListener invocationListener;
				if(scriptInvocation.isInteractive()) {
					interactiveScriptListener.track(scriptInvocation.getScriptId(), scriptInvocation.getInvocationListener());
					invocationListener = interactiveScriptListener;
				} else {
					invocationListener = scriptInvocation.getInvocationListener();
				}

				ScriptExecutionTask<?> executionTask = scriptExecutorPool.execute(scriptInvocation.getTaskId(),
						scriptInvocation.getScriptId(), scriptInvocation.getScriptBindings(), invocationListener, false);
				Future<?> taskFuture = threadPoolProvider.submit(executionTask);
				executionTask.setTaskFuture(taskFuture);
				runningScripts.put(executionTask.getTaskId(), executionTask);
				scriptInvocation.release();
			}
		} catch (NoSuchScriptException e) {
			if(scriptInvocation != null) {
				if(scriptInvocation.getInvocationListener() != null) {
					scriptInvocation.getInvocationListener().onScriptException(scriptInvocation.getScriptId(), e);
				}
				interactiveScriptListener.onScriptException(scriptInvocation.getScriptId(), e);
			} else {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(shuttingDown.get()) {
			return;
		}
	}

	private void cleanupCompletedFutures() {
		for (GameFuture gameFuture : runningFutures.values()) {
			if(gameFuture == null) {
				continue;
			}
			if (gameFuture.isReadyForGC()) {
				completedFutures.add(gameFuture.getFutureId());
			}
		}
		final IntSet.IntSetIterator iterator = completedFutures.iterator();
		while(iterator.hasNext) {
			final int futureId = iterator.next();
			runningFutures.remove(futureId);
		}
		completedFutures.clear();
	}

	private void cleanupCompletedScripts() {
		for (ScriptExecutionTask<?> scriptExecutionTask : runningScripts.values()) {
			if(scriptExecutionTask == null) {
				continue;
			}
			if (scriptExecutionTask.isFinished()) {
				scriptExecutionTask.cleanup();
				completedScripts.add(scriptExecutionTask.getTaskId());
			}
		}
		final IntSet.IntSetIterator iterator = completedScripts.iterator();
		while(iterator.hasNext) {
			final int taskId = iterator.next();
			runningScripts.remove(taskId);
		}
		completedScripts.clear();
	}

	/**
	 * Skips all currently running scripts
	 */
	public void skipAllScripts() {
		for (ScriptExecutionTask<?> scriptExecutionTask : runningScripts.values()) {
			if(scriptExecutionTask == null) {
				continue;
			}
			scriptExecutionTask.skipScript();
		}
	}

	/**
	 * Skips all running instances of a script
	 * 
	 * @param scriptId
	 *            The ID of the script to skip
	 */
	public void skipScript(int scriptId) {
		final IntMap.Keys keys = runningScripts.keys();
		while(keys.hasNext) {
			final int taskId = keys.next();
			ScriptExecutionTask<?> scriptExecutionTask = runningScripts.get(taskId);
			if (scriptExecutionTask == null) {
				continue;
			}
			if (scriptExecutionTask.getScriptId() != scriptId) {
				continue;
			}
			scriptExecutionTask.skipScript();
		}
	}

	/**
	 * Skips a specific running instance of a script
	 *
	 * @param taskId The ID of the task to skip
	 */
	public void skipScriptByTaskId(int taskId) {
		final IntMap.Keys keys = runningScripts.keys();
		while(keys.hasNext) {
			final int otherTaskId = keys.next();
			if(taskId != otherTaskId) {
				continue;
			}
			ScriptExecutionTask<?> scriptExecutionTask = runningScripts.get(taskId);
			if (scriptExecutionTask == null) {
				continue;
			}
			scriptExecutionTask.skipScript();
		}
	}

	/**
	 * Skips all currently running {@link GameFuture}s
	 */
	public void skipAllRunningGameFutures() {
		for (GameFuture gameFuture : runningFutures.values()) {
			if(gameFuture == null) {
				continue;
			}
			gameFuture.skipFuture();
		}
	}

	/**
	 * Skips all currently queued {@link GameFuture}s
	 */
	public void skipAllQueuedGameFutures() {
		while(!queuedFutures.isEmpty()) {
			final GameFuture gameFuture = queuedFutures.poll();
			if(gameFuture == null) {
				continue;
			}
			gameFuture.skipFuture();
		}
	}

	/**
	 * Removes all currently running {@link GameFuture}s without sending skipFuture event
	 */
	public void cancelAllRunningGameFutures() {
		runningFutures.clear();
	}

	/**
	 * Removes all currently queued {@link GameFuture}s without sending skipFuture event
	 */
	public void cancelAllQueuedGameFutures() {
		queuedFutures.clear();
	}

	/**
	 * Removes all currently queued {@link GameFuture}s without sending skipFuture event
	 */
	public void cancelAllQueuedScripts() {
		cancelAllQueuedScripts(true);
	}

	/**
	 * Removes all currently queued {@link GameFuture}s without sending skipFuture event
	 * @param notifyListeners If true will notify invocation listeners of script cancellation
	 */
	public void cancelAllQueuedScripts(boolean notifyListeners) {
		if(!notifyListeners) {
			scriptInvocationQueue.clear();
			return;
		}

		final List<ScriptInvocation> scriptInvocations = new ArrayList<>();
		scriptInvocationQueue.clear(scriptInvocations);
		notifyScriptCancelled(scriptInvocations);
	}

	/**
	 * Removes all currently queued scripts with a given script ID
	 */
	public void cancelQueuedScript(int scriptId) {
		cancelQueuedScript(scriptId, true);
	}

	/**
	 * Removes a specific queued script by its task ID
	 */
	public void cancelQueuedScriptByTaskId(int taskId) {
		cancelQueuedScriptByTaskId(taskId, true);
	}

	/**
	 * Removes all currently queued scripts with a given script ID
	 * @param notifyListeners If true will notify invocation listeners of script cancellation
	 */
	public void cancelQueuedScript(int scriptId, boolean notifyListeners) {
		final List<ScriptInvocation> scriptInvocations = new ArrayList<>();
		scriptInvocationQueue.cancelByScriptId(scriptId, scriptInvocations);
		if(!notifyListeners) {
			scriptInvocations.clear();
			return;
		}

		notifyScriptCancelled(scriptInvocations);
	}

	/**
	 * Removes a specific queued script by its task ID
	 * @param notifyListeners If true will notify invocation listeners of script cancellation
	 */
	public void cancelQueuedScriptByTaskId(int taskId, boolean notifyListeners) {
		final List<ScriptInvocation> scriptInvocations = new ArrayList<>();
		scriptInvocationQueue.cancelByTaskId(taskId, scriptInvocations);
		if(!notifyListeners) {
			scriptInvocations.clear();
			return;
		}

		notifyScriptCancelled(scriptInvocations);
	}

	/**
	 * Clears all interactive scripts queued
	 */
	public void cancelAllQueuedInteractiveScripts() {
		cancelAllQueuedInteractiveScripts(true);
	}

	/**
	 * Clears all non-interactive scripts queued
	 */
	public void cancelAllQueuedNonInteractiveScripts() {
		cancelAllQueuedNonInteractiveScripts(true);
	}

	/**
	 * Clears all interactive scripts queued
	 * @param notifyListeners If true will send the scriptSkipped event any listener associated with a cancelled invoke
	 */
	public void cancelAllQueuedInteractiveScripts(boolean notifyListeners) {
		if(!notifyListeners) {
			scriptInvocationQueue.clearInteractiveScriptQueue();
			return;
		}

		final List<ScriptInvocation> scriptInvocations = new ArrayList<>();
		scriptInvocationQueue.clearInteractiveScriptQueue(scriptInvocations);
		notifyScriptCancelled(scriptInvocations);
	}

	/**
	 * Clears all non-interactive scripts queued
	 * @param notifyListeners If true will send the scriptSkipped event any listener associated with a cancelled invoke
	 */
	public void cancelAllQueuedNonInteractiveScripts(boolean notifyListeners) {
		if(!notifyListeners) {
			scriptInvocationQueue.clearNonInteractiveScriptQueue();
			return;
		}

		final List<ScriptInvocation> scriptInvocations = new ArrayList<>();
		scriptInvocationQueue.clearNonInteractiveScriptQueue(scriptInvocations);
		notifyScriptCancelled(scriptInvocations);
	}

	/**
	 * Cancels a queued script or skips it if it is currently running
	 * @param scriptId The script ID
	 */
	public void skipOrCancelScript(int scriptId) {
		try {
			skipScript(scriptId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			cancelQueuedScript(scriptId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Cancels a specific invocation of a script by its task ID, or skips it if it is currently running
	 * @param taskId The task ID
	 */
	public void skipOrCancelScriptByTaskId(int taskId) {
		try {
			skipScriptByTaskId(taskId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			cancelQueuedScriptByTaskId(taskId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compiles a script for execution. Note it is best to call this
	 * sequentially before any script executions to avoid throwing a
	 * {@link InsufficientCompilersException}.
	 *
	 * Note: If the filepath has already been compiled, the script will not be compiled and the previous compilation is used.
	 *
	 * @param filepath The filepath to store for the script
	 * @param scriptContent
	 *            The text contents of the script
	 * @return The unique id for the script
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 */
	public int compileScript(String filepath, String scriptContent) throws InsufficientCompilersException {
		final int existingId = scriptExecutorPool.getCompiledScriptId(filepath);
		if(existingId > -1) {
			return existingId;
		}
		return scriptExecutorPool.preCompileScript(filepath, scriptContent);
	}

	/**
	 * Compiles a script for execution. Note it is best to call this
	 * sequentially before any script executions to avoid throwing a
	 * {@link InsufficientCompilersException}.
	 *
	 * @param scriptContent
	 *            The text contents of the script
	 * @return The unique id for the script
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 */
	public int compileScript(String scriptContent) throws InsufficientCompilersException {
		return compileScript(String.valueOf(scriptContent.hashCode()), scriptContent);
	}

	/**
	 * Returns the script ID for a given filepath
	 * @param filepath The filepath to lookup
	 * @return -1 if the script has not been compiled
	 */
	public int getCompiledScriptId(String filepath) {
		return scriptExecutorPool.getCompiledScriptId(filepath);
	}

	/**
	 * Compiles a script for execution. Note it is best to call this
	 * sequentially before any script executions to avoid throwing a
	 * {@link InsufficientCompilersException}
	 * 
	 * @param inputStream
	 *            The {@link InputStream} to read the script contents from
	 * @return The unique id for the script
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 * @throws IOException
	 *             Throw if the {@link InputStream} could not be read or closed
	 */
	public int compileScript(String filepath, InputStream inputStream) throws InsufficientCompilersException, IOException {
		Scanner scanner = new Scanner(inputStream);
		scanner.useDelimiter("\\A");
		String contents = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		inputStream.close();
		return compileScript(filepath, contents);
	}

	/**
	 * Queues a compiled script for execution in the engine's thread pool
	 * 
	 * @param scriptId
	 *            The id of the script to run
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @return The unique task ID for this invocation
	 */
	public int invokeCompiledScript(int scriptId, ScriptBindings scriptBindings) {
		return invokeCompiledScript(scriptId, scriptBindings, null);
	}

	/**
	 * Queues a compiled script for execution in the engine's thread pool
	 * 
	 * @param scriptId
	 *            The id of the script to run
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @param invocationListener
	 *            A {@link ScriptInvocationListener} to list for invocation
	 *            results
	 * @return The unique task ID for this invocation
	 */
	public int invokeCompiledScript(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		return invokeCompiledScript(scriptId, scriptBindings, invocationListener, 0);
	}

	/**
	 * Queues a compiled script for execution in the engine's thread pool
	 *
	 * @param scriptId
	 *            The id of the script to run
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @param invocationListener
	 *            A {@link ScriptInvocationListener} to list for invocation results
	 * @param priority The script execution priority (higher value = higher priority)
	 * @return The unique task ID for this invocation
	 */
	public int invokeCompiledScript(int scriptId, ScriptBindings scriptBindings,
									 ScriptInvocationListener invocationListener, int priority) {
		return invokeCompiledScript(scriptId, scriptBindings, invocationListener, priority, false);
	}

	/**
	 * Queues a compiled script for execution in the engine's thread pool
	 *
	 * @param scriptId
	 *            The id of the script to run
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @param invocationListener
	 *            A {@link ScriptInvocationListener} to list for invocation results
	 * @param priority The script execution priority (higher value = higher priority)
	 * @return The unique task ID for this invocation
	 */
	public int invokeCompiledScript(int scriptId, ScriptBindings scriptBindings,
	                                 ScriptInvocationListener invocationListener, int priority, boolean interactive) {
		final ScriptInvocation invocation = scriptInvocationPool.allocate(scriptId, scriptBindings, invocationListener, priority, interactive);
		scriptInvocationQueue.offer(invocation);
		return invocation.getTaskId();
	}

	/**
	 * Compiles and queues a script for execution in the engine's thread pool
	 * 
	 * @param scriptContent
	 *            The text content of the script
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @return The unique id for the script
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 */
	public int invokeScript(String scriptContent, ScriptBindings scriptBindings) throws InsufficientCompilersException {
		return invokeScript(scriptContent, scriptBindings, null);
	}

	/**
	 * Compiles and queues a script for execution in the engine's thread pool
	 * 
	 * @param scriptContent
	 *            The text content of the script
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @param invocationListener
	 *            A {@link ScriptInvocationListener} to list for invocation
	 *            results
	 * @return The unique id for the script
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 */
	public int invokeScript(String scriptContent, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) throws InsufficientCompilersException {
		int scriptId = compileScript(String.valueOf(scriptContent.hashCode()), scriptContent);
		invokeCompiledScript(scriptId, scriptBindings, invocationListener);
		return scriptId;
	}

	/**
	 * Compiles and queues a script for execution in the engine's thread pool
	 *
	 * @param scriptContent
	 *            The text content of the script
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @param invocationListener
	 *            A {@link ScriptInvocationListener} to list for invocation
	 *            results
	 * @param priority The script execution priority (higher value = higher priority)
	 * @return The unique id for the script
	 * @throws InsufficientCompilersException
	 *             Thrown if there are no script compilers available
	 */
	public int invokeScript(String scriptContent, ScriptBindings scriptBindings,
							ScriptInvocationListener invocationListener, int priority) throws InsufficientCompilersException {
		int scriptId = compileScript(String.valueOf(scriptContent.hashCode()), scriptContent);
		invokeCompiledScript(scriptId, scriptBindings, invocationListener, priority);
		return scriptId;
	}

	/**
	 * Executes a compiled script synchronously on the thread calling this method.
	 * 
	 * Warning: If no {@link ScriptExecutor}s are available this will block
	 * until one is available
	 *
	 * @param taskId The task id
	 * @param scriptId The script id
	 * @param scriptBindings The variable bindings for the script
	 */
	public void invokeCompiledScriptSync(int taskId, int scriptId, ScriptBindings scriptBindings) {
		invokeCompiledScriptSync(taskId, scriptId, scriptBindings, null);
	}

	/**
	 * Executes a compiled script synchronously on the thread calling this method.
	 * 
	 * Warning: If no {@link ScriptExecutor}s are available this will block
	 * until one is available
	 *
	 * @param taskId The task id
	 * @param scriptId The script id
	 * @param scriptBindings The variable bindings for the script
	 * @param invocationListener
	 *            A {@link ScriptInvocationListener} to list for invocation
	 *            results
	 */
	public void invokeCompiledScriptSync(int taskId, int scriptId, ScriptBindings scriptBindings,
										 ScriptInvocationListener invocationListener) {
		ScriptExecutionTask<?> executionTask = scriptExecutorPool.execute(taskId, scriptId, scriptBindings, invocationListener, true);
		runningScripts.put(executionTask.getTaskId(), executionTask);
		executionTask.run();
	}

	/**
	 * Returns the list of currently running scripts.
	 *
	 * Note: This list reference is re-used on every invocation of this method
	 * @return An empty list if nothing running
	 */
	public List<String> getRunningScripts() {
		tmpRunningScripts.clear();
		final IntMap.Keys keys = runningScripts.keys();
		while(keys.hasNext) {
			final int taskId = keys.next();
			ScriptExecutionTask<?> scriptExecutionTask = runningScripts.get(taskId);
			if (scriptExecutionTask == null) {
				continue;
			}
			tmpRunningScripts.add(scriptExecutorPool.getCompiledScriptPath(scriptExecutionTask.getScriptId()));
		}
		return tmpRunningScripts;
	}

	/**
	 * Returns the total scripts (interactive + non-interactive) queued
	 * @return 0 if none
	 */
	public int getTotalScriptsQueued() {
		return scriptInvocationQueue.size();
	}

	/**
	 * Returns the total interactive scripts queued
	 * @return 0 if none
	 */
	public int getTotalInteractiveScriptsQueued() {
		return scriptInvocationQueue.getInteractiveScriptsQueued();
	}

	/**
	 * Returns the total non-interactive scripts queued
	 * @return 0 if none
	 */
	public int getTotalNonInteractiveScriptsQueued() {
		return scriptInvocationQueue.getNonInteractiveScriptsQueued();
	}

	/**
	 * Returns true if interactive script is running
	 * @return
	 */
	public boolean isInteractiveScriptRunning() {
		return scriptInvocationQueue.isInteractiveScriptRunnung();
	}

	void submitGameFuture(GameFuture gameFuture) {
		if(gameThread != null && gameThread == Thread.currentThread()) {
			queuedFutures.offer(gameFuture);
		} else {
			queuedFutures.lazyOffer(gameFuture);
		}
	}

	/**
	 * Sets if newly {@link GameFuture}s should cancel a previously scheduled
	 * {@link GameFuture} with the same ID. IDs attempt be unique but if there
	 * are {@link GameFuture}s that never complete IDs may come into conflict
	 * after millions of generated {@link GameFuture}s. Defaults to true.
	 * 
	 * @param cancelReallocatedFutures
	 *            True if existing {@link GameFuture}s with same ID should be
	 *            cancelled
	 */
	public void setCancelReallocatedFutures(boolean cancelReallocatedFutures) {
		this.cancelReallocatedFutures = cancelReallocatedFutures;
	}


	private void notifyScriptCancelled(List<ScriptInvocation> scriptInvocations) {
		for(ScriptInvocation invocation : scriptInvocations) {
			if(invocation.getInvocationListener() == null) {
				return;
			}
			final ScriptInvocationListener invocationListener = invocation.getInvocationListener();
			if(invocationListener.callOnGameThread()) {
				scriptNotifications
						.offer(new ScriptCancelledNotification(invocationListener, invocation.getScriptId()));
			} else {
				invocationListener.onScriptCancelled(invocation.getScriptId());
			}
		}
		scriptInvocations.clear();
	}
}
