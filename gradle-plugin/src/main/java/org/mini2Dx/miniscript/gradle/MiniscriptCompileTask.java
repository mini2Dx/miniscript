package org.mini2Dx.miniscript.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.luaj.vm2.luajc.LuaJC;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;

public class MiniscriptCompileTask extends DefaultTask {
	private final Property<Boolean> recursive;
	private final Property<String> outputClass;

	private File scriptsDir;
	private File outputDir;

	public MiniscriptCompileTask() {
		super();
		recursive = getProject().getObjects().property(Boolean.class);
		outputClass = getProject().getObjects().property(String.class);
	}

	@TaskAction
	public void compile() {

	}

	private void compileFile(File inputFile, File outputDir) throws IOException {
		// create the chunk
		FileInputStream fis = new FileInputStream( inputFile );
		final Hashtable t = LuaJC.instance.compileAll( fis, inf.luachunkname, inf.srcfilename, globals, genmain);
		fis.close();

		// write out the chunk
		for (Enumeration e = t.keys(); e.hasMoreElements(); ) {
			String key = (String) e.nextElement();
			byte[] bytes = (byte[]) t.get(key);
			if ( key.indexOf('/')>=0 ) {
				String d = (destdir!=null? destdir+"/": "")+key.substring(0,key.lastIndexOf('/'));
				new File(d).mkdirs();
			}
			String destpath = (destdir!=null? destdir+"/": "") + key + ".class";
			FileOutputStream fos = new FileOutputStream( destpath );
			fos.write( bytes );
			fos.close();
		}
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
