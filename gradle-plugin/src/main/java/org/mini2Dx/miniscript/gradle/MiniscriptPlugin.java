package org.mini2Dx.miniscript.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class MiniscriptPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPlugins().apply(JavaPlugin.class);
		final JavaPluginConvention javaConvention =  project.getConvention().getPlugin(JavaPluginConvention.class);
		final SourceSet mainSourceSet = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

		final MiniscriptExtension extension = project.getExtensions().create("miniscript", MiniscriptExtension.class, project);

		project.getTasks().create("compileScripts",
				MiniscriptCompileTask.class,
				new Action<MiniscriptCompileTask>() {

			@Override
			public void execute(MiniscriptCompileTask miniscriptCompileTask) {
				miniscriptCompileTask.getRecursive().set(extension.getRecursive());
				miniscriptCompileTask.setScriptsDir(extension.getScriptsDir());
				miniscriptCompileTask.getOutputClass().set(extension.getOutputClass());
				miniscriptCompileTask.setOutputDir(mainSourceSet.getJava().getOutputDir());
			}
		});
	}
}
