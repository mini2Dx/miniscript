package org.mini2Dx.miniscript.gradle.compiler;

import java.io.IOException;

public interface ScriptCompiler {

	public Object compileFile(CompilerConfig compilerConfig) throws IOException;
}
