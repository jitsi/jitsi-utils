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

import kotlin.reflect.KClass

/**
 * A [ConfigSource] is what is used to retrieve configuration values
 * from some location.
 */
interface ConfigSource {
    /**
     * Given a [valueType], return a function which takes in a
     * configuration property key (aka a key 'name') and returns the value
     * of the property at the given name as type [T]
     */
    fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T

    /**
     * A name for this [ConfigSource] to give extra context in the
     * event of errors
     */
    val name: String

    /**
     * Tell this [ConfigSource] to reload its config from wherever it
     * was obtained
     */
    fun reload()

    /**
     * Dump the contents of this [ConfigSource], masking any sensitive
     * fields
     */
    fun toStringMasked(): String
}
