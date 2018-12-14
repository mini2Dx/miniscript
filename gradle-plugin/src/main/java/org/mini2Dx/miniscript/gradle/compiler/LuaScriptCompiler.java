package org.mini2Dx.miniscript.gradle.compiler;

import org.gradle.api.GradleException;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.LuaJC;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public class LuaScriptCompiler implements ScriptCompiler {

	@Override
	public Object compileFile(CompilerConfig compilerConfig) throws IOException {
		final String luaChunkName = compilerConfig.getOutputPackageAsPath() + compilerConfig.getInputScriptFilenameWithoutSuffix();
		FileInputStream fileInputStream = new FileInputStream( compilerConfig.getInputScriptFile() );
		final Hashtable luaHashtable = LuaJC.instance.compileAll( fileInputStream, luaChunkName,
				compilerConfig.getOutputPackageAsPath() + compilerConfig.getInputScriptFilename(),
				JsePlatform.standardGlobals(), false);
		fileInputStream.close();

		for (Enumeration enumeration = luaHashtable.keys(); enumeration.hasMoreElements(); ) {
			final String className = (String) enumeration.nextElement();
			final byte[] bytes = (byte[]) luaHashtable.get(className);
			final FileOutputStream outputStream = new FileOutputStream(compilerConfig.getOutputClassFile());
			outputStream.write( bytes );
			outputStream.close();
		}

		final LuaClassLoader luaClassLoader = new LuaClassLoader(luaHashtable);
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

		private LuaClassLoader(Hashtable luaHashtable) {
			this.luaHashtable = luaHashtable;
		}

		public Class findClass(String classname) throws ClassNotFoundException {
			byte[] bytes = (byte[]) luaHashtable.get(classname);
			if ( bytes != null )
				return defineClass(classname, bytes, 0, bytes.length);
			return super.findClass(classname);
		}
	}
}
