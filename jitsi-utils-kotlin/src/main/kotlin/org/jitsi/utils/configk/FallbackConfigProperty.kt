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

import org.jitsi.utils.configk.exception.ConfigPropertyNotFoundException

/**
 * A property which checks multiple sources in priority order for its value
 */
abstract class FallbackProperty<T : Any> : ConfigProperty<T> {
    protected abstract val propertyPriority: List<ConfigResult<T>>

    final override val value: T
        get() = propertyPriority.asSequence()
                .filter { it is ConfigResult.PropertyFound }
                .map { (it as ConfigResult.PropertyFound).value }
                .firstOrNull() ?: throw ConfigPropertyNotFoundException("No configuration property found for ${javaClass}}")
}
