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

package org.jitsi.utils.configk2.dsl

import org.jitsi.utils.configk.ConfigProperty
import org.jitsi.utils.configk.ConfigResult
import org.jitsi.utils.configk.configRunCatching
import org.jitsi.utils.configk.exception.NoAcceptablePropertyInstanceFoundException
import org.jitsi.utils.configk.getOrThrow
import org.jitsi.utils.configk2.ConfigPropertyAttributes
import org.jitsi.utils.configk2.ConfigPropertyAttributesBuilder
import org.jitsi.utils.configk2.ConfigSource
import kotlin.reflect.KClass

class ConfigPropertyBuilder<T : Any>(
    type: KClass<T>
) {
    var attributesBuilder = ConfigPropertyAttributesBuilder<T>(type)
    var innerRetriever: RetrievedTypeHelper<*, T>? = null

    fun name(name: String) {
        attributesBuilder.name(name)
    }

    fun fromConfig(configSource: ConfigSource) {
        attributesBuilder.fromConfig(configSource)
    }

    fun readOnce() {
        attributesBuilder.readOnce()
    }

    inline fun <reified U : Any> retrievedAs(): RetrievedTypeHelper<U, T> =
        RetrievedTypeHelper<U, T>(U::class).also { innerRetriever = it }

    fun build(): ConfigProperty<T> {
        val attrs = attributesBuilder.build()
        return innerRetriever?.build(attributesBuilder.build()) ?: run {
            object : ConfigProperty<T> {
                val retriever = org.jitsi.utils.configk2.Retriever(attrs)
                override val value: T
                    get() = retriever.retrieve()
            }
        }
    }

    class RetrievedTypeHelper<RetrievedType : Any, ActualType : Any>(
        val retrieveType: KClass<RetrievedType>
    ) {
        var converter: ((RetrievedType) -> ActualType)? = null

        infix fun convertedBy(converter: (RetrievedType) -> ActualType) {
            this.converter = converter
        }

        fun build(attrs: ConfigPropertyAttributes<ActualType>) : ConfigProperty<ActualType> {
            // First make a retriever that retrieves it as RetrievedType
            val innerAttrs = ConfigPropertyAttributes(attrs.keyPath, retrieveType, attrs.readOnce, attrs.configSource)
            val innerRetriever = org.jitsi.utils.configk2.Retriever(innerAttrs)

            return object : ConfigProperty<ActualType> {
                override val value: ActualType
                    get() = converter!!(innerRetriever.retrieve())
            }

        }
    }
}

class MultiConfigPropertyBuilder<T : Any>(val type: KClass<T>) {
    val innerProperties = mutableListOf<ConfigProperty<T>>()

    fun property(block: ConfigPropertyBuilder<T>.() -> Unit) {
        innerProperties.add(ConfigPropertyBuilder<T>(type).apply(block).build())
    }

    private fun findResultOrAggregateExceptions(configProperties: Iterable<ConfigProperty<T>>): ConfigResult<T> {
        val exceptions = mutableListOf<Throwable>()
        for (prop in configProperties) {
            when (val result = configRunCatching { prop.value }) {
                is ConfigResult.PropertyNotFound -> exceptions.add(result.exception)
                is ConfigResult.PropertyFound -> return result
            }
        }
        return ConfigResult.notFound(NoAcceptablePropertyInstanceFoundException(exceptions))
    }

    fun build(): ConfigProperty<T> {
        return object : ConfigProperty<T> {
            override val value: T
                get() = findResultOrAggregateExceptions(innerProperties).getOrThrow()

        }
    }
}

inline fun <reified T : Any> property(block: ConfigPropertyBuilder<T>.() -> Unit): ConfigProperty<T> =
    ConfigPropertyBuilder(T::class).also(block).build()

inline fun <reified T : Any> multiProperty(block: MultiConfigPropertyBuilder<T>.() -> Unit): ConfigProperty<T> =
    MultiConfigPropertyBuilder(T::class).also(block).build()
