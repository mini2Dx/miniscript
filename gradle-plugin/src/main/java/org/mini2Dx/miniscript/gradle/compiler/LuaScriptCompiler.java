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

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.mini2Dx.miniscript.gradle.CompilerInputFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public class LuaScriptCompiler implements ScriptCompiler {
	private final Project project;

	public LuaScriptCompiler(Project project) {
		super();
		this.project = project;
	}

	@Override
	public Object compileFile(CompilerConfig compilerConfig) throws IOException {
		final CompilerInputFile inputFile = compilerConfig.getInputScriptFile();
		final String luaChunkName = compilerConfig.getOutputPackageAsPath() + inputFile.getOutputClassName();
		FileInputStream fileInputStream = new FileInputStream( inputFile.getInputScriptFile() );
		final Hashtable luaHashtable = PatchedLuaJC.instance.compileAll( fileInputStream, luaChunkName,
				compilerConfig.getOutputPackageAsPath() + inputFile.getOutputClassName(),
				JsePlatform.standardGlobals(), false);
		fileInputStream.close();

		for (Enumeration enumeration = luaHashtable.keys(); enumeration.hasMoreElements(); ) {
			final String className = (String) enumeration.nextElement();
			final byte[] bytes = (byte[]) luaHashtable.get(className);
			final FileOutputStream outputStream = new FileOutputStream(compilerConfig.getOutputClassFile());
			outputStream.write( bytes );
			outputStream.close();
		}

		final LuaClassLoader luaClassLoader = new LuaClassLoader(luaHashtable, project);
		for ( Enumeration enumeration = luaHashtable.keys(); enumeration.hasMoreElements(); ) {
			final String className = (String) enumeration.nextElement();
			try {
				final Class clazz = luaClassLoader.loadClass(className);
				return clazz.newInstance();
			} catch ( Exception ex ) {
				throw new GradleException("Failed to load " + className);
			}
		}
		return null;
	}

	private static final class LuaClassLoader extends ClassLoader {
		private final Hashtable luaHashtable;
		private final Project project;

		private LuaClassLoader(Hashtable luaHashtable, Project project) {
			this.luaHashtable = luaHashtable;
			this.project = project;
		}

		public Class findClass(String classname) throws ClassNotFoundException {
			byte[] bytes = (byte[]) luaHashtable.get(classname);
			if ( bytes != null )
				return defineClass(classname.replace('/', '.'), bytes, 0, bytes.length);
			return project.getBuildscript().getClassLoader().loadClass(classname);
		}
	}
}
