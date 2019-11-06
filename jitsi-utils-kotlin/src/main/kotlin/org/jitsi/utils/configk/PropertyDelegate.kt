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

@file:Suppress("unused")

package org.jitsi.utils.configk

import org.jitsi.utils.configk.exception.ConfigurationValueTypeUnsupportedException
import org.jitsi.utils.configk.strategy.ReadStrategy
import org.jitsi.utils.configk.strategy.getReadStrategy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A property delegate for retrieving a configuration value of type [T]
 * from a configuration source.
 */
interface PropertyDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Result<T>
}

/**
 * A delegate which handles creating the appropriate [ReadStrategy] and
 * invoking it when accessed.
 */
sealed class AbstractPropertyDelegate<T>(prop: ConfigProperty, configValueSupplier: () -> T) : PropertyDelegate<T> {
    private val readStrategy: ReadStrategy<T> =
        getReadStrategy(prop.readOnce, configValueSupplier)

    private val result: Result<T>
        get() = readStrategy.get()

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Result<T> = result
}

// Delegate classes for each supported configuration value type

class BooleanPropertyDelegate(prop: ConfigProperty, config: Config, path: String) :
        AbstractPropertyDelegate<Boolean>(prop, { config.getBoolean(path)} ) {
}

class IntPropertyDelegate(prop: ConfigProperty, config: Config, path: String) :
        AbstractPropertyDelegate<Int>(prop, { config.getInt(path)} ) {
}

class StringPropertyDelegate(prop: ConfigProperty, config: Config, path: String) :
        AbstractPropertyDelegate<String>(prop, { config.getString(path)} ) {
}

class AnyPropertyDelegate(prop: ConfigProperty, config: Config, path: String) :
        AbstractPropertyDelegate<Any?>(prop, { config.getAny(path)} ) {
}

inline fun<reified T : Any> getPropertyDelegate(prop: ConfigProperty, config: Config, path: String): PropertyDelegate<T> {
    return getPropertyDelegate(T::class, prop, config, path)
}

@Suppress("UNCHECKED_CAST")
fun<T : Any> getPropertyDelegate(clazz: KClass<T>, prop: ConfigProperty, config: Config, path: String): PropertyDelegate<T> {
    return when (clazz) {
        Boolean::class -> BooleanPropertyDelegate(prop, config, path) as PropertyDelegate<T>
        Int::class -> IntPropertyDelegate(prop, config, path) as PropertyDelegate<T>
        String::class -> StringPropertyDelegate(prop, config, path) as PropertyDelegate<T>
        else -> throw ConfigurationValueTypeUnsupportedException("No supported getter for configuration value type $clazz")
    }
}
