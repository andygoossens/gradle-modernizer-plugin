# Gradle Modernizer Plugin

![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.andygoossens.modernizer)

Gradle Modernizer Plugin detects uses of legacy APIs which modern Java versions
supersede.

These modern APIs are often more performant, safer, and idiomatic than the
legacy equivalents.
For example, Modernizer can detect uses of `Vector` instead of `ArrayList`,
`String.getBytes(String)` instead of `String.getBytes(Charset)`, and
Guava `Objects.equal` instead of Java 7 `Objects.equals`.
The default configuration detects
[over 200 legacy APIs](https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/resources/modernizer.xml),
including third-party libraries like
[Guava](https://github.com/google/guava).

This Gradle plugin is actually a wrapper around
[Modernizer Maven Plugin](https://github.com/gaul/modernizer-maven-plugin)
so that the same functionality is now available in Gradle builds.

## Usage

To use the plugin, include in your build script:

```groovy
// You need to do this only once
plugins {
    // Option A: When your root project has a SourceSet
    // e.g. the root project is applying the java/groovy/kotlin plugin as well 
    id "com.github.andygoossens.modernizer" version "1.8.0"
    // Option B: When your root project does not have a SourceSet
    id "com.github.andygoossens.modernizer" version "1.8.0" apply false
}

repositories {
    mavenCentral()
}

// Option 1: Apply the plugin in each project where you want to use it
// Gradle's old way:
apply plugin: 'com.github.andygoossens.modernizer'
// Gradle's new way:
plugins {
    id 'com.github.andygoossens.modernizer'
}

// Option 2: Apply the plugin in all projects (even in the root project)
//           Preferably used with option A (= not mentioning 'apply false')
allprojects {
    apply plugin: 'com.github.andygoossens.modernizer'
}

// Option 3: Apply the plugin in all sub-projects (but not the root project)
//           Preferably used with option B (= mentioning 'apply false')
subprojects {
    apply plugin: 'com.github.andygoossens.modernizer'
}
```

If you are still using the old plugin id `com.github.andygoossens.gradle-modernizer-plugin` then
please switch to `com.github.andygoossens.modernizer` instead.

If you want to call the `modernizer` task directly:
```bash
./gradlew modernizer
```

Most of the time you don't need to call the `modernizer` task yourself.
The task knows its place inside the build lifecycle, and it will execute
whenever it is deemed necessary.

### Extension properties

| Property name         | Type   | Default value                | Description                                                                                      |
|-----------------------|--------|------------------------------|--------------------------------------------------------------------------------------------------|
|toolVersion            |String  |See table below               |Version of modernizer-maven-plugin that will be used.                                             |
|javaVersion            |String  |${project.targetCompatibility}|Target Java version. Decides which violations will apply.                                         |
|failOnViolations       |boolean |false                         |Fail build when a violation has been detected.                                                    |
|includeTestClasses     |boolean |false                         |Whether test classes will be searched for violations.                                             |
|violationLogLevel      |String  |warn                          |Logs violations at this level. Possible values: error, warn, info, debug, trace                   |
|violationsFile         |String  |null                          |User-specified violation file. Overrides standard violation checks.                               |
|violationsFiles        |String[]|[]                            |User-specified violation files. Overrides `violationsFile` and standard checks.                   |
|exclusionsFile         |String  |null                          |Disables user-specified violations. See format description below.                                 |
|exclusions             |String[]|[]                            |Violations to disable. See format description below.                                              |
|exclusionPatterns      |String[]|[]                            |Violation patterns to disable. See format description below.                                      |
|ignorePackages         |String[]|[]                            |Package prefixes to ignore. See format description below.                                         |
|ignoreClassNamePatterns|String[]|[]                            |Full qualified class names (incl. package) to ignore. See format description below.               |
|ignoreGeneratedClasses |boolean |true                          |Whether classes annotated with an annotation whose retention policy is <code>runtime</code> or <code>class</code> and whose simple name contain "Generated" will be ignored.|
|skip                   |boolean |false                         |Whether task should be skipped.                                                                   |

#### Usage example

```groovy
modernizer {
    failOnViolations = true
    includeTestClasses = true
}
```

Note that you can only configure the plugin in projects where the plugin has been applied.
See "Usage" section above.

#### Formats

##### exclusionsFile

This is a text file with one exclusion per line in the javap format.

Example:
```
java/lang/String.getBytes:(Ljava/lang/String;)[B
com/google/common/base/Function
sun/misc/BASE64Decoder.decodeBuffer:(Ljava/lang/String;)[B
```

##### exclusions

This is a list of exclusions. Each exclusion should be in the javap format.

Example:
```groovy
exclusions = [
        'java/lang/String.getBytes:(Ljava/lang/String;)[B',
        'com/google/common/base/Function',
        'sun/misc/BASE64Decoder.decodeBuffer:(Ljava/lang/String;)[B',
]
```

##### exclusionPatterns

This is a list of exclusion patterns. Each exclusion should be a regular
expression that matches the javap format of a violation.

Example:
```groovy
exclusionPatterns = [
        'java/lang/.*',
]
```

##### ignorePackages

This is a list of package prefixes for which no violations will be reported.

Ignoring a package will ignore all its children.
Specifying foo.bar subsequently ignores foo.bar.\*, foo.bar.baz.\* and so on.

Example:
```groovy
ignorePackages = [
        'com.github.andygoossens',
]
```

##### ignoreClassNamePatterns

This is a list of full qualified class names (incl. package) to ignore.
Each exclusion should be a regular expression that matches a package and/or class;
the package will be / not . separated (ASM's format).

Example:
```groovy
ignoreClassNamePatterns = [
        '.*MyLegacyClass',
]
```

### Ignoring elements

You cannot only ignore elements by using extension properties (see above), you
can indicate that violations within a class or method should be ignored by
the plugin by adding `@SuppressModernizer` to the element you'd like
to ignore:

```java
import org.gaul.modernizer_maven_annotations.SuppressModernizer;

public class Example {
    @SuppressModernizer
    public static void method() { ... }
}
```

Add the following dependency to your Gradle build script:

```groovy
// Option 1: compile time dependency (Gradle's old way)
compile 'org.gaul:modernizer-maven-annotations:2.6.0'

// Option 2: implementation dependency (Gradle's new way)
implementation 'org.gaul:modernizer-maven-annotations:2.6.0'
```

## Version comparison

Gradle Modernizer Plugin is basically a wrapper around Modernizer Maven Plugin.
The table below describes how they relate to each other.

| Gradle Modernizer Plugin | Modernizer Maven Plugin |
|--------------------------|-------------------------|
| 1.0.x                    | 1.6.0                   |
| 1.1.x                    | 1.6.0                   |
| 1.2.x                    | 1.8.0                   |
| 1.3.x                    | 2.0.0                   |
| 1.4.x                    | 2.1.0                   |
| 1.5.x                    | 2.2.0                   |
| 1.6.x                    | 2.3.0                   |
| 1.7.x                    | 2.5.0                   |
| 1.8.x                    | 2.6.0                   |

Note that you can override the default version of Modernizer Maven Plugin which will be used.
Specify in the `toolVersion` extension property the version that you want to use. Pay attention:
This might break when there is an API change!


## FAQ

### I found an undetected case of legacy API. Can you add a new violation rule?

Sounds great! However, I cannot add it myself as
[Modernizer Maven Plugin](https://github.com/gaul/modernizer-maven-plugin) maintains the list of violations rules.
Open an issue there and describe what you found.

Note that it might take some time for them to release a new version with your rule.

### There is a new version of Modernizer Maven Plugin but there is no corresponding version of your plugin.

That is not a question. Unfortunately, I might not have found the time to release a new version of the Gradle plugin.

In the meanwhile you can specify the desired version in the `toolVersion` extension property.
The Gradle plugin will then pickup the requested version and, if the API is still the same, use it.

### Will you add a feature X?

That depends on whether the feature is specific to Gradle. If it is, then I will see what I can do.
However, if it requires changes in Modernizer Maven Plugin, then it is up to its maintainers.


## References

* [Modernizer Maven Plugin](https://github.com/gaul/modernizer-maven-plugin) 

## License

```
Licensed under the Apache License, Version 2.0
Copyright (C) 2016-2023 Andy Goossens

Inspired by, and based upon code from:

Modernizer Maven Plugin
Copyright (C) 2014-2023 Andrew Gaul

Gradle Docker plugin
Copyright (C) 2014 the original author or authors.
```