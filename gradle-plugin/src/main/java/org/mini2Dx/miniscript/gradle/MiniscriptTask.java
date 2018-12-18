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
package org.mini2Dx.miniscript.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class MiniscriptTask extends DefaultTask {
	private final Property<Boolean> prefixWithRoot;
	protected final Property<Boolean> recursive;
	protected final Property<String> outputClass;
	protected final DirectoryProperty scriptsDir;

	protected File outputDir;

	protected String outputPackage;

	public MiniscriptTask() {
		super();
		prefixWithRoot = getProject().getObjects().property(Boolean.class);
		recursive = getProject().getObjects().property(Boolean.class);
		outputClass = getProject().getObjects().property(String.class);
		scriptsDir = getProject().getLayout().directoryProperty();
	}

	@TaskAction
	public void run() throws IOException {
		outputPackage = outputClass.get().substring(0, outputClass.get().lastIndexOf('.'));

		final List<CompilerInputFile> inputFiles = new ArrayList<CompilerInputFile>();
		listScriptFiles(inputFiles, scriptsDir.get().getAsFile());
		process(inputFiles);
	}

	private void listScriptFiles(List<CompilerInputFile> result, File inputDirectory) throws IOException {
		for(File file : inputDirectory.listFiles()) {
			if(file.isDirectory()) {
				if(recursive.get()) {
					listScriptFiles(result, file);
				}
				continue;
			}

			result.add(new CompilerInputFile(scriptsDir.get().getAsFile(), file));
		}
	}

	protected abstract void process(List<CompilerInputFile> inputFiles) throws IOException;

	@InputDirectory
	public DirectoryProperty getScriptsDir() {
		return scriptsDir;
	}

	@Input
	public Property<Boolean> getPrefixWithRoot() {
		return prefixWithRoot;
	}

	@Input
	public Property<Boolean> getRecursive() {
		return recursive;
	}

	@Input
	public Property<String> getOutputClass() {
		return outputClass;
	}

	@OutputDirectory
	public File getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}
}
