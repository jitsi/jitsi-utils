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

import org.jitsi.utils.configk.exception.NoAcceptablePropertyInstanceFoundException
import org.jitsi.utils.configk.spi.TypedConfigValueGetterService
import org.jitsi.utils.configk.strategy.ReadFrequencyStrategy
import org.jitsi.utils.configk.strategy.getReadStrategy
import kotlin.reflect.KClass

data class PropertyInfo<T : Any>(
    val valueType: KClass<T>,
    val configKey: String,
    val attrs: ConfigPropertyAttributes
)

fun <T : Any> PropertyInfo<T>.fromSource(configSource: Any): PropertyInfoSource<T> =
    PropertyInfoSource(configSource, this)

data class PropertyInfoSource<T : Any>(
    val configSource: Any,
    val propertyInfo: PropertyInfo<T>
)

interface PropertyValueRetriever<T : Any> {
    val result: ConfigResult<T>
}

class Retriever<T : Any>(
    val propertyInfoSource: PropertyInfoSource<T>
) : PropertyValueRetriever<T>, ConfigProperty<T> {
    private val readFrequencyStrategy: ReadFrequencyStrategy<T>

    init {
        val typedGetter
            = TypedConfigValueGetterService.getterFor(propertyInfoSource.propertyInfo.valueType)
        val supplier = { typedGetter(propertyInfoSource.configSource, propertyInfoSource.propertyInfo.configKey) }
        readFrequencyStrategy =
            getReadStrategy(propertyInfoSource.propertyInfo.attrs.readOnce, supplier)

    }

    override val result: ConfigResult<T>
        get() = readFrequencyStrategy.get()

    override val value: T
        get() = result.getOrThrow()
}

class MultiRetriever<T : Any>(
    vararg val retrievers: Retriever<T>
) : PropertyValueRetriever<T>, ConfigProperty<T> {

    /**
     * Go through the list of retrievers and return the first
     * found config value, or, a [NoAcceptablePropertyInstanceFoundException]
     * containing the exceptions from each checked retriever if none
     * of them successfully retrieved a result
     */
    private fun findResultOrAggregateExceptions(vararg retrievers: Retriever<T>): ConfigResult<T> {
        val exceptions = mutableListOf<Throwable>()
        for (retriever in retrievers) {
            when (val result = retriever.result) {
                is ConfigResult.PropertyNotFound -> exceptions.add(result.exception)
                is ConfigResult.PropertyFound -> return result
            }
        }
        return ConfigResult.notFound(NoAcceptablePropertyInstanceFoundException(exceptions))
    }

    /**
     * Iterate over a set of [Retriever]s and find
     */
    class MultiRetrieversResult<T : Any>(private vararg val retrievers: Retriever<T>) {
        val failures: List<Throwable> = retrievers
                .map { it.result }
                .filterIsInstance<ConfigResult.PropertyNotFound<*>>()
                .map { it.exception }

        val hasFailure = failures.isNotEmpty()

        fun getOrThrow(): T {
            if (hasFailure) {
                throw NoAcceptablePropertyInstanceFoundException(failures)
            }
            return value
        }

        val result: ConfigResult<T>
            get() {
                return configRunCatching {
                    if (hasFailure) {
                        throw NoAcceptablePropertyInstanceFoundException(failures)
                    }
                    value
                }
            }

        val value: T
            get() = retrievers
                    .asSequence()
                    .map { it.result }
                    .filterIsInstance<ConfigResult.PropertyFound<T>>()
                    .map { it.value }
                    .first()
    }

    override val result: ConfigResult<T>
        get() = findResultOrAggregateExceptions(*retrievers)

    override val value: T
        get() = findResultOrAggregateExceptions().getOrThrow()
}