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
package org.mini2Dx.miniscript.ruby;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.mini2Dx.miniscript.core.*;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;
import org.mini2Dx.miniscript.core.exception.NoSuchScriptException;
import org.mini2Dx.miniscript.core.exception.ScriptExecutorUnavailableException;

/**
 * An implementation of {@link ScriptExecutorPool} for Ruby-based scripts
 */
public class RubyScriptExecutorPool implements ScriptExecutorPool<EmbedEvalUnit> {
	private final Map<Long, ScriptingContainer> threadCompilers = new ConcurrentHashMap<Long, ScriptingContainer>();
	private final Map<Integer, PerThreadGameScript<EmbedEvalUnit>> scripts = new ConcurrentHashMap<Integer, PerThreadGameScript<EmbedEvalUnit>>();
	private final Map<String, Integer> filepathToScriptId = new ConcurrentHashMap<String, Integer>();
	private final BlockingQueue<ScriptExecutor<EmbedEvalUnit>> executors;
	private final GameScriptingEngine gameScriptingEngine;
	private final SynchronizedObjectPool<RubyEmbeddedScriptInvoker> embeddedScriptInvokerPool = new SynchronizedObjectPool<RubyEmbeddedScriptInvoker>() {
		@Override
		protected RubyEmbeddedScriptInvoker construct() {
			return new RubyEmbeddedScriptInvoker(gameScriptingEngine, RubyScriptExecutorPool.this);
		}
	};

	public RubyScriptExecutorPool(GameScriptingEngine gameScriptingEngine, int poolSize) {
		this.gameScriptingEngine = gameScriptingEngine;
		executors = new ArrayBlockingQueue<ScriptExecutor<EmbedEvalUnit>>(poolSize);

		for (int i = 0; i < poolSize; i++) {
			executors.offer(new RubyScriptExecutor(this));
		}
	}

	@Override
	public int getCompiledScriptId(String filepath) {
		return filepathToScriptId.getOrDefault(filepath, -1);
	}

	@Override
	public int preCompileScript(String filepath, String scriptContent) throws InsufficientCompilersException {
		PerThreadGameScript<EmbedEvalUnit> script = new PerThreadGameScript<EmbedEvalUnit>(scriptContent);
		scripts.put(script.getId(), script);
		filepathToScriptId.put(filepath, script.getId());
		return script.getId();
	}

	@Override
	public ScriptExecutionTask<?> execute(int scriptId, ScriptBindings scriptBindings,
			ScriptInvocationListener invocationListener) {
		ScriptExecutor<EmbedEvalUnit> executor = allocateExecutor();
		if (executor == null) {
			throw new ScriptExecutorUnavailableException(scriptId);
		}
		if(!scripts.containsKey(scriptId)) {
			executor.release();
			throw new NoSuchScriptException(scriptId);
		}
		return new ScriptExecutionTask<EmbedEvalUnit>(gameScriptingEngine, executor, scriptId, scripts.get(scriptId),
				scriptBindings, invocationListener);
	}

	@Override
	public void release(ScriptExecutor<EmbedEvalUnit> executor) {
		try {
			executors.put(executor);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private ScriptExecutor<EmbedEvalUnit> allocateExecutor() {
		try {
			return executors.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ScriptingContainer getLocalScriptingContainer() {
		long threadId = Thread.currentThread().getId();
		if (!threadCompilers.containsKey(threadId)) {
			ScriptingContainer scriptingContainer = new ScriptingContainer(LocalContextScope.SINGLETHREAD,
					LocalVariableBehavior.PERSISTENT);
			scriptingContainer.setCompileMode(CompileMode.JIT);
			threadCompilers.put(threadId, scriptingContainer);
		}
		return threadCompilers.get(threadId);
	}

	@Override
	public GameScriptingEngine getGameScriptingEngine() {
		return gameScriptingEngine;
	}

	GameScript<EmbedEvalUnit> getScript(int scriptId) {
		return scripts.get(scriptId);
	}

	public SynchronizedObjectPool<RubyEmbeddedScriptInvoker> getEmbeddedScriptInvokerPool() {
		return embeddedScriptInvokerPool;
	}
}
