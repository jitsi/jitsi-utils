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
import org.jitsi.utils.configk.spi.TypedConfigValueGetterService
import org.jitsi.utils.configk.strategy.ReadFrequencyStrategy
import org.jitsi.utils.configk.strategy.getReadStrategy
import org.jitsi.utils.configk2.dsl.DummyConfig
import java.time.Duration

interface Retriever<T : Any> {
    fun retrieve(): T
}

class ConfigRetriever<T : Any>(
    val propertyAttributes: ConfigPropertyAttributes<T>
) : Retriever<T> {
    private val readFrequencyStrategy: ReadFrequencyStrategy<T>

    init {
        //TODO: change back to dynamic!!
//        val typedGetter
//                = TypedConfigValueGetterService.getterFor(propertyAttributes.valueType)
        val typedGetter: (Any, String) -> T = when(propertyAttributes.valueType) {
            Int::class -> ({ config, path -> (config as DummyConfig).getInt(path) as T })
            Duration::class -> ({ config, path -> (config as DummyConfig).getDuration(path) as T })
            else -> TODO()

        }
        val supplier = { typedGetter(propertyAttributes.configSource, propertyAttributes.keyPath) }
        readFrequencyStrategy =
            getReadStrategy(propertyAttributes.readOnce, supplier)

    }

    override fun retrieve(): T = readFrequencyStrategy.get().getOrThrow()
}