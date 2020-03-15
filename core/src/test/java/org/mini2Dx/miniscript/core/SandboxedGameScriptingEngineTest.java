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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mini2Dx.miniscript.core.dummy.DummyGameFuture;
import org.mini2Dx.miniscript.core.dummy.DummyJavaObject;
import org.mini2Dx.miniscript.core.dummy.ScriptResult;
import org.mini2Dx.miniscript.core.exception.InsufficientCompilersException;

/**
 * Base UAT class for {@link GameScriptingEngine} implementations that support sandboxing
 */
public abstract class SandboxedGameScriptingEngineTest {
	protected ScriptBindings scriptBindings;
	protected DummyGameFuture gameFuture;
	protected DummyJavaObject javaObject;
	
	protected GameScriptingEngine scriptingEngine;
	protected AtomicBoolean scriptExecuted;
	protected AtomicReference<ScriptResult> scriptResult;
	
	@Before
	public void setUp() {
		scriptingEngine = createScriptingEngineWithSandboxing();
		
		javaObject = new DummyJavaObject();
		gameFuture = new DummyGameFuture(scriptingEngine);
		scriptExecuted = new AtomicBoolean(false);
		scriptResult = new AtomicReference<ScriptResult>(ScriptResult.NOT_EXECUTED);
		
		scriptBindings = new ScriptBindings();
		scriptBindings.put("stringValue", "hello");
		scriptBindings.put("booleanValue", false);
		scriptBindings.put("intValue", 7);
		scriptBindings.put("future", gameFuture);
		scriptBindings.put("jObject", javaObject);
	}
	
	@After
	public void teardown() {
		scriptingEngine.dispose();
	}
	
	@Test
	public void testSandboxScript() throws InsufficientCompilersException, IOException {
		final int expectedScriptId = scriptingEngine.compileScript(getSandboxScriptFilepath(), getSandboxScript());
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

			@Override
			public boolean callOnGameThread() {
				return true;
			}
		});
		while(!scriptExecuted.get()) {
			scriptingEngine.update(1f);
		}
		Assert.assertEquals(ScriptResult.SUCCESS, scriptResult.get());
		Assert.assertEquals(true, gameFuture.isUpdated());
		Assert.assertEquals(false, gameFuture.waitOccurred());
		Assert.assertEquals(false, gameFuture.isFutureSkipped());
		Assert.assertEquals(false, gameFuture.isScriptSkipped());
		Assert.assertEquals(78, javaObject.getValue());
	}
	
	@Test
	public void testSandboxScriptWithIllegalLines() throws InsufficientCompilersException, IOException {
		final int expectedScriptId = scriptingEngine.compileScript(getSandboxScriptWithIllegalLinesFilepath(), getSandboxScriptWithIllegalLines());
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

			@Override
			public boolean callOnGameThread() {
				return true;
			}
		});
		while(!scriptExecuted.get()) {
			scriptingEngine.update(1f);
		}
		Assert.assertEquals(ScriptResult.EXCEPTION, scriptResult.get());
		Assert.assertEquals(true, gameFuture.isUpdated());
		Assert.assertEquals(false, gameFuture.waitOccurred());
		Assert.assertEquals(false, gameFuture.isFutureSkipped());
		Assert.assertEquals(false, gameFuture.isScriptSkipped());
		Assert.assertEquals(0, javaObject.getValue());
	}
	
	protected abstract GameScriptingEngine createScriptingEngineWithSandboxing();

	protected abstract String getSandboxScriptWithIllegalLinesFilepath();

	protected abstract InputStream getSandboxScriptWithIllegalLines();

	protected abstract String getSandboxScriptFilepath();

	protected abstract InputStream getSandboxScript();
	
	protected boolean checkExpectedScriptResults(ScriptExecutionResult executionResult) {
		if(!executionResult.containsKey("stringValue")) {
			System.err.println("stringValue not present");
			return false;
		}
		if(!executionResult.containsKey("booleanValue")) {
			System.err.println("booleanValue not present");
			return false;
		}
		if(!executionResult.containsKey("intValue")) {
			System.err.println("intValue not present");
			return false;
		}
		if(!"hello123".equals(executionResult.get("stringValue"))) {
			System.err.println("Expected stringValue to be hello123 but was " + executionResult.get("stringValue"));
			return false;
		}
		if(!((Boolean) executionResult.get("booleanValue"))) {
			System.err.println("Expected booleanValue to be true but was " + executionResult.get("stringValue"));
			return false;
		}
		if(((Integer) executionResult.get("intValue")) != 101) {
			System.err.println("Expected intValue to be 101 but was " + executionResult.get("stringValue"));
			return false;
		}
		return true;
	}
}
