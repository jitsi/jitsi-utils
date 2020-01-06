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

package org.jitsi.utils.config

import org.jitsi.utils.config.exception.NoAcceptablePropertyInstanceFoundException
import org.jitsi.utils.config.strategy.getReadStrategy

/**
 * A config property for which we search multiple [ConfigSource]s, in order,
 * to find the value
 */
abstract class FallbackProperty <T : Any>(
    vararg val attrs: ConfigPropertyAttributes<T>
) : ConfigProperty<T> {
    protected val getters =
        attrs.map { getReadStrategy(it.readOnce, it.supplier) }

    override val value: T
        get() {
            val exceptions = mutableListOf<Throwable>()
            for (getter in getters) {
                when (val result = getter.get()) {
                    is ConfigResult.PropertyNotFound -> exceptions.add(result.exception)
                    is ConfigResult.PropertyFound -> return result.value
                }
            }
            throw NoAcceptablePropertyInstanceFoundException(exceptions)
        }
}
