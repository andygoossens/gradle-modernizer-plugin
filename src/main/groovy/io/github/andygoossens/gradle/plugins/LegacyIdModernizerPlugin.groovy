package io.github.andygoossens.gradle.plugins

import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Applied only via a deprecated plugin id. Warns the user and
 * delegates to {@link ModernizerPlugin} for actual behavior, so both ids
 * stay functionally identical while only this one nags about migrating.
 */
@CompileStatic
class LegacyIdModernizerPlugin extends ModernizerPlugin {

    @Override
    void apply(Project project) {
        project.logger.warn(
            "Plugin id 'com.github.andygoossens.gradle-modernizer-plugin' is deprecated " +
                "and will be removed in a future release. Please switch to " +
                "'io.github.andygoossens.modernizer' instead."
        )

        super.apply(project)
    }
}