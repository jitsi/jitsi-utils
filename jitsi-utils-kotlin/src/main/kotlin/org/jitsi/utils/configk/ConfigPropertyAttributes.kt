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

import kotlin.reflect.KClass

data class ConfigPropertyAttributes<T : Any>(
    /**
     * The 'path' at which this property lives (also known as the
     * property's 'name')
     */
    val keyPath: String,
    /**
     * The type of this property's value
     */
    val valueType: KClass<T>,
    /**
     * Whether or not the configuration property's value should be read
     * only a single time and cached (i.e. runtime changes to this
     * value will *not* be seen by the running code) or re-read from
     * the configuration source every time its accessed.
     */
    val readOnce: Boolean,
    /**
     * The [ConfigSource] instance from which this property will
     * be read
     */
    val configSource: ConfigSource,

    val deprecationNotice: DeprecationNotice? = null
) {
    val isDeprecated: Boolean = deprecationNotice != null
}

data class DeprecationNotice(val message: String)

/**
* A helper method to define a supplier function for [ConfigPropertyAttributes]
*/
val <T : Any> ConfigPropertyAttributes<T>.supplier: () -> T
    get() = { configSource.getterFor(valueType).invoke(keyPath) }

interface ConfigPropertyAttributesBuilder<T : Any> {
    fun readOnce()
    fun readEveryTime()
    fun name(propName: String)
    fun fromConfig(configSource: ConfigSource)
    fun deprecated(message: String)
    fun build(): ConfigPropertyAttributes<T>
}

class ConfigPropertyAttributesBuilderImpl<T : Any>(
    private val valueType: KClass<T>,
    private var keyPath: String? = null,
    private var readOnce: Boolean? = null,
    private var configSource: ConfigSource? = null,
    private var deprecationNotice: DeprecationNotice? = null
) : ConfigPropertyAttributesBuilder<T> {

    override fun readOnce() { readOnce = true }
    override fun readEveryTime() { readOnce = false }
    override fun name(propName: String) { keyPath = propName }
    override fun fromConfig(configSource: ConfigSource) { this.configSource = configSource }
    override fun deprecated(message: String) { this.deprecationNotice = DeprecationNotice(message) }
    override fun build(): ConfigPropertyAttributes<T> {
        val keyPath: String = keyPath ?: throw Exception("Property name not set")
        val readOnce: Boolean = readOnce ?: throw Exception("Read frequency not set")
        val configSource: ConfigSource = configSource ?: throw Exception("Config source not set")
        return ConfigPropertyAttributes(keyPath, valueType, readOnce, configSource, deprecationNotice)
    }
}
