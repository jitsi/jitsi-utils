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
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A property delegate for retrieving a configuration value of type [T]
 * from a configuration source.
 */
interface PropertyDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Result<T>
}

fun<T> getOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (t: Throwable) {
        null
    }
}

/**
 * A delegate which handles creating the appropriate [ReadStrategy] and
 * invoking it when accessed.
 */
sealed class AbstractPropertyDelegate<T>(propAttributes: ConfigPropertyAttributes, configValueSupplier: () -> T) : PropertyDelegate<T> {
    private val readStrategy: ReadStrategy<T> =
        getReadStrategy(propAttributes.readOnce, configValueSupplier)

    private val result: Result<T>
        get() = readStrategy.get()

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Result<T> = result
}

// Delegate classes for each supported configuration value type

class BooleanPropertyDelegate(propAttributes: ConfigPropertyAttributes, config: Config, path: String) :
        AbstractPropertyDelegate<Boolean>(propAttributes, { config.getBoolean(path)} )

class IntPropertyDelegate(propAttributes: ConfigPropertyAttributes, config: Config, path: String) :
        AbstractPropertyDelegate<Int>(propAttributes, { config.getInt(path)} )

class StringPropertyDelegate(propAttributes: ConfigPropertyAttributes, config: Config, path: String) :
        AbstractPropertyDelegate<String>(propAttributes, { config.getString(path)} )

class DurationPropertyDelegate(propAttributes: ConfigPropertyAttributes, config: Config, path: String) :
        AbstractPropertyDelegate<Duration>(propAttributes, { config.getDuration(path)} )


class AnyPropertyDelegate(propAttributes: ConfigPropertyAttributes, config: Config, path: String) :
        AbstractPropertyDelegate<Any?>(propAttributes, { config.getAny(path)} )

inline fun<reified T : Any> getPropertyDelegate(propAttributes: ConfigPropertyAttributes, config: Config, path: String): PropertyDelegate<T> =
    getPropertyDelegate(T::class, propAttributes, config, path)


@Suppress("UNCHECKED_CAST")
fun<T : Any> getPropertyDelegate(clazz: KClass<T>, propAttributes: ConfigPropertyAttributes, config: Config, path: String): PropertyDelegate<T> {
    return when (clazz) {
        Boolean::class -> BooleanPropertyDelegate(propAttributes, config, path) as PropertyDelegate<T>
        Int::class -> IntPropertyDelegate(propAttributes, config, path) as PropertyDelegate<T>
        String::class -> StringPropertyDelegate(propAttributes, config, path) as PropertyDelegate<T>
        Duration::class -> DurationPropertyDelegate(propAttributes, config, path) as PropertyDelegate<T>
        else -> throw ConfigurationValueTypeUnsupportedException("No supported getter for configuration value type $clazz")
    }
}

/**
 * A wrapper around any existing type property delegate (below) which returns null
 * if the property wasn't found
 */
class OptionalPropertyDelegateWrapper<T>(private val innerDelegate: PropertyDelegate<T>) :
        PropertyDelegate<T?> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Result<T?> {
        return kotlin.runCatching { innerDelegate.getValue(thisRef, property).getOrNull() }
    }
}

@Suppress("UNCHECKED_CAST")
fun<T : Any> getOptionalPropertyDelegate(clazz: KClass<T>, propAttributes: ConfigPropertyAttributes, config: Config, path: String): PropertyDelegate<T> {
    return OptionalPropertyDelegateWrapper(getPropertyDelegate(clazz, propAttributes, config, path)) as PropertyDelegate<T>
}
