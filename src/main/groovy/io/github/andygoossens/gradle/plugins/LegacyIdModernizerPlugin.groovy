/*
 * Copyright 2026 Andy Goossens
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