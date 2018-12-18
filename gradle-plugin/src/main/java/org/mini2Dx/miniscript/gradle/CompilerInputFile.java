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

import java.io.File;

public class CompilerInputFile {
	private final File inputScriptFile;
	private final String inputScriptFilename;
	private final String inputScriptFileSuffix;
	private final String inputScriptFilenameWithoutSuffix;
	private final String inputScriptRelativeFilename;
	private final String outputClassName;

	public CompilerInputFile(File scriptsRootDir, File scriptFile) {
		super();
		this.inputScriptFile = scriptFile;
		this.inputScriptFilename = inputScriptFile.getName();
		this.inputScriptFileSuffix = inputScriptFilename.substring(inputScriptFilename.lastIndexOf('.')).toLowerCase();
		this.inputScriptFilenameWithoutSuffix = inputScriptFile.getName().substring( 0, inputScriptFile.getName().lastIndexOf('.'));
		final String inputScriptRelativeFilename = inputScriptFile.getAbsolutePath().replace(scriptsRootDir.getAbsolutePath(), "");

		if(inputScriptRelativeFilename.startsWith("/") || inputScriptRelativeFilename.startsWith("\\")) {
			this.inputScriptRelativeFilename  = inputScriptRelativeFilename.substring(1);
		} else {
			this.inputScriptRelativeFilename = inputScriptRelativeFilename;
		}

		this.outputClassName = inputScriptFilenameWithoutSuffix.replace('-', '_');
	}

	public File getInputScriptFile() {
		return inputScriptFile;
	}

	public String getInputScriptFilename() {
		return inputScriptFilename;
	}

	public String getInputScriptFileSuffix() {
		return inputScriptFileSuffix;
	}

	public String getInputScriptFilenameWithoutSuffix() {
		return inputScriptFilenameWithoutSuffix;
	}

	public String getInputScriptRelativeFilename() {
		return inputScriptRelativeFilename;
	}

	public String getOutputClassName() {
		return outputClassName;
	}

	public String getOutputPackageName(String rootScriptPackage) {
		if(inputScriptRelativeFilename.indexOf('/') > 0 || inputScriptRelativeFilename.indexOf('\\') > 0) {
			return rootScriptPackage + '.' + inputScriptRelativeFilename.
					replace('/', '.').
					replace('\\', '.').
					replace('-', '_')
					.substring(0, inputScriptRelativeFilename.lastIndexOf('.'));
		}
		return rootScriptPackage;
	}
}
