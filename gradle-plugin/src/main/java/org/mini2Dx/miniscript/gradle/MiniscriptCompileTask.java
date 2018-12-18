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

import org.gradle.api.GradleException;
import org.mini2Dx.miniscript.gradle.compiler.CompilerConfig;
import org.mini2Dx.miniscript.gradle.compiler.LuaScriptCompiler;
import org.mini2Dx.miniscript.gradle.compiler.ScriptCompiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniscriptCompileTask extends MiniscriptTask {
	private final Map<String, ScriptCompiler> compilers = new HashMap<String, ScriptCompiler>();

	public MiniscriptCompileTask() {
		super();
		compilers.put(".lua", new LuaScriptCompiler(getProject()));
	}

	@Override
	protected void process(List<CompilerInputFile> inputFiles) throws IOException {
		final CompilerConfig compilerConfig = new CompilerConfig(outputPackage, outputDir);
		compileDirectory(compilerConfig, inputFiles);
	}

	private Map<String, Object> compileDirectory(CompilerConfig compilerConfig, List<CompilerInputFile> inputFiles) throws IOException {
		final Map<String, Object> outputClasses = new HashMap<String, Object> ();
		for(CompilerInputFile inputFile : inputFiles) {
			compilerConfig.setInputScriptFile(inputFile);
			outputClasses.put(inputFile.getInputScriptRelativeFilename(), compileFile(compilerConfig));
		}
		return outputClasses;
	}

	private Object compileFile(CompilerConfig compilerConfig) throws IOException {
		final ScriptCompiler compiler = compilers.get(compilerConfig.getInputScriptFile().getInputScriptFileSuffix());
		if(compiler == null) {
			throw new GradleException("miniscript does not support build time compilation of "
					+ compilerConfig.getInputScriptFile().getInputScriptFileSuffix() + " files.");
		}
		return compiler.compileFile(compilerConfig);
	}
}
