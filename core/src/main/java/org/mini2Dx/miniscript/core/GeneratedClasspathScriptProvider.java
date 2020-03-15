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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class GeneratedClasspathScriptProvider implements ClasspathScriptProvider {
	private final Map<String, Integer> scriptFilepathsToIds = new HashMap<String, Integer>();
	private final Map<Integer, String> scriptIdsToFilepaths = new HashMap<Integer, String>();
	private final Map<Integer, Object> idsToScripts = new HashMap<Integer, Object>();

	public GeneratedClasspathScriptProvider() {
		super();

		final Map<String, Object> generatedScripts = getGeneratedScripts();

		int count = 0;
		for(String filepath : generatedScripts.keySet()) {
			scriptFilepathsToIds.put(filepath, count);
			scriptIdsToFilepaths.put(count, filepath);
			idsToScripts.put(count, generatedScripts.get(filepath));
			count++;
		}
	}

	@Override
	public <T> T getClasspathScript(int scriptId) {
		return (T) idsToScripts.get(scriptId);
	}

	@Override
	public int getScriptId(String filepath) {
		return scriptFilepathsToIds.get(filepath);
	}

	@Override
	public int getTotalScripts() {
		return scriptFilepathsToIds.size();
	}

	@Override
	public Set<String> getFilepaths() {
		return scriptFilepathsToIds.keySet();
	}

	@Override
	public String getFilepath(int scriptId) {
		return scriptIdsToFilepaths.get(scriptId);
	}

	public abstract Map getGeneratedScripts();
}
