# Gradle Modernizer Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.andygoossens.modernizer)](https://plugins.gradle.org/plugin/com.github.andygoossens.modernizer)

Gradle Modernizer Plugin detects uses of legacy APIs which modern Java versions
supersede.

These modern APIs are often more performant, safer, and idiomatic than the
legacy equivalents.

For example, Modernizer can detect uses of:

* `Collections.sort` instead of `List.sort`
* `String.getBytes(String)` instead of `String.getBytes(Charset)`
* Guava `ByteStreams.toByteArray(InputStream)` instead of `InputStream.readAllBytes()`.

The default configuration detects
[over 400 legacy APIs](https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/resources/modernizer.xml),
including third-party libraries like
[Apache Commons](https://commons.apache.org/),
[Guava](https://github.com/google/guava),
and [Joda-Time](https://www.joda.org/joda-time/).

This Gradle plugin is actually a wrapper around
[Modernizer Maven Plugin](https://github.com/gaul/modernizer-maven-plugin)
so that the same functionality is now available in Gradle builds.

## Usage

To use the plugin, include the following in your build script:

<details>
  <summary>Groovy DSL</summary>

```groovy
// You need to do this only once
plugins {
    // The Java plugin should already have been applied
    id 'java'
    // Option A: When your root project has a SourceSet
    // e.g. the root project is applying the java/groovy/kotlin plugin as well 
    id "com.github.andygoossens.modernizer" version "1.15.0"
    // Option B: When your root project does not have a SourceSet
    id "com.github.andygoossens.modernizer" version "1.15.0" apply false
}

repositories {
    mavenCentral()
}

// Apply the plugin

// Option 1: Apply the plugin in each project where you want to use it
// pick ONE of the two styles below
// Gradle's old way:
apply plugin: "com.github.andygoossens.modernizer"
// Gradle's new way:
plugins {
    id "com.github.andygoossens.modernizer"
}

// Option 2: Apply the plugin in all projects (even in the root project)
//           Preferably used with option A (= not mentioning 'apply false')
allprojects {
    apply plugin: "com.github.andygoossens.modernizer"
}

// Option 3: Apply the plugin in all subprojects (but not the root project)
//           Preferably used with option B (= mentioning 'apply false')
subprojects {
    apply plugin: "com.github.andygoossens.modernizer"
}
```
</details>

<details>
  <summary>Kotlin DSL</summary>

```kotlin
// You need to do this only once
plugins {
    // The Java plugin should already have been applied
    java
    // Option A: When your root project has a SourceSet
    // e.g., the root project is applying the java/groovy/kotlin plugin as well 
    id("com.github.andygoossens.modernizer") version "1.15.0"
    // Option B: When your root project does not have a SourceSet
    id("com.github.andygoossens.modernizer") version "1.15.0" apply false
}

repositories {
    mavenCentral()
}

// Apply the plugin

// Option 1: Apply the plugin in each project where you want to use it
plugins.apply("com.github.andygoossens.modernizer")

// Option 2: Apply the plugin in all projects (even in the root project)
//           Preferably used with option A (= not mentioning 'apply false')
allprojects {
    plugins.apply("com.github.andygoossens.modernizer")
}

// Option 3: Apply the plugin in all subprojects (but not the root project)
//           Preferably used with option B (= mentioning 'apply false')
subprojects {
    plugins.apply("com.github.andygoossens.modernizer")
}
```
</details>

If you are still using the old plugin id `com.github.andygoossens.gradle-modernizer-plugin` then
please switch to `com.github.andygoossens.modernizer` instead.

Note that Gradle's Java plugin should already have been applied in the 
same Gradle project!
If it is missing, or it is getting applied too late, the Modernizer plugin
won't be active.
If you are using one of these plugins, you should be fine:
* `java`
* `java-library`
* `application`
* `groovy`
* `org.jetbrains.kotlin.jvm`

If you want to execute the `modernizer` task directly:
```bash
./gradlew modernizer
```

Most of the time you don't need to call the `modernizer` task yourself.
The task knows its place inside the build lifecycle, and it will execute
whenever it is deemed necessary.

### Extension properties

| Property name                                       | Type               | Default value          | Description                                                                                                                                                                     |
|-----------------------------------------------------|--------------------|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| toolVersion                                         | String             | See table below        | Version of modernizer-maven-plugin that will be used.                                                                                                                           |
| javaVersion                                         | String             | Project's Java version | Target Java version. Decides which violations will apply. Defaults to the toolchain language version if configured, else `project.targetCompatibility`.                         |
| failOnViolations                                    | boolean            | false                  | Fail build when a violation has been detected.                                                                                                                                  |
| includeTestClasses                                  | boolean            | false                  | Whether test classes will be searched for violations.                                                                                                                           |
| violationLogLevel                                   | String             | warn                   | Logs violations at this level. Possible values: error, warn, info, debug, trace                                                                                                 |
| [violationsFile](#violationsfile)                   | TextResource       |                        | User-specified violation resource.                                                                                                                                              |
| [violationsFiles](#violationsfiles)                 | List<TextResource> | []                     | User-specified violation resources.                                                                                                                                             |
| [exclusionsFile](#exclusionsfile)                   | TextResource       |                        | User-specified exclusion resource. See format description below.                                                                                                                |
| [exclusions](#exclusions)                           | Set<String>        | []                     | Violations to disable. See format description below.                                                                                                                            |
| [exclusionPatterns](#exclusionpatterns)             | Set<String>        | []                     | Violation patterns to disable. See format description below.                                                                                                                    |
| [ignorePackages](#ignorepackages)                   | Set<String>        | []                     | Package prefixes to ignore. See format description below.                                                                                                                       |
| [ignoreClassNamePatterns](#ignoreclassnamepatterns) | Set<String>        | []                     | Full qualified class names (incl. package) to ignore. See format description below.                                                                                             |
| [ignoreGeneratedClasses](#ignoregeneratedclasses)   | boolean            | true                   | Whether classes annotated with an annotation whose retention policy is <code>runtime</code> or <code>class</code> and whose simple name is exactly "Generated" will be ignored. |
| skip                                                | boolean            | false                  | Whether task should be skipped.                                                                                                                                                 |

#### Usage example

```groovy
modernizer {
    failOnViolations = true
    includeTestClasses = true
}
```

Note that you can only configure the plugin in projects where the plugin has been applied.
See "Usage" section above.

#### Properties

##### violationsFile

This is a text resource that contains violations.
The XML content needs to be formatted according to
[Modernizer Maven Plugin's rules](https://github.com/gaul/modernizer-maven-plugin#custom-violations).

Any specified value will ignore all bundled violations.

Content example:
```xml
<?xml version="1.0"?>
<modernizer>
  <violation>
    <name>com/google/common/base/Function</name>
    <version>1.8</version>
    <comment>Prefer java.util.function.Function</comment>
  </violation>
</modernizer>
```

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    violationsFile = resources.text.fromFile("src/main/resources/violations.xml")
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    violationsFile = resources.text.fromFile("src/main/resources/violations.xml")
}
```
</details>

##### violationsFiles

A list of text resources containing violations.
Each entry's XML content needs to be formatted according to
[Modernizer Maven Plugin's rules](https://github.com/gaul/modernizer-maven-plugin#custom-violations).

When violations share the same `<name>` value, then a later definition will override earlier one.
Bundled violations or violations from `violationsFile` are the earliest definitions. 

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    violationsFiles = [
        resources.text.fromFile("src/main/resources/violations.xml")
    ]
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    violationsFiles = listOf(
        resources.text.fromFile("src/main/resources/violations.xml")
    )
}
```
</details>

##### exclusionsFile

This is a text resource with one exclusion per line in the javap format.

Content example:
```
java/lang/String.getBytes:(Ljava/lang/String;)[B
com/google/common/base/Function
sun/misc/BASE64Decoder.decodeBuffer:(Ljava/lang/String;)[B
```

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    exclusionsFile = resources.text.fromFile("src/main/resources/exclusions.xml")
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    exclusionsFile = resources.text.fromFile("src/main/resources/exclusions.xml")
}
```
</details>

##### exclusions

This is a list of exclusions. Each exclusion should be in the javap format.

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    exclusions = [
        'java/lang/String.getBytes:(Ljava/lang/String;)[B',
        'com/google/common/base/Function',
        'sun/misc/BASE64Decoder.decodeBuffer:(Ljava/lang/String;)[B',
    ]
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    exclusions = setOf(
        "java/lang/String.getBytes:(Ljava/lang/String;)[B",
        "com/google/common/base/Function",
        "sun/misc/BASE64Decoder.decodeBuffer:(Ljava/lang/String;)[B",
    )
}
```
</details>

##### exclusionPatterns

This is a list of exclusion patterns. Each exclusion should be a regular
expression that matches the javap format of a violation.

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    exclusionPatterns = [
        'java/lang/.*',
    ]
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    exclusionPatterns = setOf(
        "java/lang/.*",
    )
}
```
</details>

##### ignorePackages

This is a list of package prefixes for which no violations will be reported.

Ignoring a package will ignore all its children.
Specifying `foo.bar` subsequently ignores `foo.bar.*`, `foo.bar.baz.*` and so on.

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    ignorePackages = [
        'com.github.andygoossens',
    ]
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    ignorePackages = setOf(
        "com.github.andygoossens",
    )
}
```
</details>

##### ignoreClassNamePatterns

This is a list of full qualified class names (incl. package) to ignore.
Each exclusion should be a regular expression that matches a package and/or class;
the package name will be slash-separated, not dot-separated ([ASM's format](https://asm.ow2.io/faq.html#Q7)).

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    ignoreClassNamePatterns = [
        '.*MyLegacyClass',
    ]
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    ignoreClassNamePatterns = setOf(
        ".*MyLegacyClass",
    )
}
```
</details>

##### ignoreGeneratedClasses

By default, Modernizer ignores classes annotated with an annotation whose
retention policy is <code>RUNTIME</code> or <code>CLASS</code> and whose simple
name is exactly "Generated".
Individual methods and constructors carrying such an annotation are also ignored.
e.g., members generated by [Lombok](https://projectlombok.org/) when
`lombok.addLombokGeneratedAnnotation = true`.

Note: both
[javax.annotation.Generated](https://docs.oracle.com/javase/8/docs/api/javax/annotation/Generated.html)
and
[javax.annotation.processing.Generated](https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/annotation/processing/Generated.html)
have
[retention policy](https://docs.oracle.com/javase/8/docs/api/index.html?java/lang/annotation/RetentionPolicy.html)
`SOURCE` (aka discarded by compiler).

If you do not want to ignore generated classes, set the
`ignoreGeneratedClasses` property to `false`.

<details>
  <summary>Groovy DSL example</summary>

```groovy
modernizer {
    ignoreGeneratedClasses = false
}
```
</details>

<details>
  <summary>Kotlin DSL example</summary>

```kotlin
modernizer {
    ignoreGeneratedClasses = false
}
```
</details>

### Ignoring elements

You cannot only ignore elements by using extension properties (see above), you
can indicate that Modernizer should ignore violations by adding a
`@SuppressModernizer` annotation to the element you'd like to ignore:

```java
import org.gaul.modernizer_maven_annotations.SuppressModernizer;

@SuppressModernizer
public class Example {
    @SuppressModernizer
    public Example() { /* your code here */ }
  
    @SuppressModernizer
    public static void method() { /* your code here */ }
}
```

Add the following dependency to your Gradle build script if you do not want to
create this annotation yourself:

```groovy
implementation "org.gaul:modernizer-maven-annotations:3.5.0"
```

Modernizer matches the annotation by simple name, so any `@SuppressModernizer`
in any package will suppress violations. The `modernizer-maven-annotations`
dependency above is a convenient canonical copy but is not required.

Fields and packages cannot be suppressed because the annotation's `@Target`
does not include them; use the plugin's `exclusions` or
`ignoreClassNamePatterns` properties instead.

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
| 1.9.x                    | 2.7.0                   |
| 1.10.0                   | 2.9.0                   |
| 1.11.0                   | 3.1.0                   |
| 1.12.0                   | 3.2.0                   |
| 1.13.0                   | 3.3.0                   |
| 1.14.0                   | 3.4.0                   |
| 1.15.0                   | 3.5.0                   |

Note that you can override the default version of Modernizer Maven Plugin which will be used.
Specify the desired version in the `toolVersion` extension property.
Pay attention: This might break when there is an API change!


## Migrating from 1.x to 2.x

There were a few breaking changes between versions 1.x and 2.x to improve
support for [Gradle's configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) and have proper tracking of changed
content.
This improves build speed and leads to fewer mistakes.

The plugin now needs to be configured with [Gradle's properties](https://docs.gradle.org/current/userguide/properties_providers.html).
Most properties simply moved from the raw type to `Property<...>`,
but there were a few exceptions:
* `violationsFile` went from `String` to `Property<TextResource>` 
* `violationsFiles` went from `List<String>` to `ListProperty<TextResource>` 
* `exclusionsFile` went from `String` to `Property<TextResource>` 

Example:
```kotlin
modernizer {
    // old
    // (file content changes were not noticed) 
    violationsFile = "src/main/resources/violations.xml"
    
    // new
    // (file content changes are detected)
    violationsFile = resources.text.fromFile("src/main/resources/violations.xml")
}
```

## FAQ

### I found an undetected case of legacy API. Can you add a new violation rule?

Sounds great! However, I cannot add it myself as
[Modernizer Maven Plugin](https://github.com/gaul/modernizer-maven-plugin) maintains the list of violation rules.
Open an issue there and describe what you found.

Note that it might take some time for them to release a new version with your rule.

### There is a new version of Modernizer Maven Plugin, but there is no corresponding version of your plugin.

That is not a question. Unfortunately, I might not have found the time to release a new version of the Gradle plugin.

In the meanwhile you can specify the desired version in the `toolVersion` extension property.
The Gradle plugin will then pick up the requested version and, if the API is still the same, use it.

```groovy
modernizer {
    toolVersion = "3.5.0"
}
```

### Will you add a feature X?

That depends on whether the feature is specific to Gradle. If it is, then I will see what I can do.
However, if it requires changes in Modernizer Maven Plugin, then it is up to its maintainers.


## References

* [Modernizer Maven Plugin](https://github.com/gaul/modernizer-maven-plugin) 

## License

```
Licensed under the Apache License, Version 2.0
Copyright (C) 2016-2026 Andy Goossens

Inspired by, and based upon code from:

Modernizer Maven Plugin
Copyright (C) 2014-2026 Andrew Gaul

Gradle Docker plugin
Copyright (C) 2014 the original author or authors.
```