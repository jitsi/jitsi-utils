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

package org.jitsi.utils.configk2.examples

import org.jitsi.utils.configk2.exception.ConfigPropertyNotFoundException
import org.jitsi.utils.configk2.exception.ConfigurationValueTypeUnsupportedException
import org.jitsi.utils.configk2.ConfigSource
import java.time.Duration
import kotlin.reflect.KClass

class ExampleConfigSource(
    val props: Map<String, Any>
) : ConfigSource {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T {
        return when(valueType) {
            Int::class -> getterHelper(::getInt)
            Long::class -> getterHelper(::getLong)
            Duration::class -> getterHelper(::getDuration)
            else -> throw ConfigurationValueTypeUnsupportedException.new(valueType)
        }
    }

    private fun getInt(path: String): Int? = props[path] as? Int
    private fun getLong(path: String): Long? = props[path] as? Long
    private fun getDuration(path: String): Duration? = props[path] as? Duration

    @Suppress("UNCHECKED_CAST")
    private fun <U, T : Any> getterHelper(getter: (String) -> U): (String) -> T {
        return { path ->
            getter(path) as? T ?:
                throw ConfigPropertyNotFoundException("Could not find value for property at '$path'")
        }
    }
}

