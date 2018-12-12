package org.mini2Dx.miniscript.gradle;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import java.io.File;

public class MiniscriptExtension {
	private final Property<Boolean> recursive;
	private final Property<String> outputClass;

	private File scriptsDir;

	public MiniscriptExtension(Project project) {
		super();
		recursive = project.getObjects().property(Boolean.class);
		outputClass = project.getObjects().property(String.class);
	}

	public File getScriptsDir() {
		return scriptsDir;
	}

	public void setScriptsDir(File scriptsDir) {
		this.scriptsDir = scriptsDir;
	}

	public Property<Boolean> getRecursive() {
		return recursive;
	}

	public Property<String> getOutputClass() {
		return outputClass;
	}
}
