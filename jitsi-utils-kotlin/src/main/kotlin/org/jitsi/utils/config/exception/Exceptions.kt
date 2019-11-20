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

package org.jitsi.utils.config.exception

import kotlin.reflect.KClass

/**
 * Prints only the exception's 'simple' name, not the fully qualified
 * class name (which makes the error harder to decipher and doesn't
 * add much in the case of config exceptions)
 */
open class SimpleToStringException(
    message: String
) : Exception(message) {
    override fun toString(): String = "${this.javaClass.simpleName}: $message"
}

/**
 * Used for when the configuration source doesn't support parsing the requested
 * type
 */
class ConfigurationValueTypeUnsupportedException private constructor(
    message: String
) : SimpleToStringException(message) {
    companion object {
        // We can't have a generic type on the class constructor, so use a helper here
        // to create the log message
        fun <T : Any> new(valueType: KClass<T>): ConfigurationValueTypeUnsupportedException =
            ConfigurationValueTypeUnsupportedException("No getter found for value of type $valueType")
    }
}

/**
 * Used for when a single config source can't find an instance
 * of a property
 */
class ConfigPropertyNotFoundException(
    message: String
) : SimpleToStringException(message)

class ConfigValueParsingException(
    message: String
) : SimpleToStringException(message)

/**
 * Used when no instance of a property can be found across multiple
 * config sources
 */
class NoAcceptablePropertyInstanceFoundException(
    val exceptions: List<Throwable>
) : SimpleToStringException("Unable to find or parse configuration property due to: ${exceptions}")
