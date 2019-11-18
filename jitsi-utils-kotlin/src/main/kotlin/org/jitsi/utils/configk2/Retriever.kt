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

package org.jitsi.utils.configk2

import org.jitsi.utils.configk.getOrThrow
import org.jitsi.utils.configk.strategy.ReadFrequencyStrategy
import org.jitsi.utils.configk.strategy.getReadStrategy

interface Retriever<T : Any> {
    fun retrieve(): T
}

class ConfigRetriever<T : Any>(
    val propertyAttributes: ConfigPropertyAttributes<T>
) : Retriever<T> {
    private val readFrequencyStrategy: ReadFrequencyStrategy<T> =
        getReadStrategy(propertyAttributes.readOnce, propertyAttributes.supplier)

    override fun retrieve(): T = readFrequencyStrategy.get().getOrThrow()
}

val <T : Any> ConfigPropertyAttributes<T>.supplier: () -> T
    get() {
        val typedGetter = configSource.getterFor(valueType)
        return { typedGetter(configSource, keyPath) }
    }