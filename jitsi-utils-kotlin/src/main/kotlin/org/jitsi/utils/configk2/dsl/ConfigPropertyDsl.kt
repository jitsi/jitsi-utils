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
import org.jitsi.utils.configk2.ConfigPropertyAttributes
import org.jitsi.utils.configk2.ConfigPropertyAttributesBuilder
import org.jitsi.utils.configk2.ConfigRetriever
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
                val retriever = ConfigRetriever(attrs)
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
            val innerRetriever = ConfigRetriever(innerAttrs)

            return object : ConfigProperty<ActualType> {
                override val value: ActualType
                    get() = converter!!(innerRetriever.retrieve())
            }

        }
    }
}

inline fun <reified T : Any> property(block: ConfigPropertyBuilder<T>.() -> Unit): ConfigProperty<T> {
    val x = ConfigPropertyBuilder(T::class).also(block)
    return x.build()
}
