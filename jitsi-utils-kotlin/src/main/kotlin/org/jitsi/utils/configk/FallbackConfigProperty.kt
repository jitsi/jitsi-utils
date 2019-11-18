/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.utils.configk

import org.jitsi.utils.configk.exception.NoAcceptablePropertyInstanceFoundException

/**
 * A property which checks multiple sources in priority order for its value
 */
//TODO: we need to store the retrievers here instead of the results, as storing
// the results won't work correctly with read-every-time properties.
abstract class FallbackConfigProperty<T : Any> : ConfigProperty<T> {
    protected abstract val propertyPriority: List<ConfigResult<T>>

    final override val value: T
        get() {
            return propertyPriority.asSequence()
                .filterIsInstance<ConfigResult.PropertyFound<T>>()
                .map { it.value }
                .firstOrNull() ?: run {
                    // We were unable to parse the property's value in any
                    // of the places we looked, so go through and gather each
                    // the error at each source to give a more complete picture
                    // for debugging
                    val exceptions = propertyPriority.asSequence()
                            .filterIsInstance<ConfigResult.PropertyNotFound<*>>()
                            .map { it.exception }
                            .toList()
                    throw NoAcceptablePropertyInstanceFoundException(exceptions)
                }
        }
}
