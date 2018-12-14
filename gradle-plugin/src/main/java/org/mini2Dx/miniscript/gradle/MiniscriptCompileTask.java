package org.mini2Dx.miniscript.gradle;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.LuaJC;
import org.mini2Dx.miniscript.core.ClasspathScriptProvider;
import org.mini2Dx.miniscript.core.GeneratedClasspathScriptProvider;
import org.mini2Dx.miniscript.gradle.compiler.CompilerConfig;
import org.mini2Dx.miniscript.gradle.compiler.LuaScriptCompiler;
import org.mini2Dx.miniscript.gradle.compiler.ScriptCompiler;

import java.io.*;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class MiniscriptCompileTask extends DefaultTask {
	private final Property<Boolean> recursive;
	private final Property<String> outputClass;

	private final Map<String, ScriptCompiler> compilers = new HashMap<String, ScriptCompiler>();

	private File scriptsDir;
	private File outputDir;

	public MiniscriptCompileTask() {
		super();
		recursive = getProject().getObjects().property(Boolean.class);
		outputClass = getProject().getObjects().property(String.class);

		compilers.put(".lua", new LuaScriptCompiler());
	}

	@TaskAction
	public void compile() throws IOException {
		final String outputPackage = outputClass.get().substring(0, outputClass.get().lastIndexOf('.'));
		final CompilerConfig compilerConfig = new CompilerConfig(outputPackage, outputDir);
		final Map<String, Object> outputClasses = compileDirectory(compilerConfig, scriptsDir);
		generateClassImplementation(outputClasses);
	}

	private void generateClassImplementation(Map<String, Object> outputClasses) throws IOException {
		final DynamicType.Builder classBuilder = new ByteBuddy()
				.subclass(GeneratedClasspathScriptProvider.class)
				.name(outputClass.get());

		classBuilder.method(isDeclaredBy(GeneratedClasspathScriptProvider.class).
				and(ElementMatchers.named("getGeneratedScripts"))).
				intercept(FixedValue.value(outputClasses));
		final DynamicType.Unloaded<?> result = classBuilder.make();
		result.saveIn(outputDir);
	}

	private Map<String, Object> compileDirectory(CompilerConfig compilerConfig, File scriptsDirectory) throws IOException {
		final Map<String, Object> outputClasses = new HashMap<String, Object> ();
		if(!compilerConfig.getOutputDirectory().exists()) {
			compilerConfig.getOutputDirectory().mkdirs();
		}
		for(File file : scriptsDirectory.listFiles()) {
			if(file.isDirectory()) {
				if(recursive.get()) {
					final CompilerConfig nestedConfig = new CompilerConfig(
							compilerConfig.getOutputPackage() + "." + file.getName(), outputDir);
					outputClasses.putAll(compileDirectory(nestedConfig, new File(scriptsDirectory, file.getName())));
				}
				continue;
			}

			compilerConfig.setInputScriptFile(file);
			outputClasses.put(compilerConfig.getInputScriptFilename(), compileFile(compilerConfig));
		}
		return outputClasses;
	}

	private Object compileFile(CompilerConfig compilerConfig) throws IOException {
		final ScriptCompiler compiler = compilers.get(compilerConfig.getInputScriptFileSuffix());
		if(compiler == null) {
			throw new GradleException("miniscript does not support build time compilation of "
					+ compilerConfig.getInputScriptFileSuffix() + " files.");
		}
		return compiler.compileFile(compilerConfig);
	}

	@InputDirectory
	public File getScriptsDir() {
		return scriptsDir;
	}

	public void setScriptsDir(File scriptsDir) {
		this.scriptsDir = scriptsDir;
	}

	@OutputDirectory
	public File getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@Input
	public Property<Boolean> getRecursive() {
		return recursive;
	}

	@Input
	public Property<String> getOutputClass() {
		return outputClass;
	}
}
