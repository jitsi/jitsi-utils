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

package org.jitsi.utils.configk.dsl

import org.jitsi.utils.configk.ConfigPropertyImpl
import org.jitsi.utils.configk.ConfigProperty
import org.jitsi.utils.configk.exception.NoAcceptablePropertyInstanceFoundException
import org.jitsi.utils.configk.ConfigPropertyAttributes
import org.jitsi.utils.configk.ConfigPropertyAttributesBuilder
import org.jitsi.utils.configk.ConfigPropertyAttributesBuilderImpl
import org.jitsi.utils.configk.ConfigResult
import org.jitsi.utils.configk.ConfigSource
import org.jitsi.utils.configk.configRunCatching
import org.jitsi.utils.configk.getOrThrow
import org.jitsi.utils.configk.supplier
import kotlin.reflect.KClass

/**
 * A helper class used to build an instance of
 * [ConfigProperty].
 */
class ConfigPropertyBuilder<T : Any>(
    type: KClass<T>
) : ConfigPropertyAttributesBuilder<T> by ConfigPropertyAttributesBuilderImpl(type){
    /**
     * If set, when building the [ConfigProperty] we'll use this
     * helper to retrieve the value as a type other than [T]
     * and then convert it to an instance of [T]
     */
    var innerRetriever: RetrievedTypeHelper<*, T>? = null

    inline fun <reified U : Any> retrievedAs(): RetrievedTypeHelper<U, T> =
        RetrievedTypeHelper<U, T>(U::class).also { innerRetriever = it }

    fun buildProp(): ConfigProperty<T> {
        val attrs = this.build()
        return innerRetriever?.build(attrs) ?: run { ConfigPropertyImpl(attrs) }
    }

    /**
     * A helper class used when a configuration value is retrieved as a
     * different type than the eventual value type.  For example,
     * this allows creating a [ConfigProperty] with a value
     * type of [Int], but retrieving it from the [ConfigSource] as
     * another type (say, [java.time.Duration] and then converting it to [Long])
     */
    class RetrievedTypeHelper<RetrievedType : Any, ActualType : Any>(
        private val retrieveType: KClass<RetrievedType>
    ) {
        private lateinit var converter: ((RetrievedType) -> ActualType)

        infix fun convertedBy(converter: (RetrievedType) -> ActualType) {
            this.converter = converter
        }

        fun build(attrs: ConfigPropertyAttributes<ActualType>) : ConfigProperty<ActualType> {
            if (!::converter.isInitialized) {
                throw Exception("Property '${attrs.keyPath}' of type " +
                        "${attrs.valueType.simpleName} from source " +
                        "${attrs.configSource.name} was set to retrieve " +
                        "as type ${retrieveType.simpleName}, but no " +
                        "conversion function was " +
                        "given via 'convertedBy'")
            }

            val retrievedTypeAttrs =
                ConfigPropertyAttributes(attrs.keyPath, retrieveType, attrs.readOnce, attrs.configSource)
            // The supplier which retrieves the value as RetrievedType
            val retrievedTypeSupplier = retrievedTypeAttrs.supplier
            // The supplier which invokes the retrievedTypeSupplier and converts the result to ActualType
            val actualTypeSupplier = { converter(retrievedTypeSupplier()) }

            return ConfigPropertyImpl(attrs, actualTypeSupplier)
        }
    }
}

/**
 * A helper class used to build a [ConfigProperty] instance which is composed
 * of multiple 'inner' properties.  Each of these properties is queried in
 * order when looking for the value.
 */
class MultiConfigPropertyBuilder<T : Any>(private val type: KClass<T>) {
    private val innerProperties = mutableListOf<ConfigProperty<T>>()

    fun property(block: ConfigPropertyBuilder<T>.() -> Unit) {
        innerProperties.add(ConfigPropertyBuilder(type).apply(block).buildProp())
    }

    private fun findResultOrAggregateExceptions(configProperties: Iterable<ConfigProperty<T>>): ConfigResult<T> {
        val exceptions = mutableListOf<Throwable>()
        for (prop in configProperties) {
            //TODO: configRunCatching here is a bit redundant, as the inner props already have
            // a ConfigResult, they just don't expose it (they only expose the value).  This makes
            // sense for a normal property--but it's a bit wasteful for a multi-property, it was
            // just nice to re-use the code.  We could either have
            // multiconfigproperty not creater inner property types (and just create
            // attrs/readstrats and use those), or, we could expose ConfigResult
            // in ConfigProperty and access that instead of wrapping the access of
            // value in configRunCatching
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
    ConfigPropertyBuilder(T::class).also(block).buildProp()

inline fun <reified T : Any> multiProperty(block: MultiConfigPropertyBuilder<T>.() -> Unit): ConfigProperty<T> =
    MultiConfigPropertyBuilder(T::class).also(block).build()
