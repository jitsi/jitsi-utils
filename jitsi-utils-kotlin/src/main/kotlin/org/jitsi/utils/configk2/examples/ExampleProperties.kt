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

import org.jitsi.utils.configk2.ConfigProperty
import org.jitsi.utils.configk2.ConfigSource
import org.jitsi.utils.configk2.dsl.MultiConfigPropertyBuilder
import org.jitsi.utils.configk2.dsl.multiProperty
import org.jitsi.utils.configk2.dsl.property
import java.time.Duration

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
        val simpleProperty = property<Int> {
            name("newPropInt")
            readOnce()
            fromConfig(newConfig())
        }

        val transformingProperty = property<Long> {
            name("onlyNewProp")
            readOnce()
            fromConfig(newConfig())
            retrievedAs<Duration>() convertedBy { it.toMillis() }
        }

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

        val legacyPropertyUsingHelper =
            simple<Long>(readOnce = true, legacyName = "oldPropLong", newName = "newPropLong")

        // This prop won't be found in legacy config and will fall back to newConfig
        val legacyFallthroughProperty = multiProperty<Long> {
            property {
                name("non-existant")
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

        val neverFoundProperty =
            simple<Int>(readOnce = true, legacyName = "notFound", newName = "notFound")
    }

    //TODO: validate read frequency stuff is working
    //TODO: we should have an AbstractConfigProperty which would print a message if
    // a prop was marked as deprecated/etc and it was found
    //TODO: clean up and organize code
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



fun main() {
    println("simpleProperty = ${ExampleProperties.simpleProperty.value}")
    println("transformingProperty = ${ExampleProperties.transformingProperty.value}")
    println("legacyProperty = ${ExampleProperties.legacyProperty.value}")
    println("legacyPropertyUsingHelper = ${ExampleProperties.legacyPropertyUsingHelper.value}")
    println("legacyFallthroughProperty = ${ExampleProperties.legacyFallthroughProperty.value}")
    try {
        println("neverFoundProperty = ${ExampleProperties.neverFoundProperty.value}")
    } catch (t: Throwable) {
        println(t)
    }
}
