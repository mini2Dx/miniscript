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

import org.mini2Dx.miniscript.core.util.ReadWriteArrayQueue;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a pool of reusable {@link ScriptInvocation} instances to reduce
 * object allocation
 */
public class ScriptInvocationPool {
	private final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
	private final Queue<ScriptInvocation> pool = new ReadWriteArrayQueue<>();
	
	/**
	 * Allocate a {@link ScriptInvocation} instance
	 * @param scriptId The script id being invoked
	 * @param scriptBindings The {@link ScriptBindings}
	 * @param invocationListener An optional {@link ScriptInvocationListener}
	 * @param priority The script execution priority
	 * @return A {@link ScriptInvocation} instance set to the provided parameters
	 */
	public ScriptInvocation allocate(int scriptId, ScriptBindings scriptBindings, ScriptInvocationListener invocationListener,
	                                 int priority, boolean interactive) {
		ScriptInvocation result = pool.poll();
		if(result == null) {
			//If pool is empty create a new instance that will eventually be returned to the pool
			result = new ScriptInvocation(this);
		}
		result.setScriptId(scriptId);
		result.setScriptBindings(scriptBindings);
		result.setInvocationListener(invocationListener);
		result.setPriority(priority);
		result.setInteractive(interactive);
		result.setInvokeTimestamp(System.nanoTime());
		result.setTaskId(ID_GENERATOR.incrementAndGet());
		return result;
	}
	
	/**
	 * Releases a {@link ScriptInvocation} back to the pool
	 * @param scriptInvocation The {@link ScriptInvocation} to release
	 */
	public void release(ScriptInvocation scriptInvocation) {
		pool.offer(scriptInvocation);
	}
}
