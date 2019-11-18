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

package org.jitsi.utils.configk2

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

    val configSource: ConfigSource
)

open class ConfigPropertyAttributesBuilder(
    protected var keyPath: String? = null,
    protected var readOnce: Boolean? = null,
    protected var configSource: ConfigSource? = null
) {
    open fun readOnce(): ConfigPropertyAttributesBuilder {
        readOnce = true
        return this
    }

    open fun readEveryTime(): ConfigPropertyAttributesBuilder {
        readOnce = false
        return this
    }

    open fun name(propName: String): ConfigPropertyAttributesBuilder {
        keyPath = propName
        return this
    }

    open fun fromConfig(configSource: ConfigSource): ConfigPropertyAttributesBuilder {
        this.configSource = configSource
        return this
    }

    fun <T : Any> withType(valueType: KClass<T>): TypedConfigPropertyAttributesBuilder<T> {
        return TypedConfigPropertyAttributesBuilder(valueType, keyPath, readOnce)
    }

    open fun build(): ConfigPropertyAttributes<*> {
        throw Exception("No prop value type set!")
    }
}

class TypedConfigPropertyAttributesBuilder<T : Any>(
    private val valueType: KClass<T>,
    keyPath: String? = null,
    readOnce: Boolean? = null
) : ConfigPropertyAttributesBuilder(keyPath, readOnce) {
    override fun readOnce(): TypedConfigPropertyAttributesBuilder<T> {
        super.readOnce()
        return this
    }

    override fun readEveryTime(): ConfigPropertyAttributesBuilder {
        super.readEveryTime()
        return this
    }

    override fun name(propName: String): TypedConfigPropertyAttributesBuilder<T> {
        super.name(propName)
        return this
    }

    override fun fromConfig(configSource: ConfigSource): TypedConfigPropertyAttributesBuilder<T> {
        super.fromConfig(configSource)
        return this
    }

    override fun build(): ConfigPropertyAttributes<T> =
        ConfigPropertyAttributes<T>(keyPath!!, valueType, readOnce!!, configSource!!)
}

