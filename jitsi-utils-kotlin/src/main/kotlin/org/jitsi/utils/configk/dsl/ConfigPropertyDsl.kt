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
import org.jitsi.utils.configk.PropertyDelegate
import org.jitsi.utils.configk.PropertyDelegateImpl
import org.jitsi.utils.configk.getOrThrow
import org.jitsi.utils.configk.readOnce
import org.jitsi.utils.configk.spi.TypedConfigValueGetterService
import kotlin.reflect.KClass

fun legacyConfig() = Any()
fun newConfig() = Any()

class XXX : ConfigProperty<Boolean> by property({
    attributes {
        readOnce()
    }
    search(
        "legacy".asType<Int>() from legacyConfig(),
        //"blah" from legacyConfig() asType<Int>().convertedTo<Long> {}
        ("new".asType<Long>() from newConfig()).convertedTo { it.toInt() }
    )
})

data class PropBuilder(
    var attributes: ConfigPropertyAttributes? = null
) {
    inner class PropNameType<T : Any>(val name: String, val clazz: KClass<T>)

    inline fun <reified T : Any> String.asType(): PropBuilder.PropNameType<T> =
        this@PropBuilder.PropNameType(this, T::class)

    inline infix fun <reified T : Any> PropBuilder.PropNameType<T>.from(configInstance: Any): PropertyDelegate<T> {
        val typedGetter = TypedConfigValueGetterService.getterFor(clazz)
        val supplier = { typedGetter(configInstance, this.name) }
        return PropertyDelegateImpl(this@PropBuilder.attributes!!, supplier)
    }
}

fun PropBuilder.attributes(block: ConfigPropertyAttributes.() -> Unit) {
    val attrs = attributes ?: ConfigPropertyAttributes(true)
    attrs.block()
}

fun <T : Any, U : Any> PropertyDelegate<T>.convertedTo(block: (T) -> U): PropertyDelegate<U> {
    return propDelegateHelper(this.attributes) { block(getValue().getOrThrow())}
}

fun<U : Any> propDelegateHelper(attrs: ConfigPropertyAttributes, supplier: () -> U): PropertyDelegateImpl<U> {
    return object : PropertyDelegateImpl<U>(attrs, supplier) {}
}

inline fun <reified T : Any> PropBuilder.search(vararg configProps: PropertyDelegate<T>): List<PropertyDelegate<T>> =
    configProps.toList()

inline fun <reified T : Any> property(block: PropBuilder.() -> Unit): ConfigProperty<T> {
    with (PropBuilder()) {
        block()
    }
    return object : ConfigProperty<T> {
        override val value: T
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }
}
