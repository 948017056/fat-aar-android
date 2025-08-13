package com.kezong.fataar

import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.tasks.ManifestProcessorTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.TaskProvider

import java.lang.reflect.Field

final class VersionAdapter {

    private final Project mProject
    private final LibraryVariant mVariant

    VersionAdapter(final Project project, final LibraryVariant variant) {
        mProject = project
        mVariant = variant
    }

    ConfigurableFileCollection getClassPathDirFiles() {
        ConfigurableFileCollection classpath
        if (FatUtils.compareVersion(AGPVersion, "8.3.0") >= 0) {
            classpath = mProject.files("${mProject.buildDir.path}/intermediates/" +
                    "javac/${mVariant.name}/compile${mVariant.name.capitalize()}JavaWithJavac/classes")
        } else {
            classpath = mProject.files("${mProject.buildDir.path}/intermediates/" +
                    "javac/${mVariant.name}/classes")
        }
        return classpath
    }

    File getLibsDirFile() {
        return mProject.file(
                "${mProject.buildDir.path}/intermediates/aar_libs_directory/${mVariant.name}/sync${mVariant.name.capitalize()}LibJars/libs"
        )
    }

    Task getJavaCompileTask() {
        return mVariant.getJavaCompileProvider().get()
    }

    ManifestProcessorTask getProcessManifest() {
        return mVariant.getOutputs().first().getProcessManifestProvider().get()
    }

    Task getMergeAssets() {
        return mVariant.getMergeAssetsProvider().get()
    }

    String getSyncLibJarsTaskPath() {
        return "sync${mVariant.name.capitalize()}LibJars"
    }

    File getOutputFile() {
        return outputFile(mProject, mVariant, AGPVersion)
    }

    static TaskProvider<Task> getBundleTaskProvider(Project project, String variantName) throws UnknownTaskException {
        def taskPath = "bundle" + variantName.capitalize()
        TaskProvider bundleTask
        try {
            bundleTask = project.tasks.named(taskPath)
        } catch (UnknownTaskException ignored) {
            taskPath += "Aar"
            bundleTask = project.tasks.named(taskPath)
        }
        return bundleTask
    }

    static String getAGPVersion() {
        try {
            Class aClass = Class.forName("com.android.Version")
            Field version = aClass.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            return version.get(aClass)
        } catch (Throwable ignore) {
            Class aClass = Class.forName("com.android.builder.model.Version")
            Field version = aClass.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            return version.get(aClass)
        }
    }
}
