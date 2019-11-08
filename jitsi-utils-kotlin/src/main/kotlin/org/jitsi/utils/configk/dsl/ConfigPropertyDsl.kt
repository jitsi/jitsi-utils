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

import org.jitsi.utils.configk.ConfigProperty
import org.jitsi.utils.configk.ConfigPropertyAttributes
import org.jitsi.utils.configk.ConfigResult
import org.jitsi.utils.configk.FallbackConfigProperty
import org.jitsi.utils.configk.PropValueRetriever
import org.jitsi.utils.configk.getOrThrow
import org.jitsi.utils.configk.readOnce
import org.jitsi.utils.configk.spi.TypedConfigValueGetterService
import java.time.Duration
import kotlin.reflect.KClass


//NOTE: as of now, this file is a playground until we decide on something

fun legacyConfig() = Any()
fun newConfig() = Any()

class XXX : ConfigProperty<Long> by property({
    attributes {
        readOnce()
    }
    search(
        "legacyPropName" from legacyConfig() asType(Long::class),
        "newPropName" from newConfig() asType(Duration::class) convertedBy { it.toMillis() }
    )
})

abstract class DefaultConfig<T : Any>(
    clazz: KClass<T>,
    attributes: ConfigPropertyAttributes,
    legacyPropName: String,
    newPropName: String
) : FallbackConfigProperty<T>() {

    private val legacyProp = PropSource(
        clazz,
        attributes = attributes,
        path = legacyPropName,
        configSource = legacyConfig()
    )
    private val newProp = PropSource(
        clazz,
        attributes = attributes,
        path = newPropName,
        configSource = newConfig()
    )

    override val propertyPriority: List<ConfigResult<T>>
        get() = listOf(legacyProp.result, newProp.result)
}

class Simple : DefaultConfig<Int>(Int::class, readOnce(), legacyPropName, newPropName) {
    companion object {
        const val legacyPropName = "legacyName"
        const val newPropName = "newName"
    }
}

class YYY : ConfigProperty<Long> {
    private val attributes = ConfigPropertyAttributes(readOnce = true)
    val legacyProp = createProp<Long>(
        attributes = attributes,
        propName = "legacyName",
        configSource = legacyConfig()
    )
    val newProp = createProp<Long>(
        attributes = attributes,
        propName = "newName",
        configSource = newConfig()
    )

    override val value: Long
        get() = TODO() //legacyProp.result.getOrElse { newProp.result.getOrThrow() }
}

inline fun <reified T : Any> createProp(
    attributes: ConfigPropertyAttributes,
    propName: String,
    configSource: Any
) : PropSource<T> = PropSource(T::class, attributes, propName, configSource)

inline fun <reified T : Any> ConfigPropertyAttributes.propSource(name: String, configSource: Any) =
    PropSource(T::class, this, name, configSource)

class PropSource<T : Any>(
    clazz: KClass<T>,
    val attributes: ConfigPropertyAttributes,
    val path: String,
    val configSource: Any
) {
    private val propValueRetriever: PropValueRetriever<T>
    init {
        val typedGetter = TypedConfigValueGetterService.getterFor(clazz)
        val supplier = { typedGetter(configSource, path) }
        propValueRetriever = PropValueRetriever(attributes, supplier)
    }

    val result: ConfigResult<T>
        get() = propValueRetriever.result
}

data class PropBuilder(
    var attributes: ConfigPropertyAttributes? = null
) {
    inner class PropNameSource(val name: String, val cofigSource: Any)

    inline infix fun <reified T : Any> PropNameSource.asType(clazz: KClass<T>): PropValueRetriever<T> {
        val typedGetter = TypedConfigValueGetterService.getterFor(T::class)
        val supplier = { typedGetter(this.cofigSource, this.name) }
        return PropValueRetriever<T>(this@PropBuilder.attributes!!, supplier)
    }

    infix fun String.from(configSource: Any): PropNameSource = PropNameSource(this, configSource)
}

fun PropBuilder.attributes(block: ConfigPropertyAttributes.() -> Unit) {
    val attrs = attributes ?: ConfigPropertyAttributes(true)
    attrs.block()
}

infix fun <T : Any, U : Any> PropValueRetriever<T>.convertedBy(block: (T) -> U): PropValueRetriever<U> {
    return propValueRetrieverHelper(this.attributes) { block(result.getOrThrow())}
}

fun<U : Any> propValueRetrieverHelper(attrs: ConfigPropertyAttributes, supplier: () -> U): PropValueRetriever<U> {
    return PropValueRetriever(attrs, supplier)
}

inline fun <reified T : Any> PropBuilder.search(vararg retrievers: PropValueRetriever<T>): List<PropValueRetriever<T>> =
    retrievers.toList()

inline fun <reified T : Any> property(block: PropBuilder.() -> Unit): ConfigProperty<T> {
    with (PropBuilder()) {
        block()
    }
    return object : ConfigProperty<T> {
        override val value: T
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }
}
