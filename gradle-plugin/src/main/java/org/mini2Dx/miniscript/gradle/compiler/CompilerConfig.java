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
package org.mini2Dx.miniscript.gradle.compiler;

import org.mini2Dx.miniscript.gradle.CompilerInputFile;

import java.io.File;

public class CompilerConfig {
	private final File rootOutputDir;
	private final String outputRootPackage;
	private final String outputRootPackageAsPath;
	private final File outputRootDirectory;

	private CompilerInputFile inputScriptFile;

	private String outputPackage, outputPackageAsPath;
	private File packageDir;

	public CompilerConfig(String outputRootPackage, File rootOutputDir) {
		super();
		this.rootOutputDir = rootOutputDir;
		this.outputRootPackage = outputRootPackage;
		this.outputRootPackageAsPath = outputRootPackage.replace('.', '/') + "/";
		this.outputRootDirectory = new File(rootOutputDir, outputRootPackageAsPath.replace('/', File.separatorChar));
	}

	public void setInputScriptFile(CompilerInputFile inputScriptFile) {
		this.inputScriptFile  = inputScriptFile;

		this.outputPackage = inputScriptFile.getOutputPackageName(outputRootPackage);
		this.outputPackageAsPath = outputPackage.replace('.', '/') + "/";

		packageDir = new File(rootOutputDir, outputPackage.replace('.', File.separatorChar));
		if(!packageDir.exists()) {
			packageDir.mkdirs();
		}
	}

	public String getOutputPackage() {
		return outputPackage;
	}

	public String getOutputPackageAsPath() {
		return outputPackageAsPath;
	}

	public CompilerInputFile getInputScriptFile() {
		return inputScriptFile;
	}

	public File getOutputClassFile(String qualifiedClassName) {
		final String className;
		if(qualifiedClassName.contains("/")) {
			className = qualifiedClassName.substring(qualifiedClassName.lastIndexOf('/') + 1).replace('-', '_');
		} else {
			className = qualifiedClassName.replace('-', '_');
		}
		return new File(packageDir,  className + ".class");
	}

	public String getOutputRootPackage() {
		return outputRootPackage;
	}

	public String getOutputRootPackageAsPath() {
		return outputRootPackageAsPath;
	}

	public File getOutputRootDirectory() {
		return outputRootDirectory;
	}
}
