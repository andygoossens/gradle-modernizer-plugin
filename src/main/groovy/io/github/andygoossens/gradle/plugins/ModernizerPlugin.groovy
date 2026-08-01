/*
 * Copyright 2016-2026 Andy Goossens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.andygoossens.gradle.plugins

import io.github.andygoossens.gradle.plugins.utils.ModernizerThreadContextClassLoader
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class ModernizerPlugin implements Plugin<Project> {

    public static final String MODERNIZER_CONFIGURATION_NAME = 'modernizer'
    public static final String MODERNIZER_DEFAULT_VERSION = '3.5.0'
    public static final String EXTENSION_NAME = 'modernizer'
    public static final String TASK_NAME = 'modernizer'

    @Override
    void apply(Project project) {
        project.getPluginManager().withPlugin("java") {
            ModernizerPluginExtension extension = createExtension(project)
            Configuration configuration = configureConfiguration(project, extension)

            configureAbstractModernizerTask(project, configuration)
            createTask(project, extension)
        }
    }

    private static Configuration configureConfiguration(Project project,
                                                        ModernizerPluginExtension extension) {
        project.getConfigurations().create(MODERNIZER_CONFIGURATION_NAME, { Configuration c ->
            c.setVisible(false)
            c.setTransitive(true)
            c.setDescription("The Modernizer libraries to be used for this project.")
            // avoid CVE warning from upstream dependency. see issue #1
            c.exclude([group: "org.codehaus.plexus", module: "plexus-utils"])
            c.defaultDependencies(d ->
                d.add(project.dependencies.create("org.gaul:modernizer-maven-plugin:${extension.getToolVersion().get()}")))
        })
    }

    private static ModernizerPluginExtension createExtension(Project project) {
        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class)

        SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        FileCollection mainSourceDirectories = mainSourceSet.allSource.sourceDirectories
        FileCollection mainOutputDirectories = mainSourceSet.output.classesDirs

        SourceSet testSourceSet = javaPluginExtension.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME)
        FileCollection testSourceDirectories = testSourceSet.allSource.sourceDirectories
        FileCollection testOutputDirectories = testSourceSet.output.classesDirs

        ModernizerPluginExtension extension = project.extensions.create(EXTENSION_NAME, ModernizerPluginExtension,
            mainSourceDirectories, mainOutputDirectories,
            testSourceDirectories, testOutputDirectories,
            MODERNIZER_DEFAULT_VERSION)

        // LAZY CONVENTION BINDING
        extension.javaVersion.convention(project.provider(() -> {
            // Check toolchain first (Modern Gradle), then fall back to targetCompatibility
            if (javaPluginExtension.toolchain.languageVersion.isPresent()) {
                return javaPluginExtension.toolchain.languageVersion.get().toString()
            }
            
            return javaPluginExtension.targetCompatibility?.toString()
        }))
        
        return extension
    }

    private static void configureAbstractModernizerTask(Project project, Configuration configuration) {
        ModernizerThreadContextClassLoader modernizerClassLoader = new ModernizerThreadContextClassLoader()
        project.tasks.withType(AbstractModernizerTask).configureEach({ AbstractModernizerTask task ->
            task.group = "Verification"
            task.description = "Detects use of legacy APIs which modern Java versions supersede."
            task.threadContextClassLoader = modernizerClassLoader
            task.classpath.from(configuration)
        })
    }

    private static void createTask(Project project, ModernizerPluginExtension extension) {
        TaskProvider<ModernizerTask> taskProvider = project.tasks.register(TASK_NAME, ModernizerTask, task -> {
            task.toolVersion.set(extension.toolVersion)
            task.javaVersion.set(extension.javaVersion)
            task.failOnViolations.set(extension.failOnViolations)
            task.includeTestClasses.set(extension.includeTestClasses)
            task.violationsFile.set(extension.violationsFile)
            task.violationsFiles.set(extension.violationsFiles)
            task.exclusionsFile.set(extension.exclusionsFile)
            task.violationLogLevel.set(extension.violationLogLevel)
            task.ignoreGeneratedClasses.set(extension.ignoreGeneratedClasses)
            task.exclusions.set(extension.exclusions)
            task.exclusionPatterns.set(extension.exclusionPatterns)
            task.ignorePackages.set(extension.ignorePackages)
            task.ignoreClassNamePatterns.set(extension.ignoreClassNamePatterns)
            task.skip.set(extension.skip)

            task.mainSourceDirectories.from(extension.mainSourceDirectories)
            task.mainOutputDirectories.from(extension.mainOutputDirectories)
            task.testSourceDirectories.from(extension.testSourceDirectories)
            task.testOutputDirectories.from(extension.testOutputDirectories)
            
            task.dependsOn(JavaPlugin.CLASSES_TASK_NAME)

            if (extension.includeTestClasses.get()) {
                task.dependsOn(JavaPlugin.TEST_CLASSES_TASK_NAME)
            }
        })

        project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME, t ->
            t.dependsOn(taskProvider)
        )
    }
}
