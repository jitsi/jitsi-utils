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

package org.jitsi.utils.config

import org.jitsi.utils.config.examples.ExampleConfigSource
import org.jitsi.utils.config.exception.NoAcceptablePropertyInstanceFoundException
import org.jitsi.utils.config.strategy.getReadStrategy
import java.time.Duration
import kotlin.reflect.KClass

class ConfigPropertyAttributesBuilder<T : Any>(
    private val valueType: KClass<T>
) {
    private var readOnce: Boolean? = null
    private var keyPath: String? = null
    private var configSource: ConfigSource? = null
    private var deprecationNotice: DeprecationNotice? = null
    /**
     * This can't be private because we access it from an inline function, but
     * it should be considered effectively private
     */
    /* private */ var innerRetriever: RetrievedTypeHelper<*>? = null

    fun readOnce() {
        readOnce = true
    }

    fun readEveryTime() {
        readOnce = false
    }

    fun name(propName: String) {
        this.keyPath = propName
    }

    fun fromConfig(configSource: ConfigSource) {
        this.configSource = configSource
    }

    fun deprecated(message: String) {
        this.deprecationNotice = DeprecationNotice(message)
    }

    fun build(): ConfigPropertyAttributes<T> {
        val keyPath: String = keyPath ?: throw Exception("Property name not set")
        val readOnce: Boolean = readOnce ?: throw Exception("Read frequency not set")
        val configSource: ConfigSource = configSource ?: throw Exception("Config source not set")

        val supplier = innerRetriever?.toSupplier(keyPath, configSource) ?:
            { configSource.getterFor(valueType).invoke(keyPath)}

        return ConfigPropertyAttributes(
            keyPath, valueType, readOnce, configSource, supplier, deprecationNotice
        )
    }
    inline fun <reified U : Any> retrievedAs(): RetrievedTypeHelper<U> {
        check(innerRetriever == null) { "Cannot use both 'retrievedAs' and 'transformedBy'"}
        return RetrievedTypeHelper(U::class).also { this.innerRetriever = it }
    }

    /**
     * Allow a transformation of the value without changing the type.
     * To implement this, we re-use the type-changing [RetrievedTypeHelper]
     * but just set the before and after types to be the same, and call
     * 'convertedBy' ourselves with the given function
     */
    fun transformedBy(transformer: (T) -> T) {
        check(innerRetriever == null) { "Cannot use both 'retrievedAs' and 'transformedBy'"}
        innerRetriever = RetrievedTypeHelper(valueType).apply {
            convertedBy(transformer)
        }
    }

    /**
     * A helper class used when a configuration value is retrieved as a
     * different type than the eventual value type.  For example,
     * this allows creating a [ConfigProperty] with a value
     * type of [Int], but retrieving it from the [ConfigSource] as
     * another type (say, [java.time.Duration] and then converting it to [Long])
     */
    inner class RetrievedTypeHelper<RetrievedType : Any>(
        private val valueType: KClass<RetrievedType>
    ) {
        private lateinit var converter: (RetrievedType) -> T

        infix fun convertedBy(converter: (RetrievedType) -> T) {
            this.converter = converter
        }

        fun toSupplier(
            keyPath: String,
            configSource: ConfigSource
        ): () -> T = { converter(configSource.getterFor(valueType).invoke(keyPath)) }
    }
}

open class SingleConfigProp <T : Any> (
    val attrs: ConfigPropertyAttributes<T>
) : ConfigProperty<T> {
    val getter = getReadStrategy(attrs.readOnce, attrs.supplier)

    override val value: T
        get() = getter.get().getOrThrow()
}

class ActualProp : SingleConfigProp<Int>(
    with(ConfigPropertyAttributesBuilder(Int::class)) {
        name("blah")
        readOnce()
        fromConfig(ExampleConfigSource("test", mapOf("blah" to "1")))
        retrievedAs<String>() convertedBy { it.toInt() }
        build()
    }
)

open class MultiConfigProp <T : Any>(
    vararg attrs: ConfigPropertyAttributes<T>
) : ConfigProperty<T> {
    protected val getters =
        attrs.map { getReadStrategy(it.readOnce, it.supplier)}

    override val value: T
        get() {
            val exceptions = mutableListOf<Throwable>()
            for (getter in getters) {
                when (val result = getter.get()) {
                    is ConfigResult.PropertyNotFound -> exceptions.add(result.exception)
                    is ConfigResult.PropertyFound -> return result.value
                }
            }
            throw NoAcceptablePropertyInstanceFoundException(exceptions)
        }
}

class LegacyConfigSource : ConfigSource {
    override val name: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reload() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toStringMasked(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

inline fun <reified T : Any> legacyProperty(block: ConfigPropertyAttributesBuilder<T>.() -> Unit): ConfigPropertyAttributes<T> {
    return with (ConfigPropertyAttributesBuilder(T::class)) {
        block()
        fromConfig(LegacyConfigSource())
        build()
    }
}

inline fun <reified T : Any> newProperty(block: ConfigPropertyAttributesBuilder<T>.() -> Unit): ConfigPropertyAttributes<T> {
    return with (ConfigPropertyAttributesBuilder(T::class)) {
        block()
        fromConfig(LegacyConfigSource())
        build()
    }
}

class ActualMultiProp : MultiConfigProp<Long>(
    legacyProperty {
        name("name")
        readOnce()
    },
    newProperty {
        name("name")
        readOnce()
        retrievedAs<Duration>() convertedBy { it.toMillis() }
    }
)

fun main() {
    val x = ActualProp()
    println(x.value)
}

