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

package org.jitsi.utils.configk2.examples

import org.jitsi.utils.configk2.ConfigSource
import java.time.Duration
import kotlin.reflect.KClass

class ExampleConfigSource : ConfigSource {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getterFor(valueType: KClass<T>): (ConfigSource, String) -> T {
        return when(valueType) {
            Int::class -> ({ config, path -> (config as ExampleConfigSource).getInt(path) as T })
            Long::class -> ({ config, path -> (config as ExampleConfigSource).getLong(path) as T })
            Duration::class -> ({ config, path -> (config as ExampleConfigSource).getDuration(path) as T })
            else -> TODO("no getter available for $valueType")
        }
    }

    fun getInt(path: String): Int = 42
    fun getLong(path: String): Long = 43
    fun getDuration(path: String): Duration = Duration.ofSeconds(10)
}

private val newConfig = ExampleConfigSource()
private val legacyConfig = ExampleConfigSource()

fun newConfig(): ConfigSource = newConfig
fun legacyConfig(): ConfigSource = legacyConfig