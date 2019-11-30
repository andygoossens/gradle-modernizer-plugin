/*
 * Copyright 2016-2019 Andy Goossens
 * Copyright 2014-2018 Andrew Gaul
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

import static java.util.Collections.emptyList

class ModernizerPluginExtension {

    @Internal
    FileCollection classpath

    /**
     * The version of modernizer-maven-plugin to be used.
     */
    @Input
    String toolVersion = null

    /**
     * Enables violations based on target Java version, e.g., 1.8. For example,
     * Modernizer will detect uses of Vector as violations when targeting Java
     * 1.2 but not when targeting Java 1.1.
     */
    @Input
    String javaVersion = null

    /** Fail task if Modernizer detects any violations. */
    @Input
    boolean failOnViolations = false

    /** Run Modernizer on test classes. */
    @Input
    boolean includeTestClasses = false

    /**
     * User-specified violation file. Also disables standard violation checks.
     * Can point to files from classpath using an absolute path, e.g.:
     *
     * classpath:/modernizer.xml
     *
     * for the default violations file.
     */
    @Input
    String violationsFile = "classpath:/modernizer.xml"

    /**
     * User-specified violation files. The violations loaded from
     * violationsFiles override the ones specified in violationsFile (or the
     * default violations file if no violationsFile is given). Violations from
     * the latter files override violations from the former files.
     *
     * Can point to files from classpath using an absolute path, e.g.:
     *
     * classpath:/modernizer.xml
     *
     * for the default violations file.
     */
    @Input
    List<String> violationsFiles = emptyList()

    /**
     * Disables user-specified violations. This is a text file with one
     * exclusion per line in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    @Input
    String exclusionsFile = null

    /**
     * Log level to emit violations at, e.g., error, warn, info, debug, trace.
     */
    @Input
    String violationLogLevel = "warn"

    /**
     * Classes annotated with {@code @Generated} will be excluded from
     * scanning.
     */
    @Input
    boolean ignoreGeneratedClasses = true

    /**
     * Violations to disable. Each exclusion should be in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    @Input
    Set<String> exclusions = new HashSet<String>()

    /**
     * Violation patterns to disable. Each exclusion should be a
     * regular expression that matches the javap format:
     *
     * java/lang/.*
     */
    @Input
    Set<String> exclusionPatterns = new HashSet<String>()

    /**
     * Package prefixes to ignore, specified using &lt;ignorePackage&gt; child
     * elements. Specifying foo.bar subsequently ignores foo.bar.*,
     * foo.bar.baz.* and so on.
     */
    @Input
    Set<String> ignorePackages = new HashSet<String>()
    
    /**
     * Full qualified class names (incl. package) to ignore, specified using
     * &lt;ignoreClassNamePattern&gt; child elements. Each exclusion should be
     * a regular expression that matches a package and/or class; the package
     * will be / not . separated (ASM's format).
     */
    @Input
    Set<String> ignoreClassNamePatterns = new HashSet<String>()

    /**
     * Skips the plugin execution.
     */
    @Input
    boolean skip = false
}
