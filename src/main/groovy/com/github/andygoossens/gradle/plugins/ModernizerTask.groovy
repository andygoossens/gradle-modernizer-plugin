/*
 * Copyright 2014-2015 Andrew Gaul
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

import com.github.andygoossens.gradle.plugins.utils.StreamUtils
import groovy.util.logging.Log
import org.gaul.modernizer_maven_plugin.Violation
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.xml.sax.SAXException

import javax.xml.parsers.ParserConfigurationException

@Log
class ModernizerTask extends AbstractModernizerTask {

    Project project
    ModernizerPluginExtension extension

    private def modernizer

    @Override
    void run(Object closure) {
        if (extension.skip) {
            log.info('Skipping modernizer execution')
            return
        }

        def modernizerClass = threadContextClassLoader.loadClass("org.gaul.modernizer_maven_plugin.Modernizer")

        Map<String, Violation> violations
        InputStream is
        if (extension.violationsFile == null) {
            is = modernizerClass.getResourceAsStream('/modernizer.xml')
        } else {
            File file = new File(extension.violationsFile)
            try {
                is = new FileInputStream(file)
            } catch (FileNotFoundException fnfe) {
                throw new GradleScriptException("Error opening violation file: $file", fnfe);
            }
        }
        try {
            violations = modernizerClass.parseFromXml(is);
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading violation data", ioe);
        } catch (ParserConfigurationException pce) {
            throw new GradleScriptException("Error parsing violation data", pce);
        } catch (SAXException saxe) {
            throw new GradleScriptException("Error parsing violation data", saxe);
        } finally {
            StreamUtils.closeQuietly(is)
        }

        Set<String> allExclusions = new HashSet<String>();
        allExclusions.addAll(extension.exclusions);
        if (extension.exclusionsFile != null) {
            is = null;
            try {
                File file = new File(extension.exclusionsFile);
                if (file.exists()) {
                    is = new FileInputStream(file);
                } else {
                    is = this.getClass().getClassLoader().getResourceAsStream(extension.exclusionsFile);
                }
                if (is == null) {
                    throw new GradleException("Could not find exclusion file: $extension.exclusionsFile");
                }

                allExclusions.addAll(StreamUtils.readAllLines(is));
            } catch (IOException ioe) {
                throw new GradleScriptException("Error reading exclusion file: $extension.exclusionsFile", ioe);
            } finally {
                StreamUtils.closeQuietly(is);
            }
        }

        modernizer = modernizerClass.newInstance(extension.javaVersion, violations, allExclusions, extension.ignorePackages)

        try {
            long count = recurseFiles(project.sourceSets.main.output.classesDir);
            if (extension.includeTestClasses) {
                count += recurseFiles(project.sourceSets.test.output.classesDir);
            }
            if (extension.failOnViolations && count != 0) {
                throw new GradleException("Found $count violations");
            }
        } catch (IOException ioe) {
            throw new GradleScriptException("Error reading Java classes", ioe);
        }
    }

    private long recurseFiles(File file) throws IOException {
        long count = 0;
        if (!file.exists()) {
            return count;
        }
        if (file.isDirectory()) {
            String[] children = file.list();
            if (children != null) {
                for (String child : children) {
                    count += recurseFiles(new File(file, child));
                }
            }
        } else if (file.getPath().endsWith(".class")) {
            InputStream is = new FileInputStream(file);
            try {
                Collection occurrences = modernizer.check(is);
                for (def occurrence : occurrences) {
                    String name = file.getPath();
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
                    log.warning("$name:${occurrence.lineNumber}: ${occurrence.violation.comment}");
                    ++count;
                }
            } finally {
                StreamUtils.closeQuietly(is);
            }
        }
        return count;
    }
}
