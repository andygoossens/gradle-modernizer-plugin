/*
 * Copyright 2016-2019 Andy Goossens
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
package com.github.andygoossens.gradle.plugins

import com.github.andygoossens.gradle.plugins.utils.ModernizerThreadContextClassLoader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class ModernizerPlugin implements Plugin<Project> {

    public static final String MODERNIZER_CONFIGURATION_NAME = 'modernizer'
    public static final String MODERNIZER_DEFAULT_VERSION = '2.6.0'
    public static final String EXTENSION_NAME = 'modernizer'
    public static final String TASK_NAME = 'modernizer'

    @Override
    void apply(Project project) {
        Configuration configuration = configureConfiguration(project)

        ModernizerPluginExtension extension = createExtension(project)
        configureDefaultDependencies(project, configuration, extension)

        extension.classpath = configuration

        configureAbstractModernizerTask(project, configuration, extension)
        ModernizerTask modernizerTask = createTask(project, extension)
        addTaskDependencies(project, modernizerTask, extension)
    }

    private static Configuration configureConfiguration(Project project) {
        project.getConfigurations().create(MODERNIZER_CONFIGURATION_NAME)
            .setVisible(false)
            .setTransitive(true)
            .setDescription("The Modernizer libraries to be used for this project.")
            // avoid CVE warning from upstream dependency. see issue #1
            .exclude([group: "org.codehaus.plexus", module: "plexus-utils"])
    }

    private static ModernizerPluginExtension createExtension(Project project) {
        ModernizerPluginExtension extension = project.extensions.create(EXTENSION_NAME, ModernizerPluginExtension)
        extension.setToolVersion(MODERNIZER_DEFAULT_VERSION)

        extension
    }

    private static void configureDefaultDependencies(Project project, Configuration configuration,
                                                     ModernizerPluginExtension extension) {
        configuration.defaultDependencies { dependencies ->
            dependencies.add(
                    project.dependencies.create("org.gaul:modernizer-maven-plugin:${extension.getToolVersion()}"))
        }
    }

    private void configureAbstractModernizerTask(Project project, Configuration configuration,
                                                 ModernizerPluginExtension extension) {
        ModernizerThreadContextClassLoader modernizerClassLoader = new ModernizerThreadContextClassLoader(extension)
        project.tasks.withType(AbstractModernizerTask) {
            group = "Verification"
            description = "Detects use of legacy APIs which modern Java versions supersede."
            threadContextClassLoader = modernizerClassLoader

            conventionMapping.with {
                classpath = { configuration }
            }
        }
    }

    private static ModernizerTask createTask(Project project, ModernizerPluginExtension extension) {
        ModernizerTask modernizerTask = project.tasks.create(TASK_NAME, ModernizerTask)
        modernizerTask.setExtension(extension)

        modernizerTask
    }

    private
    static addTaskDependencies(Project project, ModernizerTask modernizerTask, ModernizerPluginExtension extension) {
        project.configure(project) {
            afterEvaluate {
                modernizerTask.dependsOn('classes')
                
                if (extension.includeTestClasses) {
                    modernizerTask.dependsOn('testClasses')
                }

                project.getTasksByName('check', false).each {
                    t -> t.dependsOn(modernizerTask)
                }
            }
        }
    }
}
