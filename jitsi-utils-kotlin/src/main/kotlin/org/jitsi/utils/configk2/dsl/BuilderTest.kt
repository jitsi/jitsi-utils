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
import org.jitsi.utils.configk2.ConfigRetriever
import org.jitsi.utils.configk2.ConfigSource
import org.jitsi.utils.configk2.TypedConfigPropertyAttributesBuilder
import java.time.Duration
import kotlin.reflect.KClass

class TypedFoo<T : Any>(
    type: KClass<T>
) {
    var attributesBuilder = TypedConfigPropertyAttributesBuilder<T>(type)
    var innerRetriever: RetrievedType<*, T>? = null

    fun name(name: String) {
        attributesBuilder.name(name)
    }

    fun fromConfig(configSource: ConfigSource) {
        attributesBuilder.fromConfig(configSource)
    }

    fun readOnce() {
        attributesBuilder.readOnce()
    }

    inline fun <reified U : Any> retrievedAs(block: RetrievedType<U, T>.() -> Unit) {
        innerRetriever = RetrievedType<U, T>(U::class).apply(block)
    }


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

    class RetrievedType<RetrieveType : Any, ActualType : Any>(
        val retrieveType: KClass<RetrieveType>
    ) {
        var converter: ((RetrieveType) -> ActualType)? = null

        fun convertedBy(converter: (RetrieveType) -> ActualType) {
            this.converter = converter
        }

        fun build(attrs: ConfigPropertyAttributes<ActualType>) : ConfigProperty<ActualType> {
            // First make a retriever that retrieves it as RetrieveType
            val innerAttrs = ConfigPropertyAttributes(attrs.keyPath, retrieveType, attrs.readOnce, attrs.configSource)
            val innerRetriever = ConfigRetriever(innerAttrs)

            return object : ConfigProperty<ActualType> {
                override val value: ActualType
                    get() = converter!!(innerRetriever.retrieve())
            }

        }
    }
}

inline fun <reified T : Any> property2(block: TypedFoo<T>.() -> Unit): ConfigProperty<T> {
    val x = TypedFoo(T::class)
    x.block()
    return x.build()
}

class DummyConfig : ConfigSource {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getterFor(valueType: KClass<T>): (ConfigSource, String) -> T {
        return when(valueType) {
            Int::class -> ({ config, path -> (config as DummyConfig).getInt(path) as T })
            Duration::class -> ({ config, path -> (config as DummyConfig).getDuration(path) as T })
            else -> TODO()
        }
    }

    fun getInt(path: String): Int = 42
    fun getDuration(path: String): Duration = Duration.ofSeconds(10)
}

fun main() {
    val x = property2<Long> {
        name("name")
        readOnce()
        fromConfig(DummyConfig())
        retrievedAs<Duration> {
            convertedBy { it.toMillis() }
        }
    }

    val y = property2<Int> {
        name("name")
        readOnce()
        fromConfig(DummyConfig())
    }

    println("x = ${x.value}")
    println("y = ${y.value}")
}

// val x = property<Long> {
//   name("name")
//   readOnce()
//   fromConfig(newConfig())
//   retrievedAs<Duration> {
//     convertedBy { it.toMillis }
//   }
//}