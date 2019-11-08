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

import org.jitsi.utils.configk.spi.TypedConfigValueGetterService
import org.jitsi.utils.configk.strategy.ReadFrequencyStrategy
import org.jitsi.utils.configk.strategy.getReadStrategy
import kotlin.reflect.KClass

/**
 * A retriever retrieves the value of a property
 * via a [ReadFrequencyStrategy] derived from the given
 * [ConfigPropertyAttributes].  Note that we don't know that
 * this retrieve will successfully find the property, so
 * the result is modeled as a [ConfigResult], which may
 * hold a found value or an exception.
 *
 * NOTE: PropValueRetriever leverages a supplier instead of
 * taking in the type, source and key directly because then
 * we can easily chain a 'transforming' retriever to a prior
 * one, which doesn't retrieve the value from some config
 * source but instead transforms a value retrieved from a
 * config source.  A static factory method is provided to
 * help with the case where the value is coming from a
 * config source.
 */
class PropValueRetriever<T : Any>(
    val attributes: ConfigPropertyAttributes,
    configValueSupplier: () -> T
) {
    private val readFrequencyStrategy: ReadFrequencyStrategy<T> =
        getReadStrategy(attributes.readOnce, configValueSupplier)

    val result: ConfigResult<T>
        get() = readFrequencyStrategy.get()

    companion object {
        fun <T : Any>fromConfig(
            propValueType: KClass<T>,
            attributes: ConfigPropertyAttributes,
            configKey: String,
            configSource: Any
        ): PropValueRetriever<T> {
            val typedGetter = TypedConfigValueGetterService.getterFor(propValueType)
            val supplier = { typedGetter(configSource, configKey) }
            return PropValueRetriever<T>(attributes, supplier)
        }
    }
}

fun <T : Any> fromConfig(
    propValueType: KClass<T>,
    attributes: ConfigPropertyAttributes,
    configKey: String,
    configSource: Any
) : PropValueRetriever<T> = PropValueRetriever.fromConfig(propValueType, attributes, configKey, configSource)
