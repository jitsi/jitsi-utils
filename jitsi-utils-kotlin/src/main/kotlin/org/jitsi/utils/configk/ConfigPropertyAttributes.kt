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
import java.util.function.Supplier
import kotlin.reflect.KClass

data class ConfigPropertyAttributes(val readOnce: Boolean)

// TODO: previously, 'convertedBy' would chain onto a retriever and create a new retriever, but
// we often don't need all the attrs, etc. for a convertedBy retriever.  instead, we should come
// up with a simple, top-level 'retriever' interface (which probably just has a 'retrieve'
// method) and then we can subclass it to differentiate a 'converting' retriever from
// one that pulls from config
// --> just tried out the implementation of this, need to do some test props to see how it is
interface RetrieverInterface<T : Any> {
    fun retrieve(): T
}

//class Retriever<T : Any>(
//    val propertyAttributes: ConfigPropertyAttributes,
//) : RetrieverInterface<T> {
//    private val readFrequencyStrategy: ReadFrequencyStrategy<T>
//
//    init {
//        val typedGetter
//                = TypedConfigValueGetterService.getterFor(propertyAttributes.valueType)
//        val supplier = { typedGetter(propertyAttributes.configSource, propertyAttributes.keyPath) }
//        readFrequencyStrategy =
//            getReadStrategy(propertyAttributes.readOnce, supplier)
//
//    }
//
//    override fun retrieve(): T = readFrequencyStrategy.get().getOrThrow()
//}

class ValueTypeConverter<T : Any, R : Any>(
    private val supplier: Supplier<T>,
    private val converter: (T) -> R
) : RetrieverInterface<R> {

    override fun retrieve(): R = converter(supplier.get())
}

fun <T : Any, U : Any> RetrieverInterface<T>.convertedBy(converter: (T) -> U): RetrieverInterface<U> =
    ValueTypeConverter(Supplier { this@convertedBy.retrieve() }, converter)

fun readOnce(): ConfigPropertyAttributes = ConfigPropertyAttributes(readOnce = true)
fun readEveryTime(): ConfigPropertyAttributes = ConfigPropertyAttributes(readOnce = false)
