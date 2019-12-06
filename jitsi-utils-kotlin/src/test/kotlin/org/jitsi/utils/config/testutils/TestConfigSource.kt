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

package org.jitsi.utils.config.testutils

import org.jitsi.utils.config.ConfigSource
import org.jitsi.utils.config.exception.ConfigPropertyNotFoundException
import org.jitsi.utils.config.exception.ConfigurationValueTypeUnsupportedException
import java.time.Duration
import kotlin.reflect.KClass

class TestConfigSource(
    override val name: String,
    props: Map<String, Any>
) : ConfigSource {
    val props = props.toMutableMap()
    var numGetsCalled = 0
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T {
        return when(valueType) {
            Int::class -> getterHelper(::getInt)
            Long::class -> getterHelper(::getLong)
            Duration::class -> getterHelper(::getDuration)
            else -> throw ConfigurationValueTypeUnsupportedException.new(valueType)
        }
    }

    override fun reload() { /* No op */ }

    override fun toStringMasked(): String = props.toString()

    private fun getInt(path: String): Int? = props[path] as? Int
    private fun getLong(path: String): Long? = props[path] as? Long
    private fun getDuration(path: String): Duration? = props[path] as? Duration

    @Suppress("UNCHECKED_CAST")
    private fun <U, T : Any> getterHelper(getter: (String) -> U): (String) -> T {
        return { path ->
            numGetsCalled++
            getter(path) as? T ?:
            throw ConfigPropertyNotFoundException("Could not find value for property at '$path'")
        }
    }
}
