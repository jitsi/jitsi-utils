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

package org.jitsi.utils.config.examples

import org.jitsi.utils.config.exception.ConfigPropertyNotFoundException
import org.jitsi.utils.config.exception.ConfigurationValueTypeUnsupportedException
import org.jitsi.utils.config.ConfigSource
import org.jitsi.utils.config.exception.ConfigValueParsingException
import java.time.Duration
import kotlin.reflect.KClass

/**
 * An example [ConfigSource].  This [ConfigSource] just reads from
 * a given map which contains property key names mapped to their
 * already-typed values.
 */
class ExampleConfigSource(
    override val name: String,
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

    private fun getInt(path: String): Int = getValueHelper(path, Int::class) { it as? Int }
    private fun getLong(path: String): Long = getValueHelper(path, Long::class) { it as? Long }
    private fun getDuration(path: String): Duration? = getValueHelper(path, Duration::class) { it as? Duration }

    /**
     * There are 2 possible things that can happen:
     * 1) The property wasn't found at all
     * 2) The property was found but we can't cast/parse it to the requested type
     *
     * getValueHelper include some code to detect both of those situations and give
     * more useful exception messages
     */
    private fun <T : Any> getValueHelper(path: String, desiredType: KClass<T>, typeCast: (Any) -> T?): T {
        val result = props.getOrElse(path) {
            throw ConfigPropertyNotFoundException("Could not find value for property at '$path' " +
                    "in config $name")
        }
        return typeCast(result) ?: throw ConfigValueParsingException("Value '$result' " +
                "(type ${result.javaClass.simpleName}) at path '$path' in config $name " +
                "could not be cast to ${desiredType.simpleName}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <U, T : Any> getterHelper(getter: (String) -> U): (String) -> T {
        return { path ->
            getter(path) as? T ?:
                throw ConfigPropertyNotFoundException("Could not find value for property at '$path'")
        }
    }
}

