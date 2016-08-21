# Gradle Modernizer Plugin

Gradle Modernizer Plugin detects uses of legacy APIs which modern Java versions
supersede.

These modern APIs are often more performant, safer, and idiomatic than the
legacy equivalents.
For example, Modernizer can detect uses of `Vector` instead of `ArrayList`,
`String.getBytes(String)` instead of `String.getBytes(Charset)`, and
Guava `Objects.equal` instead of Java 7 `Objects.equals`.
The default configuration detects
[over 100 legacy APIs](https://github.com/andrewgaul/modernizer-maven-plugin/blob/master/src/main/resources/modernizer.xml),
including third-party libraries like
[Guava](https://code.google.com/p/guava-libraries/).

This Gradle plugin is actually a wrapper around
[Modernizer Maven Plugin](https://github.com/andrewgaul/modernizer-maven-plugin/)
so that the same functionality is now available in Gradle builds.

## Usage

To use the plugin, include in your build script:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'com.github.andygoossens:gradle-modernizer-plugin:0.0.1'
    }
}

apply plugin: 'com.github.andygoossens.gradle-modernizer-plugin'

repositories {
    mavenCentral()
}
```

If you want to call the `modernizer` task directly:
```bash
./gradlew modernizer
```

But most of the time you don't need to call the `modernizer` task yourself.
The task knows its place inside the build lifecycle and it will execute
whenever it is deemed necessary.

### Extension properties

| Property name    | Type   | Default value                | Description                                                     |
|------------------|--------|------------------------------|-----------------------------------------------------------------|
|toolVersion       |String  |See table below               |Version of modernizer-maven-plugin that will be used.            |
|javaVersion       |String  |${project.targetCompatibility}|Target Java version. Decides which violations will apply.        |
|failOnViolations  |boolean |false                         |Fail build when a violation has been detected.                   |
|includeTestClasses|boolean |false                         |Whether test classes will be searched for violations.            |
|violationsFile    |String  |null                          |User-specified violation file.                                   |
|exclusionsFile    |String  |null                          |Disables user-specified violations. See format description below.|
|exclusions        |String[]|[]                            |Violations to disable. See format description below.             |
|ignorePackages    |String[]|[]                            |Package prefixes to ignore. See format description below.        |
|skip              |boolean |false                         |Whether task should be skipped.                                  |

#### Usage example

```groovy
modernizer {
    failOnViolations = true
    includeTestClasses = true
}
```

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

## Version comparison

Gradle Modernizer Plugin is basically a wrapper around Modernizer Maven Plugin.
The table below describes how they relate to each other.

| Gradle Modernizer Plugin | Modernizer Maven Plugin |
|--------------------------|-------------------------|
| 1.0.0                    | 1.4.0                   |

Note that you can override the default version of Modernizer Maven Plugin which will be used.
Specify in the `toolVersion` extension property the version that you want to use. But pay attention:
This might break when there is an API change!


## FAQ

### I found an undetected case of legacy API. Can you add a new violation rule?

Sounds great! However I cannot add it myself as the list of violations rules is maintained in
[Modernizer Maven Plugin](https://github.com/andrewgaul/modernizer-maven-plugin).
Open an issue there and describe what you found.

Note that it might take some time for them to release a new version with your rule.

### There is a new version of Modernizer Maven Plugin but there is no corresponding version of your plugin.

That is not a question. Unfortunately, I might not have found the time to release a new version of the Gradle plugin.

In the meanwhile you can specify the desired version in the `toolVersion` extension property.
The Gradle plugin will then pickup the requested version and, if the API is still the same, use it.

### Will you add feature X?

That depends on whether the feature is specific to Gradle. If it is, then I will see what I can do.
However if it requires changes in Modernizer Maven Plugin, then it is up to its maintainers.


## References

* [Modernizer Maven Plugin](https://github.com/andrewgaul/modernizer-maven-plugin) 

## License

```
Licensed under the Apache License, Version 2.0
Copyright (C) 2016 Andy Goossens

Inspired by, and based upon code from:

Modernizer Maven Plugin
Copyright (C) 2014-2015 Andrew Gaul

Gradle Docker plugin
Copyright (C) 2014 the original author or authors.
```