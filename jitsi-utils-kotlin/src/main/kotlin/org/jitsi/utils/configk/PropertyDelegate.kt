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

import org.jitsi.utils.configk.spi.TypedConfigValueGetterService
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A property delegate wrapper around a [PropValueRetriever]
 */
class PropertyDelegate<T : Any>(
    attributes: ConfigPropertyAttributes,
    configValueSupplier: () -> T
) {
    private val retriever = PropValueRetriever(attributes, configValueSupplier)

    val attributes: ConfigPropertyAttributes
        get() = retriever.attributes

    operator fun getValue(thisRef: Any?, property: KProperty<*>): ConfigResult<T> = retriever.result

    fun getValue(): ConfigResult<T> = retriever.result
}

/**
 * A helper to pass a KClass automatically to [getPropertyDelegate] below
 */
inline fun<reified T : Any> getPropertyDelegate(
    propAttributes: ConfigPropertyAttributes,
    config: Any,
    path: String
): PropertyDelegate<T> = getPropertyDelegate(T::class, propAttributes, config, path)

/**
 * Return a [PropertyDelegate] which will use the proper supplier for [T]
 */
@Suppress("UNCHECKED_CAST")
fun<T : Any> getPropertyDelegate(
    clazz: KClass<T>,
    propAttributes: ConfigPropertyAttributes,
    config: Any,
    path: String
): PropertyDelegate<T> {
    val typedGetter = TypedConfigValueGetterService.getterFor(clazz)
    val supplier = { typedGetter(config, path) }
    return PropertyDelegate(propAttributes, supplier)
}
