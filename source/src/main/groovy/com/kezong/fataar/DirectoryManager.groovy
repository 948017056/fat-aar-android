package com.kezong.fataar;

import com.android.build.gradle.api.LibraryVariant;

import org.gradle.api.Project;

final class DirectoryManager {

    private static final String RE_BUNDLE_FOLDER = "aar_rebundle";

    private static final String INTERMEDIATES_TEMP_FOLDER = "fat-aar";

    private final Project project
    private final LibraryVariant variant

    DirectoryManager(Project project, LibraryVariant variant) {
        this.project = project
        this.variant = variant
    }

    File getReBundleDirectory() {
        return this.project.getLayout().getBuildDirectory()
                .dir("outputs/${RE_BUNDLE_FOLDER}/${this.variant.name}")
                .get().asFile
    }

    File getMergeClassDirectory() {
        return this.project.getLayout().getBuildDirectory()
                .dir("intermediates/${INTERMEDIATES_TEMP_FOLDER}/merge_classes/${this.variant.name}")
                .get().asFile
    }

    File getAarMainJarFile() {
        return this.project.getLayout().getBuildDirectory()
                .file("intermediates/aar_main_jar/${this.variant.name}/sync${this.variant.name.capitalize()}LibJars/classes.jar")
                .get().asFile
    }

    File getAarMainClassesWithKotlinModulesDirectory() {
        return this.project.getLayout().getBuildDirectory()
                .dir("intermediates/${INTERMEDIATES_TEMP_FOLDER}/aar_main_classes_with_kotlin_modules/${this.variant.name}")
                .get().asFile
    }
}
