package org.mini2Dx.miniscript.gradle.compiler;

import java.io.File;

public class CompilerConfig {
	private final String outputPackage;
	private final String outputPackageAsPath;
	private final File outputDirectory;

	private File inputScriptFile;
	private String inputScriptFilename;
	private String inputScriptFileSuffix;
	private String inputScriptFilenameWithoutSuffix;

	private String outputClass;
	private File outputClassFile;

	public CompilerConfig(String outputPackage, File rootOutputDir) {
		super();
		this.outputPackage = outputPackage;
		this.outputPackageAsPath = outputPackage.replace('.', '/') + "/";
		this.outputDirectory = new File(rootOutputDir, outputPackageAsPath);
	}

	public void setInputScriptFile(File inputScriptFile) {
		this.inputScriptFile = inputScriptFile;
		this.inputScriptFilename = inputScriptFile.getName();
		this.inputScriptFileSuffix = inputScriptFilename.substring(inputScriptFilename.lastIndexOf('.')).toLowerCase();
		this.inputScriptFilenameWithoutSuffix = inputScriptFile.getName().substring( 0, inputScriptFile.getName().lastIndexOf('.'));

		this.outputClass = outputPackage + "." + inputScriptFilenameWithoutSuffix + ".class";
		this.outputClassFile = new File(outputDirectory, inputScriptFilenameWithoutSuffix + ".class");
	}

	public String getOutputPackage() {
		return outputPackage;
	}

	public String getOutputPackageAsPath() {
		return outputPackageAsPath;
	}

	public File getOutputDirectory() {
		return outputDirectory;
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

	public String getOutputClass() {
		return outputClass;
	}

	public File getOutputClassFile() {
		return outputClassFile;
	}
}
