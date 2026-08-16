/*
 * Copyright 2016-2026 Andy Goossens
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
package io.github.andygoossens.gradle.plugins

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.github.andygoossens.gradle.plugins.utils.StreamUtils
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.*
import org.xml.sax.SAXException

import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Slf4j
@CompileStatic
@CacheableTask
abstract class ModernizerTask extends AbstractModernizerTask {

    private static final String MODERNIZER_CLASS = "org.gaul.modernizer_maven_plugin.Modernizer"
    private static final String SUPPRESS_DETECTOR_CLASS = "org.gaul.modernizer_maven_plugin.SuppressModernizerAnnotationDetector"
    private static final String GENERATOR_DETECTOR_CLASS = "org.gaul.modernizer_maven_plugin.SuppressGeneratedAnnotationDetector"

    /**
     * Plain data carrier for a single violation occurrence, extracted from the
     * reflectively-loaded ViolationOccurrence instance inside
     * {@link #createModernizerCheck(java.lang.Class, java.lang.String, java.util.Map, java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.util.Set, boolean)},
     * so the rest of this class never has to touch that type directly.
     */
    private static class ViolationRecord {
        final int lineNumber
        final String comment

        ViolationRecord(int lineNumber, String comment) {
            this.lineNumber = lineNumber
            this.comment = comment
        }
    }

    @Input
    abstract Property<String> getToolVersion()

    @Input
    abstract Property<String> getJavaVersion()

    @Input
    abstract Property<Boolean> getFailOnViolations()

    @Input
    abstract Property<Boolean> getIncludeTestClasses()

    @Nested
    @Optional
    abstract Property<TextResource> getViolationsFile()

    @Nested
    abstract ListProperty<TextResource> getViolationsFiles()

    @Nested
    @Optional
    abstract Property<TextResource> getExclusionsFile()

    @Input
    abstract Property<String> getViolationLogLevel()

    @Input
    abstract Property<Boolean> getIgnoreGeneratedClasses()

    @Input
    abstract SetProperty<String> getExclusions()

    @Input
    abstract SetProperty<String> getExclusionPatterns()

    @Input
    abstract SetProperty<String> getIgnorePackages()

    @Input
    abstract SetProperty<String> getIgnoreClassNamePatterns()

    @Input
    abstract Property<Boolean> getSkip()

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getMainSourceDirectories()

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getMainOutputDirectories()

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getTestSourceDirectories()

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getTestOutputDirectories()

    @OutputFile
    abstract RegularFileProperty getReportFile()

    private Function<InputStream, List<ViolationRecord>> modernizerCheck

    @Override
    void run(Object closure) {
        if (getSkip().get()) {
            log.info('Skipping modernizer execution')
            getReportFile().get().asFile.text =
                """Modernizer marker file
Finished at: ${System.currentTimeMillis()}
Skipped execution
"""
            return
        }

        Class modernizerClass = threadContextClassLoader.loadClass(MODERNIZER_CLASS)

        Map<String, Collection<? /*Violation*/>> allViolations =
            parseViolations(modernizerClass, resolveViolationsStream(modernizerClass))
        for (TextResource resource : getViolationsFiles().get()) {
            allViolations.putAll(parseViolations(modernizerClass, resource.asFile().newInputStream()))
        }

        Set<String> allExclusions = new HashSet<String>()
        allExclusions.addAll(getExclusions().get())
        allExclusions.addAll(readExclusions())

        Set<Pattern> allExclusionPatterns = new HashSet<Pattern>()
        for (String pattern : getExclusionPatterns().get()) {
            try {
                allExclusionPatterns.add(Pattern.compile(pattern))
            } catch (PatternSyntaxException pse) {
                throw new GradleScriptException("Invalid exclusion pattern", pse)
            }
        }

        Function<Path, Set<String>> suppressDetector =
            bindDetector(threadContextClassLoader.loadClass(SUPPRESS_DETECTOR_CLASS))
        Function<Path, Set<String>> generatedDetector =
            bindDetector(threadContextClassLoader.loadClass(GENERATOR_DETECTOR_CLASS))

        Set<String> ignoreClassNames = new HashSet<String>()
        try {
            FileCollection detectionFileCollection = getMainOutputDirectories()
            if (getIncludeTestClasses().get()) {
                detectionFileCollection = detectionFileCollection + getTestOutputDirectories()
            }

            def detectionFiles = detectionFileCollection.files
            for (File detectionFile : detectionFiles) {
                // Ignore classes annotated with org.gaul.modernizer_maven_annotations.SuppressModernize
                Path detectionPath = detectionFile.toPath()
                ignoreClassNames.addAll(suppressDetector.apply(detectionPath))

                if (getIgnoreGeneratedClasses().get()) {
                    // Ignore classes annotated with Generated (does not matter from which package)
                    ignoreClassNames.addAll(generatedDetector.apply(detectionPath))
                }
            }
        } catch (IOException e) {
            throw new GradleScriptException("Error reading suppressions", e)
        }

        Set<Pattern> allIgnoreClassNamePatterns = new HashSet<Pattern>()
        for (String pattern : getIgnoreClassNamePatterns().get()) {
            try {
                allIgnoreClassNamePatterns.add(Pattern.compile(pattern))
            } catch (PatternSyntaxException pse) {
                throw new GradleScriptException("Invalid ignore class name pattern", pse)
            }
        }

        modernizerCheck = createModernizerCheck(modernizerClass, getJavaVersion().get(), allViolations, allExclusions,
            allExclusionPatterns, getIgnorePackages().get(), ignoreClassNames, allIgnoreClassNamePatterns,
            getIgnoreGeneratedClasses().get())

        try {
            long count = recurseFileCollection(getMainSourceDirectories(), getMainOutputDirectories())
            if (getIncludeTestClasses().get()) {
                count += recurseFileCollection(getTestSourceDirectories(), getTestOutputDirectories())
            }

            getReportFile().get().asFile.text =
                """Modernizer marker file
Finished at: ${System.currentTimeMillis()}
Violations: $count
"""
            
            if (getFailOnViolations().get() && count != 0) {
                throw new GradleException("Found $count violations")
            }
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading classes", ioe)
        }
    }

    // ------------------------------------------------------------------------
    // The only dynamic seams in this class: everything below this point
    // touches types loaded from the user-configured modernizer version.

    @CompileDynamic
    private InputStream resolveViolationsStream(Class modernizerClass) {
        if (getViolationsFile().isPresent()) {
            return getViolationsFile().get().asFile().newInputStream()
        }

        return modernizerClass.getResourceAsStream("/modernizer.xml")
    }

    @CompileDynamic
    private static Map<String, Collection<? /*Violation*/>> parseViolations(Class modernizerClass, InputStream is) {
        try (InputStream stream = is) {
            return modernizerClass.parseFromXml(stream) as Map<String, Collection<?>>
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading violation data", ioe)
        } catch (ParserConfigurationException pce) {
            throw new GradleScriptException("Error parsing violation data", pce)
        } catch (SAXException saxe) {
            throw new GradleScriptException("Error parsing violation data", saxe)
        }
    }

    @CompileDynamic
    private static Function<Path, Set<String>> bindDetector(Class detectorClass) {
        def method = detectorClass.getDeclaredMethod("detect", Path)
        return { Path p -> method.invoke(null, p) as Set<String> } as Function<Path, Set<String>>
    }

    @CompileDynamic
    private static Function<InputStream, List<ViolationRecord>> createModernizerCheck(
        Class modernizerClass, String javaVersion,
        Map<String, Collection<?>> violations, Set<String> exclusions, Set<Pattern> exclusionPatterns,
        Set<String> ignorePackages, Set<String> ignoreClassNames, Set<Pattern> ignoreClassNamePatterns,
        boolean ignoreGeneratedClasses) {
        def instance = modernizerClass.newInstance(javaVersion, violations, exclusions, exclusionPatterns,
            ignorePackages, ignoreClassNames, ignoreClassNamePatterns, ignoreGeneratedClasses)

        return { InputStream is ->
            Collection violationOccurrences = instance.check(is)
            violationOccurrences.collect { violationOccurrence ->
                new ViolationRecord(
                    violationOccurrence.lineNumber as int,
                    violationOccurrence.violation.comment as String
                )
            } as List<ViolationRecord>
        } as Function<InputStream, List<ViolationRecord>>
    }

    // Fully statically-checked from here on.
    // ------------------------------------------------------------------------

    private Collection<String> readExclusions() {
        if (!getExclusionsFile().isPresent()) {
            return []
        }

        try (InputStream is = getExclusionsFile().get().asFile().newInputStream()) {
            return StreamUtils.readAllLines(is)
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading exclusion file", ioe)
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
                List<ViolationRecord> occurrences = modernizerCheck.apply(is)
                for (ViolationRecord occurrence : occurrences) {
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
                    return possibleSourcePath.toString()
                }
            }
        }

        // We tried our best, but we could not find the source file. Return the class file.
        return name
    }

    private static List<String> getPossibleSourceFiles(String classFileName) {
        // Cut off the extension
        int i = classFileName.lastIndexOf('.')
        classFileName = classFileName.substring(0, i)

        // Cut off inner classes
        int j = classFileName.indexOf('$')
        if (j >= 0) {
            classFileName = classFileName.substring(0, j)
        }

        // Return in order of likelihood
        return [
            classFileName + ".java",
            classFileName + ".kt",
            classFileName + ".groovy",
        ]
    }

    private void emitViolation(String name, ViolationRecord occurrence) {
        String message = "$name:${occurrence.lineNumber}: ${occurrence.comment}"
        String violationLogLevel = getViolationLogLevel().get()
        if ("error" == violationLogLevel) {
            log.error(message)
        } else if ("trace" == violationLogLevel) {
            log.trace(message)
        } else if ("warn" == violationLogLevel) {
            log.warn(message)
        } else if ("info" == violationLogLevel) {
            log.info(message)
        } else if ("debug" == violationLogLevel) {
            log.debug(message)
        } else {
            throw new IllegalStateException("unexpected log level, was: $violationLogLevel")
        }
    }
}
