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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mini2Dx.miniscript.core.dummy.DummyGameFuture;

/**
 * Base UAT class for {@link GameScriptingEngine} implementations
 */
public abstract class AbstractGameScriptingEngineTest {
	private ScriptBindings scriptBindings;
	private DummyGameFuture gameFuture;
	
	private GameScriptingEngine scriptingEngine;
	private AtomicBoolean scriptExecuted;
	private AtomicReference<ScriptResult> scriptResult;
	
	@Before
	public void setUp() {
		scriptingEngine = createScriptingEngine();
		
		gameFuture = new DummyGameFuture(scriptingEngine);
		scriptExecuted = new AtomicBoolean(false);
		scriptResult = new AtomicReference<ScriptResult>(ScriptResult.NOT_EXECUTED);
		
		scriptBindings = new ScriptBindings();
		scriptBindings.put("stringValue", "hello");
		scriptBindings.put("booleanValue", false);
		scriptBindings.put("intValue", 7);
		scriptBindings.put("future", gameFuture);
	}
	
	@After
	public void teardown() {
		scriptingEngine.dispose();
	}
	
	@Test
	public void testInvokeScript() throws Exception {
		final int expectedScriptId = scriptingEngine.compileScript(getDefaultScript());
		scriptingEngine.invokeCompiledScript(expectedScriptId, scriptBindings, new ScriptInvocationListener() {
			
			@Override
			public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
				if(scriptId != expectedScriptId) {
					scriptResult.set(ScriptResult.INCORRECT_SCRIPT_ID);
					scriptExecuted.set(true);
				} else if(!checkExpectedScriptResults(executionResult)) {
					scriptResult.set(ScriptResult.INCORRECT_VARIABLES);
					scriptExecuted.set(true);
				} else {
					scriptResult.set(ScriptResult.SUCCESS);
					scriptExecuted.set(true);
				}
			}
			
			@Override
			public void onScriptSkipped(int scriptId) {
				scriptResult.set(ScriptResult.SKIPPED);
				scriptExecuted.set(true);
			}
			
			@Override
			public void onScriptException(int scriptId, Exception e) {
				e.printStackTrace();
				scriptResult.set(ScriptResult.EXCEPTION);
				scriptExecuted.set(true);
			}
		});
		while(!scriptExecuted.get()) {
			scriptingEngine.update(1f);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
		}
		Assert.assertEquals(ScriptResult.SUCCESS, scriptResult.get());
		Assert.assertEquals(true, gameFuture.isUpdated());
		Assert.assertEquals(false, gameFuture.waitOccurred());
		Assert.assertEquals(false, gameFuture.isFutureSkipped());
		Assert.assertEquals(false, gameFuture.isScriptSkipped());
	}
	
	@Test
	public void testWaitForCompletion() throws Exception {
		final int expectedScriptId = scriptingEngine.compileScript(getWaitForCompletionScript());
		scriptingEngine.invokeCompiledScript(expectedScriptId, scriptBindings, new ScriptInvocationListener() {
			
			@Override
			public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
				if(scriptId != expectedScriptId) {
					scriptResult.set(ScriptResult.INCORRECT_SCRIPT_ID);
					scriptExecuted.set(true);
				} else if(!checkExpectedScriptResults(executionResult)) {
					scriptResult.set(ScriptResult.INCORRECT_VARIABLES);
					scriptExecuted.set(true);
				} else {
					scriptResult.set(ScriptResult.SUCCESS);
					scriptExecuted.set(true);
				}
			}
			
			@Override
			public void onScriptSkipped(int scriptId) {
				scriptResult.set(ScriptResult.SKIPPED);
				scriptExecuted.set(true);
			}
			
			@Override
			public void onScriptException(int scriptId, Exception e) {
				e.printStackTrace();
				scriptResult.set(ScriptResult.EXCEPTION);
				scriptExecuted.set(true);
			}
		});
		while(!scriptExecuted.get()) {
			scriptingEngine.update(1f);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
			gameFuture.setFutureCompleted(true);
		}
		Assert.assertEquals(ScriptResult.SUCCESS, scriptResult.get());
		Assert.assertEquals(true, gameFuture.isUpdated());
		Assert.assertEquals(true, gameFuture.waitOccurred());
		Assert.assertEquals(false, gameFuture.isFutureSkipped());
		Assert.assertEquals(false, gameFuture.isScriptSkipped());
	}
	
	@Test
	public void testSkipFuture() throws Exception {
		final int expectedScriptId = scriptingEngine.compileScript(getWaitForCompletionScript());
		scriptingEngine.invokeCompiledScript(expectedScriptId, scriptBindings, new ScriptInvocationListener() {
			
			@Override
			public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
				scriptResult.set(ScriptResult.SUCCESS);
				scriptExecuted.set(true);
			}
			
			@Override
			public void onScriptSkipped(int scriptId) {
				scriptResult.set(ScriptResult.SKIPPED);
				scriptExecuted.set(true);
			}
			
			@Override
			public void onScriptException(int scriptId, Exception e) {
				e.printStackTrace();
				scriptResult.set(ScriptResult.EXCEPTION);
				scriptExecuted.set(true);
			}
		});
		while(!scriptExecuted.get()) {
			scriptingEngine.update(1f);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
			scriptingEngine.skipAllGameFutures();
		}
		Assert.assertEquals(ScriptResult.SUCCESS, scriptResult.get());
		Assert.assertEquals(true, gameFuture.isUpdated());
		Assert.assertEquals(true, gameFuture.waitOccurred());
		Assert.assertEquals(true, gameFuture.isFutureSkipped());
		Assert.assertEquals(false, gameFuture.isScriptSkipped());
	}
	
	@Test
	public void testSkipScript() throws Exception {
		final int expectedScriptId = scriptingEngine.compileScript(getWaitForCompletionScript());
		scriptingEngine.invokeCompiledScript(expectedScriptId, scriptBindings, new ScriptInvocationListener() {
			
			@Override
			public void onScriptSuccess(int scriptId, ScriptExecutionResult executionResult) {
				scriptResult.set(ScriptResult.SUCCESS);
				scriptExecuted.set(true);
			}
			
			@Override
			public void onScriptSkipped(int scriptId) {
				scriptResult.set(ScriptResult.SKIPPED);
				scriptExecuted.set(true);
			}
			
			@Override
			public void onScriptException(int scriptId, Exception e) {
				e.printStackTrace();
				scriptResult.set(ScriptResult.EXCEPTION);
				scriptExecuted.set(true);
			}
		});
		while(!scriptExecuted.get()) {
			scriptingEngine.update(1f);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
			scriptingEngine.skipScript(expectedScriptId);
		}
		Assert.assertEquals(ScriptResult.SKIPPED, scriptResult.get());
		Assert.assertEquals(true, gameFuture.isUpdated());
		Assert.assertEquals(true, gameFuture.waitOccurred());
		Assert.assertEquals(false, gameFuture.isFutureSkipped());
		Assert.assertEquals(true, gameFuture.isScriptSkipped());
	}
	
	protected abstract GameScriptingEngine createScriptingEngine();
	
	protected abstract String getDefaultScript();
	
	protected abstract String getWaitForCompletionScript();
	
	private boolean checkExpectedScriptResults(ScriptExecutionResult executionResult) {
		if(!executionResult.containsKey("stringValue")) {
			return false;
		}
		if(!executionResult.containsKey("booleanValue")) {
			return false;
		}
		if(!executionResult.containsKey("intValue")) {
			return false;
		}
		if(!"hello123".equals(executionResult.get("stringValue"))) {
			return false;
		}
		if(!((Boolean) executionResult.get("booleanValue"))) {
			return false;
		}
		if(((Integer) executionResult.get("intValue")) != 101) {
			return false;
		}
		return true;
	}
	
	private enum ScriptResult {
		NOT_EXECUTED,
		INCORRECT_SCRIPT_ID,
		INCORRECT_VARIABLES,
		SUCCESS,
		SKIPPED,
		EXCEPTION
	}
}
