/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Thomas Cashman
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

import org.mini2Dx.miniscript.core.util.ReadWriteMap;

import java.util.Map;

public class PerThreadClasspathGameScript<S> extends GameScript<S> {
	private final Map<Long, S> threadToScriptMapping = new ReadWriteMap<>();
	private final S content;

	public PerThreadClasspathGameScript(S content) {
		super();
		this.content = content;
	}

	@Override
	public S getScript() {
		return threadToScriptMapping.get(Thread.currentThread().getId());
	}

	@Override
	public boolean hasScript() {
		return threadToScriptMapping.containsKey(Thread.currentThread().getId());
	}

	@Override
	public void setScript(S script) {
		threadToScriptMapping.put(Thread.currentThread().getId(), script);
	}

	public S compileInstance() {
		try {
			return (S) content.getClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return content;
	}
}
