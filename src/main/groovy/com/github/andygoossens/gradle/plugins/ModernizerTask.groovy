/*
 * Copyright 2016-2018 Andy Goossens
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

import com.github.andygoossens.gradle.plugins.utils.StreamUtils
import groovy.util.logging.Slf4j
import org.gaul.modernizer_maven_plugin.Violation
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Internal
import org.xml.sax.SAXException

import javax.xml.parsers.ParserConfigurationException
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import static java.lang.String.format

@Slf4j
class ModernizerTask extends AbstractModernizerTask {

    private static final String CLASSPATH_PREFIX = "classpath:"

    @Internal
    Project project
    
    @Internal
    ModernizerPluginExtension extension

    private def modernizer

    @Override
    void run(Object closure) {
        if (extension.skip) {
            log.info('Skipping modernizer execution')
            return
        }

        if (extension.javaVersion == null) {
            extension.setJavaVersion(project.targetCompatibility.toString())
        }
        
        def modernizerClass = threadContextClassLoader.loadClass("org.gaul.modernizer_maven_plugin.Modernizer")

        Map<String, Violation> allViolations = parseViolations(modernizerClass, extension.violationsFile)
        for (String violationsFilePath : extension.violationsFiles) {
            allViolations.putAll(parseViolations(modernizerClass, violationsFilePath))
        }

        Set<String> allExclusions = new HashSet<String>()
        allExclusions.addAll(extension.exclusions)
        if (extension.exclusionsFile != null) {
            allExclusions.addAll(readExclusionsFile(extension.exclusionsFile))
        }

        Set<Pattern> allExclusionPatterns = new HashSet<Pattern>()
        for (String pattern : extension.exclusionPatterns) {
            try {
                allExclusionPatterns.add(Pattern.compile(pattern))
            } catch (PatternSyntaxException pse) {
                throw new GradleScriptException(
                        "Invalid exclusion pattern", pse)
            }
        }

        modernizer = modernizerClass.newInstance(extension.javaVersion, allViolations, allExclusions,
                allExclusionPatterns, extension.ignorePackages)

        try {
            long count = recurseFileCollection(project.sourceSets.main.output.classesDirs)
            if (extension.includeTestClasses) {
                count += recurseFileCollection(project.sourceSets.test.output.classesDirs)
            }
            if (extension.failOnViolations && count != 0) {
                throw new GradleException("Found $count violations")
            }
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading Java classes", ioe)
        }
    }

    private Map<String, Violation> parseViolations(Class modernizerClass, String violationsFilePath) {
        InputStream is
        if (violationsFilePath.startsWith(CLASSPATH_PREFIX)) {
            String classpath =
                    violationsFilePath.substring(CLASSPATH_PREFIX.length())
            if (!classpath.startsWith("/")) {
                throw new IllegalArgumentException(format(
                        "Only absolute classpath references are allowed, got [%s]",
                        classpath))
            }
            is = modernizerClass.getResourceAsStream(classpath)
        } else {
            File file = new File(violationsFilePath)
            try {
                is = new FileInputStream(file)
            } catch (FileNotFoundException fnfe) {
                throw new GradleScriptException("Error opening violation file: $file", fnfe)
            }
        }
        try {
            return modernizerClass.parseFromXml(is)
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading violation data", ioe)
        } catch (ParserConfigurationException pce) {
            throw new GradleScriptException("Error parsing violation data", pce)
        } catch (SAXException saxe) {
            throw new GradleScriptException("Error parsing violation data", saxe)
        } finally {
            StreamUtils.closeQuietly(is)
        }
    }

    private Collection<String> readExclusionsFile(String exclusionsFilePath) {
        InputStream is = null
        try {
            File file = new File(exclusionsFilePath)
            if (file.exists()) {
                is = new FileInputStream(exclusionsFilePath)
            } else {
                is = this.getClass().getClassLoader().getResourceAsStream(
                        exclusionsFilePath)
            }
            if (is == null) {
                throw new GradleException("Could not find exclusion file: $extension.exclusionsFile")
            }

            return StreamUtils.readAllLines(is)
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading exclusion file: $extension.exclusionsFile", ioe)
        } finally {
            StreamUtils.closeQuietly(is)
        }
    }

    private long recurseFileCollection(FileCollection fileCollection) throws IOException {
        long count = 0
        for (File file : fileCollection.asList()) {
            count += recurseFiles(file)
        }
        
        return count
    }

    private long recurseFiles(File file) throws IOException {
        long count = 0
        if (!file.exists()) {
            return count
        }
        if (file.isDirectory()) {
            String[] children = file.list()
            if (children != null) {
                for (String child : children) {
                    count += recurseFiles(new File(file, child))
                }
            }
        } else if (file.getPath().endsWith(".class")) {
            InputStream is = new FileInputStream(file)
            try {
                Collection occurrences = modernizer.check(is)
                for (def occurrence : occurrences) {
                    String name = file.getPath()
                    // Commented out until there is a better way for doing this.
                    //
                    // Known issues:
                    // * Gradle supports multiple source dirs, so we need to loop over them to find the source file.
                    // * Maybe the source file wasn't Java at all? It could have been Groovy.
                    //
                    // It would be better if we let ASM extract the source file name from the .class file.
                    // And if that fails: fall back to .class file name.
                    
                    // Original Maven code:
//                    if (name.startsWith(outputDirectory.getPath())) {
//                        name = sourceDirectory.getPath() + name.substring(outputDirectory.getPath().length());
//                        name = name.substring(0, name.length() - ".class".length()) + ".java";
//                    } else if (name.startsWith(testOutputDirectory.getPath())) {
//                        name = testSourceDirectory.getPath() + name.substring(testOutputDirectory.getPath().length());
//                        name = name.substring(0, name.length() - ".class".length()) + ".java";
//                    }
                    emitViolation(name, occurrence)
                    ++count
                }
            } finally {
                StreamUtils.closeQuietly(is)
            }
        }
        return count
    }

    private emitViolation(String name, occurrence) {
        def message = "$name:${occurrence.lineNumber}: ${occurrence.violation.comment}"
        if ("error".equals(extension.violationLogLevel)) {
            log.error(message)
        } else if ("trace".equals(extension.violationLogLevel)) {
            log.trace(message)
        } else if ("warn".equals(extension.violationLogLevel)) {
            log.warn(message)
        } else if ("info".equals(extension.violationLogLevel)) {
            log.info(message)
        } else if ("debug".equals(extension.violationLogLevel)) {
            log.debug(message)
        } else {
            throw new IllegalStateException("unexpected log level, was: " + extension.violationLogLevel)
        }
    }
}
