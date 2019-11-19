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

package org.jitsi.utils.configk.examples

import org.jitsi.utils.configk.ConfigProperty
import org.jitsi.utils.configk.ConfigSource
import org.jitsi.utils.configk.dsl.MultiConfigPropertyBuilder
import org.jitsi.utils.configk.dsl.multiProperty
import org.jitsi.utils.configk.dsl.property
import java.time.Duration
import kotlin.reflect.KClass

private val newConfig = ExampleConfigSource(mapOf(
    "newPropInt" to 42,
    "newPropLong" to 43L,
    "onlyNewProp" to Duration.ofSeconds(10)
))
private val legacyConfig = ExampleConfigSource(mapOf(
    "oldPropInt" to 41,
    "oldPropLong" to 44L,
    "onlyOldProp" to 10
))

fun newConfig(): ConfigSource = newConfig
fun legacyConfig(): ConfigSource = legacyConfig

class ExampleProperties {
    companion object {
        // Simple property defined using the DSL
        val simpleProperty = property<Int> {
            name("newPropInt")
            readOnce()
            fromConfig(newConfig())
        }

        // Property which retrieves the value as a different type and transforms it
        val transformingProperty = property<Long> {
            name("onlyNewProp")
            readOnce()
            fromConfig(newConfig())
            retrievedAs<Duration>() convertedBy { it.toMillis() }
        }

        // Property which was defined in a prior config source
        val legacyProperty = multiProperty<Long> {
            property {
                name("oldPropLong")
                readOnce()
                fromConfig(legacyConfig())
            }
            property {
                name("newPropLong")
                readOnce()
                fromConfig(newConfig())
            }
        }

        // Same as above, using a helper DSL method
        val legacyPropertyUsingHelper =
            simple<Long>(readOnce = true, legacyName = "oldPropLong", newName = "newPropLong")

        // Same as above, but defining a class for the property instead of a value
        class LegacyPropertyUsingClass : SimpleConfig<Long>(
            valueType = Long::class,
            legacyName = "oldPropLong",
            newName = "newPropLong",
            readOnce = true
        )

        // A property where one source is deprecated
        val legacyDeprecatedProperty = multiProperty<Long> {
            property {
                name("oldPropLong")
                readOnce()
                fromConfig(legacyConfig())
                deprecated("'oldPropLong' is no longer supported, please use " +
                        "'newPropLong' in the new config file")
            }
            property {
                name("newPropLong")
                readOnce()
                fromConfig(newConfig())
            }
        }

        // This prop won't be found in legacy config and will fall back to newConfig
        val legacyFallthroughProperty = multiProperty<Long> {
            property {
                name("nonexistent")
                readOnce()
                fromConfig(legacyConfig())
            }
            property {
                name("onlyNewProp")
                readOnce()
                fromConfig(newConfig())
                retrievedAs<Duration>() convertedBy { it.toMillis() }
            }
        }

        // A property that's never found
        val neverFoundProperty =
            simple<Long>(readOnce = true, legacyName = "notFound", newName = "notFound")

        // Trying to retrieve a value as the incorrect type
        val wrongTypeProperty = property<Long> {
            name("oldPropInt")
            readOnce()
            fromConfig(legacyConfig)
        }
    }
}

// An example helper to simplify a common case (a property that was in the old config and is now in
// new config, doesn't do anything fancy with the type
inline fun <reified T : Any> simple(readOnce: Boolean, legacyName: String, newName: String): ConfigProperty<T> {
    return MultiConfigPropertyBuilder<T>(T::class).apply {
        property {
            name(legacyName)
            if (readOnce) readOnce() else readEveryTime()
            fromConfig(legacyConfig())
        }
        property {
            name(newName)
            if (readOnce) readOnce() else readEveryTime()
            fromConfig(newConfig())
        }
    }.build()
}

open class SimpleConfig<T : Any>(
    valueType: KClass<T>,
    legacyName: String,
    newName: String,
    readOnce: Boolean
) : ConfigProperty<T> {
    val multiProp = MultiConfigPropertyBuilder(valueType).apply {
        property {
            name(legacyName)
            if (readOnce) readOnce() else readEveryTime()
            fromConfig(legacyConfig())
        }
        property {
            name(newName)
            if (readOnce) readOnce() else readEveryTime()
            fromConfig(newConfig())
        }
    }.build()

    override val value: T
        get() = multiProp.value
}



fun main() {
    println("simpleProperty = ${ExampleProperties.simpleProperty.value}")
    println("transformingProperty = ${ExampleProperties.transformingProperty.value}")
    println("legacyProperty = ${ExampleProperties.legacyProperty.value}")
    println("legacyPropertyUsingHelper = ${ExampleProperties.legacyPropertyUsingHelper.value}")
    println("legacyPropertyUsingClass = ${ExampleProperties.Companion.LegacyPropertyUsingClass().value}")
    println("legacyDeprecatedProperty = ${ExampleProperties.legacyDeprecatedProperty.value}")
    println("legacyFallthroughProperty = ${ExampleProperties.legacyFallthroughProperty.value}")
    try {
        ExampleProperties.neverFoundProperty.value
    } catch (t: Throwable) {
        println("neverFoundProperty: $t")
    }
    try {
        ExampleProperties.wrongTypeProperty.value
    } catch (t: Throwable) {
        println("wrongTypeProperty: $t")
    }
}
