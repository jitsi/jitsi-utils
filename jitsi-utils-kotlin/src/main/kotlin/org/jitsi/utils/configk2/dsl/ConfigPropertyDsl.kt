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

import org.jitsi.utils.config.PropertyConfig
import org.jitsi.utils.configk.ConfigProperty
import org.jitsi.utils.configk2.ConfigPropertyAttributes
import org.jitsi.utils.configk2.ConfigPropertyAttributesBuilder
import org.jitsi.utils.configk2.ConfigRetriever
import org.jitsi.utils.configk2.TypedConfigPropertyAttributesBuilder
import kotlin.reflect.KClass

// Creating the conversion by having nested PropertyConfigBuilder -> TypedPropertyConfigBuilder<T> ->
// ConvertingTypedPropertyConfigBuilder<T, U> won't work, as the 'build' method in
// TypedPropertyConfigBuilder<T> would be 'fun build(): ConfigProperty<T>', but in
// ConvertingTypedPropertyConfigBuilder<T, U> it'd need to be 'fun build(): ConfigProperty<U>', but
// U isn't a subtype of T, so we can't do that override
// --> we can solve this by doing encapsulation instead of inheritance, just have to repeat
// some code a bit more
// --> nope, encapsulation doesn't work with the dsl, as we can take in a receiver of
// a single type (maybe that wasn't going to work anyway, since we didn't operate
// on the rertuned types when the conversion happened via withType, etc. anyway?
interface PropertyConfigBuilder<T : Any, U : Any> {
     fun name(name: String)
     fun withType(valueType: KClass<T>): TypedPropertyConfigBuilder<T>
     fun convertedBy(converter: (T) -> U): ConvertingTypedPropertyConfigBuilder<T, U>
}

class UntypedPropertyConfigBuilder {
     private val attributesBuilder: ConfigPropertyAttributesBuilder = ConfigPropertyAttributesBuilder()

     fun name(name: String) {
          attributesBuilder.name(name)
     }

     fun <V : Any> withType(valueType: KClass<V>): TypedPropertyConfigBuilder<V> {
          return TypedPropertyConfigBuilder(attributesBuilder.withType(valueType))
     }

     fun <W : Any> convertedBy(converter: (Any) -> W): ConvertingTypedPropertyConfigBuilder<Any, W> {
          TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
     }
}

open class TypedPropertyConfigBuilder<T : Any>(
     val typedConfigPropertyAttributesBuilder: TypedConfigPropertyAttributesBuilder<T>
) {
     fun name(name: String) {
          typedConfigPropertyAttributesBuilder.name(name)
     }

     fun build(): ConfigProperty<T> {
          val attrs = typedConfigPropertyAttributesBuilder.build()
          val retriever = ConfigRetriever(attrs)
          return object : ConfigProperty<T> {
               override val value: T
                    get() = retriever.retrieve()
          }
     }

     fun <W : Any> convertedBy(converter: (T) -> W): ConvertingTypedPropertyConfigBuilder<T, W> {
          return ConvertingTypedPropertyConfigBuilder<T, W>(typedConfigPropertyAttributesBuilder, converter)
     }

     fun <V : T> withType(valueType: KClass<V>): TypedPropertyConfigBuilder<V> {
          TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
     }

//     override fun <V, U : Any> convertedBy(converter: (V) -> U): ConvertingTypedPropertyConfigBuilder<V, U> where V : T {
//          return ConvertingTypedPropertyConfigBuilder<V, U>(typedConfigPropertyAttributesBuilder, converter)
//     }

//     override fun <T, U : Any> convertedBy(converter: (T) -> U) : ConvertingTypedPropertyConfigBuilder<T, U> {
//          return ConvertingTypedPropertyConfigBuilder(typedConfigPropertyAttributesBuilder, converter)
//     }
}

class ConvertingTypedPropertyConfigBuilder<T : Any, U : Any>(
     val typedConfigPropertyAttributesBuilder: TypedConfigPropertyAttributesBuilder<T>,
     val converter: (T) -> U = TODO()
) {
     fun name(name: String) {
          typedConfigPropertyAttributesBuilder.name(name)
     }

     fun <W : U> convertedBy(converter: (T) -> W): ConvertingTypedPropertyConfigBuilder<T, W> {
          TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
     }

     fun <V : T> withType(valueType: KClass<V>): TypedPropertyConfigBuilder<V> {
          TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
     }

     fun build(): ConfigProperty<U> {
          val attrs = typedConfigPropertyAttributesBuilder.build()
          val retriever = ConfigRetriever(attrs)
          val originalConfig =  object : ConfigProperty<T> {
               override val value: T
                    get() = retriever.retrieve()
          }
          return originalConfig.convertedBy(converter)
     }
}

inline fun <reified T : Any> property(block: TypedConfigPropertyAttributesBuilder<T>.() -> Unit): ConfigProperty<T> {
     val attributes = TypedConfigPropertyAttributesBuilder(T::class).apply(block).build()
     return object : ConfigProperty<T> {
          val retriever = ConfigRetriever(attributes)
          override val value: T
               get() = retriever.retrieve()
     }
}

inline fun <reified T : Any> propertyNew(block: PropertyConfigBuilder<Any, T>.() -> Unit): ConfigProperty<T> {
    TODO()
//     val attributes = TypedConfigPropertyAttributesBuilder(T::class).apply(block).build()
//     return object : ConfigProperty<T> {
//          val retriever = ConfigRetriever(attributes)
//          override val value: T
//               get() = retriever.retrieve()
//     }
}

fun <T : Any, U : Any> ConfigProperty<T>.convertedBy(converter: (T) -> U): ConfigProperty<U> {
     return object : ConfigProperty<U> {
          override val value: U
               get() = converter(this@convertedBy.value)
     }
}



//class MultiPropertyAttributesBuilder<T : Any> {
//     val propertiesAttributes = mutableListOf<ConfigPropertyAttributes<T>>()
//
//     inline fun <reified U : T> prop(block: TypedConfigPropertyAttributesBuilder<U>.() -> Unit) {
//          propertiesAttributes.add(property<U>(block))
//     }
//
//}

