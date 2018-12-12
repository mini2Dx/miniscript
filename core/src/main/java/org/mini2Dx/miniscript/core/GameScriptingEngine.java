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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.notification.ScriptNotification;

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

	private final ScriptInvocationPool scriptInvocationPool = new ScriptInvocationPool();
	private final Queue<ScriptInvocation> scriptInvocations = new ConcurrentLinkedQueue<ScriptInvocation>();
	final Queue<ScriptNotification> scriptNotifications = new ConcurrentLinkedQueue<ScriptNotification>();

	final Queue<GameFuture> queuedFutures = new ConcurrentLinkedQueue<GameFuture>();
	final Map<Integer, GameFuture> runningFutures = new ConcurrentHashMap<Integer, GameFuture>();
	private final Map<Integer, ScriptExecutionTask<?>> runningScripts = new ConcurrentHashMap<Integer, ScriptExecutionTask<?>>();
	private final Set<Integer> completedFutures = new HashSet<Integer>();
	private final Set<Integer> completedScripts = new HashSet<Integer>();

	private final ScheduledExecutorService executorService;
	private final ScriptExecutorPool<?> scriptExecutorPool;

	private boolean cancelReallocatedFutures = true;

	/**
	 * Constructs a scripting engine backed by a thread pool with the maximum
	 * amount of concurrent scripts set to the amount of processors + 1.
	 * Sandboxing is enabled if the implementation supports it.
	 */
	public GameScriptingEngine() {
		this(Runtime.getRuntime().availableProcessors() + 1);
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
		scriptExecutorPool = createScriptExecutorPool(classpathScriptProvider, maxConcurrentScripts, isSandboxingSupported());

		executorService = Executors.newScheduledThreadPool(
				Math.min(maxConcurrentScripts + 1, Runtime.getRuntime().availableProcessors() * 2));
		init();
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

	/**
	 * Constructs a scripting engine backed by a thread pool with the maximum
	 * amount of concurrent scripts set to the amount of processors + 1.
	 * @param classpathScriptProvider The auto-generated {@link ClasspathScriptProvider} for the game
	 * @param sandboxed True if script sandboxing should be enabled
	 */
	public GameScriptingEngine(ClasspathScriptProvider classpathScriptProvider, boolean sandboxed) {
		this(classpathScriptProvider,Runtime.getRuntime().availableProcessors() + 1, sandboxed);
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
		scriptExecutorPool = createScriptExecutorPool(classpathScriptProvider, maxConcurrentScripts, sandboxed);

		executorService = Executors.newScheduledThreadPool(
				Math.min(maxConcurrentScripts + 1, Runtime.getRuntime().availableProcessors() * 2));
		init();
	}

	private void init() {
		executorService.submit(this);
		executorService.scheduleAtFixedRate(new Runnable() {
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
		executorService.shutdown();
	}

	/**
	 * Checks if sandboxing is supported by the {@link GameScriptingEngine}
	 * implementation
	 * 
	 * @return True if sandboxing is supported
	 */
	public abstract boolean isSandboxingSupported();

	protected abstract ScriptExecutorPool<?> createScriptExecutorPool(ClasspathScriptProvider classpathScriptProvider,
	                                                                  int poolSize, boolean sandboxing);

	/**
	 * Updates all {@link GameFuture}s
	 * 
	 * @param delta
	 *            The time (in seconds) since the last frame update
	 */
	public void update(float delta) {
		for (GameFuture gameFuture : runningFutures.values()) {
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
		long startTime = System.currentTimeMillis();
		try {
			ScriptInvocation scriptInvocation = null;
			while ((scriptInvocation = scriptInvocations.poll()) != null) {
				ScriptExecutionTask<?> executionTask = scriptExecutorPool.execute(scriptInvocation.getScriptId(),
						scriptInvocation.getScriptBindings(), scriptInvocation.getInvocationListener());
				Future<?> taskFuture = executorService.submit(executionTask);
				executionTask.setTaskFuture(taskFuture);
				runningScripts.put(executionTask.getTaskId(), executionTask);
				scriptInvocation.release();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		long duration = System.currentTimeMillis() - startTime;
		if (duration >= 16L) {
			executorService.submit(this);
		} else {
			executorService.schedule(this, 16L - duration, TimeUnit.MILLISECONDS);
		}
	}

	private void cleanupCompletedFutures() {
		for (GameFuture gameFuture : runningFutures.values()) {
			if (gameFuture.isReadyForGC()) {
				completedFutures.add(gameFuture.getFutureId());
			}
		}
		for (int futureId : completedFutures) {
			runningFutures.remove(futureId);
		}
		completedFutures.clear();
	}

	private void cleanupCompletedScripts() {
		for (ScriptExecutionTask<?> scriptExecutionTask : runningScripts.values()) {
			if (scriptExecutionTask.isFinished()) {
				scriptExecutionTask.cleanup();
				completedScripts.add(scriptExecutionTask.getTaskId());
			}
		}
		for (int taskId : completedScripts) {
			runningScripts.remove(taskId);
		}
		completedScripts.clear();
	}

	/**
	 * Skips all currently running scripts
	 */
	public void skipAllScripts() {
		for (ScriptExecutionTask<?> scriptExecutionTask : runningScripts.values()) {
			scriptExecutionTask.skipScript();
		}
	}

	/**
	 * Skips a currently running script
	 * 
	 * @param scriptId
	 *            The ID of the script to skip
	 */
	public void skipScript(int scriptId) {
		for(int taskId : runningScripts.keySet()) {
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
	 * Skips all currently running {@link GameFuture}s
	 */
	public void skipAllGameFutures() {
		for (GameFuture gameFuture : runningFutures.values()) {
			gameFuture.skipFuture();
		}
	}

	/**
	 * Compiles a script for execution. Note it is best to call this
	 * sequentially before any script executions to avoid throwing a
	 * {@link InsufficientCompilersException}
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
		return scriptExecutorPool.preCompileScript(scriptContent);
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
	public int compileScript(InputStream inputStream) throws InsufficientCompilersException, IOException {
		Scanner scanner = new Scanner(inputStream);
		scanner.useDelimiter("\\A");
		String contents = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		inputStream.close();
		return compileScript(contents);
	}

	/**
	 * Queues a compiled script for execution in the engine's thread pool
	 * 
	 * @param scriptId
	 *            The id of the script to run
	 * @param scriptBindings
	 *            The variable bindings for the script
	 */
	public void invokeCompiledScript(int scriptId, ScriptBindings scriptBindings) {
		invokeCompiledScript(scriptId, scriptBindings, null);
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
	 */
	public void invokeCompiledScript(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		scriptInvocations.offer(scriptInvocationPool.allocate(scriptId, scriptBindings, invocationListener));
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
		int scriptId = compileScript(scriptContent);
		invokeCompiledScript(scriptId, scriptBindings, invocationListener);
		return scriptId;
	}

	/**
	 * Executes a compiled script immediately on the thread calling this method.
	 * 
	 * Warning: If no {@link ScriptExecutor}s are available this will block
	 * until one is available
	 * 
	 * @param scriptId
	 *            The script id
	 * @param scriptBindings
	 *            The variable bindings for the script
	 */
	public void invokeCompiledScriptLocally(int scriptId, ScriptBindings scriptBindings) {
		invokeCompiledScriptLocally(scriptId, scriptBindings, null);
	}

	/**
	 * Executes a compiled script immediately on the thread calling this method.
	 * 
	 * Warning: If no {@link ScriptExecutor}s are available this will block
	 * until one is available
	 * 
	 * @param scriptId
	 *            The script id
	 * @param scriptBindings
	 *            The variable bindings for the script
	 * @param invocationListener
	 *            A {@link ScriptInvocationListener} to list for invocation
	 *            results
	 */
	public void invokeCompiledScriptLocally(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		ScriptExecutionTask<?> executionTask = scriptExecutorPool.execute(scriptId, scriptBindings, invocationListener);
		runningScripts.put(executionTask.getTaskId(), executionTask);
		executionTask.run();
	}

	void submitGameFuture(GameFuture gameFuture) {
		queuedFutures.offer(gameFuture);
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
}
