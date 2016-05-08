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

/**
 * Provides scripting functionality to your game
 * 
 * Note that this is a base class for each scripting language implementation.
 */
public abstract class GameScriptingEngine implements Runnable {
	private final ScriptInvocationPool scriptInvocationPool = new ScriptInvocationPool();
	private final Queue<ScriptInvocation> scriptInvocations = new ConcurrentLinkedQueue<ScriptInvocation>();

	private final Map<Integer, GameFuture> runningFutures = new ConcurrentHashMap<Integer, GameFuture>();
	private final Map<Integer, ScriptExecutionTask<?>> runningScripts = new ConcurrentHashMap<Integer, ScriptExecutionTask<?>>();
	private final Set<Integer> completedFutures = new HashSet<Integer>();
	private final Set<Integer> completedScripts = new HashSet<Integer>();

	private final ScheduledExecutorService executorService;
	private final ScriptExecutorPool<?> scriptExecutorPool;

	private boolean cancelReallocatedFutures = true;

	/**
	 * Constructs a scripting engine backed by a thread pool with the maximum
	 * amount of concurrent scripts set to the amount of processors + 1;
	 */
	public GameScriptingEngine() {
		this(Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Constructs a scripting engine backed by a thread pool.
	 * 
	 * @param maxConcurrentScripts
	 *            The maximum amount of concurrently running scripts. Note this
	 *            is a 'requested' amount and may be less due to the amount of
	 *            available processors on the player's machine.
	 */
	public GameScriptingEngine(int maxConcurrentScripts) {
		scriptExecutorPool = createScriptExecutorPool(maxConcurrentScripts);

		executorService = Executors.newScheduledThreadPool(
				Math.min(maxConcurrentScripts + 1, Runtime.getRuntime().availableProcessors() * 2));
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
		}, 1L, 1L, TimeUnit.SECONDS);
	}

	/**
	 * Shuts down the thread pool and cleans up resources
	 */
	public void dispose() {
		executorService.shutdown();
	}

	protected abstract ScriptExecutorPool<?> createScriptExecutorPool(int poolSize);

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
	}

	/**
	 * This should not be invoked by the developer. Call {@link #update(float)}
	 * instead.
	 */
	@Override
	public void run() {
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
		executorService.submit(this);
	}

	private void cleanupCompletedFutures() {
		for (GameFuture gameFuture : runningFutures.values()) {
			if (gameFuture.isCompleted()) {
				completedFutures.add(gameFuture.getFutureId());
			} else if (gameFuture.isFutureSkipped()) {
				completedFutures.add(gameFuture.getFutureId());
			} else if (gameFuture.isScriptSkipped()) {
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
		ScriptExecutionTask<?> scriptExecutionTask = runningScripts.get(scriptId);
		if (scriptExecutionTask == null) {
			return;
		}
		scriptExecutionTask.skipScript();
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
	 * @throws InsufficientExecutorsException
	 *             Thrown if there are no script executors available to the
	 *             thread pool
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
		GameFuture previousFuture = runningFutures.put(gameFuture.getFutureId(), gameFuture);
		if (previousFuture == null) {
			return;
		}
		if (!cancelReallocatedFutures) {
			return;
		}
		try {
			previousFuture.skipFuture();
		} catch (Exception e) {
			e.printStackTrace();
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
}
