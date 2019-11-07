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
import org.jitsi.utils.configk.strategy.ReadStrategy
import org.jitsi.utils.configk.strategy.getReadStrategy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A property delegate for retrieving a configuration value of type [T]
 * from a configuration source.
 */
interface PropertyDelegate<T : Any> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): ConfigResult<T>

    /**
     * Sometimes we want to be able to wrap this delegate with something else
     * (like to grab a value and then convert it), so expose a way to retrieve
     * the value from somewhere other than a delegate context.
     */
    fun getValue(): ConfigResult<T>

    /**
     * We provide the attributes here for similar reasons as [getValue]:
     * it can be useful to wrap or chain delegates together to perform
     * transformations, and to do so the downstream code needs the
     * properties.
     */
    val attributes: ConfigPropertyAttributes
}

/**
 * A delegate which handles creating the appropriate [ReadStrategy] and
 * invoking it when accessed.
 */
open class PropertyDelegateImpl<T : Any>(
    final override val attributes: ConfigPropertyAttributes,
    configValueSupplier: () -> T
) : PropertyDelegate<T> {

    private val readStrategy: ReadStrategy<T> =
        getReadStrategy(attributes.readOnce, configValueSupplier)

    private val result: ConfigResult<T>
        get() = readStrategy.get()

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): ConfigResult<T> = result

    override fun getValue(): ConfigResult<T> = result
}

inline fun<reified T : Any> getPropertyDelegate(
    propAttributes: ConfigPropertyAttributes,
    config: Any,
    path: String
): PropertyDelegate<T> = getPropertyDelegate(T::class, propAttributes, config, path)

/**
 * Return a [PropertyDelegateImpl] which will use the proper supplier for [T]
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
    return PropertyDelegateImpl(propAttributes, supplier)
}
