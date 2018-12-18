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

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

public class MiniscriptExtension {
	private final Property<Boolean> prefixWithRoot;
	private final Property<Boolean> recursive;
	private final Property<String> outputClass;
	private final DirectoryProperty scriptsDir;

	public MiniscriptExtension(Project project) {
		super();
		prefixWithRoot = project.getObjects().property(Boolean.class);
		recursive = project.getObjects().property(Boolean.class);
		outputClass = project.getObjects().property(String.class);
		scriptsDir = project.getLayout().directoryProperty();
	}

	public Property<Boolean> getPrefixWithRoot() {
		return prefixWithRoot;
	}

	public DirectoryProperty getScriptsDir() {
		return scriptsDir;
	}

	public Property<Boolean> getRecursive() {
		return recursive;
	}

	public Property<String> getOutputClass() {
		return outputClass;
	}
}
