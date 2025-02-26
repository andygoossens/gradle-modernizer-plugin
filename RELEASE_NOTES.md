### Next version

### Version 1.11.0 (January 19, 2025)

* Support for Modernizer Maven Plugin 3.1.0 and its new API

### Version 1.10.0 (November 4, 2024)

* Support Modernizer Maven Plugin 2.9.0

### Version 1.9.3 (July 16, 2024)

* Support Gradle's configuration cache

### Version 1.9.2 (February 26, 2024)

* Lookup 'check' task lazily, fixes 'Project.afterEvaluate(Closure)' issue

### Version 1.9.1 (February 24, 2024)

* Get rid of Gradle's deprecation warnings
* Build plugin with Gradle 7.6.4

### Version 1.9.0 (September 30, 2023)

* Update Gradle plugin publishing configuration
* Support Modernizer Maven Plugin 2.7.0

### Version 1.8.0 (April 13, 2023)

* Support Modernizer Maven Plugin 2.6.0
* Build plugin with Gradle 7.6.1

### Version 1.7.0 (December 4, 2022)

* Support Modernizer Maven Plugin 2.5.0

### Version 1.6.2 (January 15, 2022)

* Exclude "plexus-utils" dependency (coming from upstream) to avoid a CVE
  warning. #1

### Version 1.6.1 (September 28, 2021)

* Support Gradle setting `RepositoriesMode.FAIL_ON_PROJECT_REPOS`
* Introduce cleaner plugin id `com.github.andygoossens.modernizer` but
  `com.github.andygoossens.gradle-modernizer-plugin` will keep on working for
  the foreseeable future.

### Version 1.6.0 (August 19, 2021)

* Support Modernizer Maven Plugin 2.3.0

### Version 1.5.0 (July 17, 2021)

* Support Modernizer Maven Plugin 2.2.0
* Build plugin with Gradle 7.1.1
* Simplified Gradle's publishing configuration

### Version 1.4.0 (August 2, 2020)

* Support Modernizer Maven Plugin 2.1.0
* Build plugin with Gradle 6.5.1

### Version 1.3.0 (November 30, 2019)

* Support Modernizer Maven Plugin 2.0.0
* Build plugin with Gradle 5.6.4
* Ignore classes annotated with annotations that contain "Generated" in their
  name

### Version 1.2.0 (March 4, 2019)

* Support Modernizer Maven Plugin 1.8.0
* Ignore classes annotated with @SuppressModernizer
* Try to find the source file across source sets

### Version 1.1.2 (March 2, 2019)

* Fix issue that prevented the plugin to be used with Gradle 5
* Build plugin with Gradle 5.2.1

### Version 1.1.1 (September 10, 2018)

* Fix usage when appearing in a plugins {} block

### Version 1.1.0 (September 9, 2018)

* Publishing to https://plugins.gradle.org/

### Version 1.0.0 (September 9, 2018)

* Initial release.
