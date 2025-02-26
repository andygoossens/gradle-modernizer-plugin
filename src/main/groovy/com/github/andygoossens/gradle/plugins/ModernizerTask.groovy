/*
 * Copyright 2016-2024 Andy Goossens
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
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Internal
import org.xml.sax.SAXException

import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import static java.lang.String.format

@Slf4j
class ModernizerTask extends AbstractModernizerTask {

    private static final String CLASSPATH_PREFIX = "classpath:"
    private static final String MODERNIZER_CLASS = "org.gaul.modernizer_maven_plugin.Modernizer"
    private static final String SUPPRESS_DETECTOR_CLASS = "org.gaul.modernizer_maven_plugin.SuppressModernizerAnnotationDetector"
    private static final String GENERATOR_DETECTOR_CLASS = "org.gaul.modernizer_maven_plugin.SuppressGeneratedAnnotationDetector"

    @Internal
    ModernizerPluginExtension extension

    private def modernizer

    @Override
    void run(Object closure) {
        if (extension.skip) {
            log.info('Skipping modernizer execution')
            return
        }

        def modernizerClass = threadContextClassLoader.loadClass(MODERNIZER_CLASS)

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

        def suppressDetectorClass = threadContextClassLoader.loadClass(SUPPRESS_DETECTOR_CLASS)
        def suppressDetectMethod = suppressDetectorClass.getDeclaredMethod("detect", Path.class)
        def generatedDetectorClass = threadContextClassLoader.loadClass(GENERATOR_DETECTOR_CLASS)
        def generatedDetectMethod = generatedDetectorClass.getDeclaredMethod("detect", Path.class)
        Set<String> ignoreClassNames = new HashSet<String>()
        try {
            def detectionFileCollection = extension.mainOutputDirectories
            if (extension.includeTestClasses) {
                detectionFileCollection += extension.testOutputDirectories
            }

            def detectionFiles = detectionFileCollection.files
            for (File detectionFile : detectionFiles) {
                // Ignore classes annotated with org.gaul.modernizer_maven_annotations.SuppressModernize
                Path detectionPath = detectionFile.toPath() 
                Set<String> suppressedClassNames = suppressDetectMethod.invoke(null, detectionPath) as Set<String>
                ignoreClassNames.addAll(suppressedClassNames)
                
                if (extension.ignoreGeneratedClasses) {
                    // Ignore classes annotated with Generated (does not matter from which package)
                    Set<String> generatedClassNames = generatedDetectMethod.invoke(null, detectionPath) as Set<String>
                    ignoreClassNames.addAll(generatedClassNames)
                }
            }
        } catch (IOException e) {
            throw new GradleScriptException("Error reading suppressions", e);
        }

        Set<Pattern> allIgnoreClassNamePatterns = new HashSet<Pattern>()
        for (String pattern : extension.ignoreClassNamePatterns) {
            try {
                allIgnoreClassNamePatterns.add(Pattern.compile(pattern))
            } catch (PatternSyntaxException pse) {
                throw new GradleScriptException(
                        "Invalid ignore class name pattern", pse)
            }
        }

        modernizer = modernizerClass.newInstance(extension.javaVersion, allViolations, allExclusions,
                allExclusionPatterns, extension.ignorePackages, ignoreClassNames, allIgnoreClassNamePatterns)

        try {
            long count = recurseFileCollection(extension.mainSourceDirectories, extension.mainOutputDirectories)
            if (extension.includeTestClasses) {
                count += recurseFileCollection(extension.testSourceDirectories, extension.testOutputDirectories)
            }
            if (extension.failOnViolations && count != 0) {
                throw new GradleException("Found $count violations")
            }
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading classes", ioe)
        }
    }

    private static Map<String, Violation> parseViolations(Class modernizerClass, String violationsFilePath) {
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

    private long recurseFileCollection(FileCollection sourceDirectories, FileCollection outputDirectories) throws IOException {
        long count = 0
        for (File outputDirectory : outputDirectories.files) {
            count += recurseDirectories(sourceDirectories, outputDirectory, outputDirectory)
        }

        return count
    }

    private long recurseDirectories(FileCollection sourceDirectories, File outputDirectory, File outputFile) throws IOException {
        long count = 0
        if (!outputFile.exists()) {
            return count
        } else if (outputFile.isDirectory()) {
            String[] children = outputFile.list()
            if (children != null) {
                for (String child : children) {
                    count += recurseDirectories(sourceDirectories, outputDirectory, new File(outputFile, child))
                }
            }
        } else {
            count += processFile(sourceDirectories, outputDirectory, outputFile)
        }

        return count
    }

    private long processFile(FileCollection sourceDirectories, File outputDirectory, File outputFile) throws IOException {
        long count = 0
        if (!outputFile.exists()) {
            return count
        }

        if (outputFile.getPath().endsWith(".class")) {
            InputStream is = new FileInputStream(outputFile)
            try {
                Collection occurrences = modernizer.check(is)
                for (def occurrence : occurrences) {
                    String sourceFile = findSourceFile(sourceDirectories, outputDirectory, outputFile)
                    emitViolation(sourceFile, occurrence)

                    ++count
                }
            } finally {
                StreamUtils.closeQuietly(is)
            }
        }

        return count
    }

    private static String findSourceFile(FileCollection sourceDirectories, File outputDirectory, File outputFile) {
        String name = outputFile.getPath()

        Path outputFilePath = outputFile.toPath()
        Path outputDirectoryPath = outputDirectory.toPath()
        Path relativePath = outputDirectoryPath.relativize(outputFilePath)

        List<String> possibleSourceFiles = getPossibleSourceFiles(relativePath.toString())
        for (File sourceDir : sourceDirectories.files) {
            for (String possibleSourceFile : possibleSourceFiles) {
                Path possibleSourcePath = Paths.get(sourceDir.absolutePath, possibleSourceFile)
                if (Files.exists(possibleSourcePath)) {
                    return possibleSourcePath
                }
            }
        }

        // We tried our best, but we could not find the source file. Return the class file.
        name
    }

    private static List<String> getPossibleSourceFiles(String classFileName) {
        // Cut off the extension
        int i = classFileName.lastIndexOf('.');
        classFileName = classFileName.substring(0, i);

        // Cut off inner classes
        int j = classFileName.indexOf('$');
        if (j >= 0) {
            classFileName = classFileName.substring(0, j);
        }

        // Return in order of likelihood
        return [
                classFileName + ".java",
                classFileName + ".kt",
                classFileName + ".groovy",
        ]
    }

    private emitViolation(String name, occurrence) {
        def message = "$name:${occurrence.lineNumber}: ${occurrence.violation.comment}"
        if ("error" == extension.violationLogLevel) {
            log.error(message)
        } else if ("trace" == extension.violationLogLevel) {
            log.trace(message)
        } else if ("warn" == extension.violationLogLevel) {
            log.warn(message)
        } else if ("info" == extension.violationLogLevel) {
            log.info(message)
        } else if ("debug" == extension.violationLogLevel) {
            log.debug(message)
        } else {
            throw new IllegalStateException("unexpected log level, was: " + extension.violationLogLevel)
        }
    }
}
