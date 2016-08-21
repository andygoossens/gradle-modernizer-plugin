/*
 * Copyright 2014 the original author or authors.
 * Copyright 2016 Andy Goossens
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

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Internal

class ModernizerPluginExtension {

    @Internal
    FileCollection classpath

    /**
     * The version of modernizer-maven-plugin to be used.
     */
    String toolVersion = null

    /**
     * Enables violations based on target Java version, e.g., 1.8. For example,
     * Modernizer will detect uses of Vector as violations when targeting Java
     * 1.2 but not when targeting Java 1.1.
     */
    String javaVersion = null

    /** Fail task if Modernizer detects any violations. */
    boolean failOnViolations = false

    /** Run Modernizer on test classes. */
    boolean includeTestClasses = false

    /**
     * User-specified violation file. Also disables standard violation checks.
     */
    String violationsFile = null

    /**
     * Disables user-specified violations. This is a text file with one
     * exclusion per line in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    String exclusionsFile = null

    /**
     * Violations to disable. Each exclusion should be in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    Set<String> exclusions = new HashSet<String>();

    /**
     * Package prefixes to ignore, specified using &lt;ignorePackage&gt; child
     * elements. Specifying foo.bar subsequently ignores foo.bar.*,
     * foo.bar.baz.* and so on.
     */
    Set<String> ignorePackages = new HashSet<String>();

    /**
     * Skips the plugin execution.
     */
    boolean skip = false
}
