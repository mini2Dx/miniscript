package org.mini2Dx.miniscript.core;

import java.util.HashMap;
import java.util.Map;

public abstract class GeneratedClasspathScriptProvider implements ClasspathScriptProvider {
	private final Map<String, Integer> scriptToIds = new HashMap<String, Integer>();
	private final Map<Integer, Object> idsToScripts = new HashMap<Integer, Object>();

	public GeneratedClasspathScriptProvider() {
		super();

		final Map<String, Object> generatedScripts = getGeneratedScripts();

		int count = 0;
		for(String filepath : generatedScripts.keySet()) {
			scriptToIds.put(filepath, count);
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
		return scriptToIds.get(filepath);
	}

	@Override
	public int getTotalScripts() {
		return scriptToIds.size();
	}

	public abstract Map<String, Object> getGeneratedScripts();
}
