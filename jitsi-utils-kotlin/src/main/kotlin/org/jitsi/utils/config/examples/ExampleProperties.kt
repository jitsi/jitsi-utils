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

package org.jitsi.utils.config.examples

import org.jitsi.utils.config.FallbackProperty
import org.jitsi.utils.config.SimpleProperty
import org.jitsi.utils.config.helpers.attributes
import java.time.Duration
import kotlin.reflect.KClass

private val newConfig = ExampleConfigSource("newConfig", mapOf(
    "newPropInt" to 42,
    "newPropLong" to 43L,
    "onlyNewProp" to Duration.ofSeconds(10)
))
private val legacyConfig = ExampleConfigSource("legacyConfig", mapOf(
    "oldPropInt" to 41,
    "oldPropLong" to 44L,
    "onlyOldProp" to 10
))

internal class ExampleProperties {
    // Basic property using a class
    class BasicProperty : SimpleProperty<Int>(
        attributes {
            name("newPropInt")
            readOnce()
            fromConfig(newConfig)
        }
    )

    // Property which retrieves the value as a different type and transforms it
    class TransformingProperty : SimpleProperty<Long>(
        attributes {
            name("onlyNewProp")
            readOnce()
            fromConfig(newConfig)
            retrievedAs<Duration>() convertedBy { it.toMillis() }
        }
    )

    // Property for which we search multiple ConfigSources (in the given order)
    class LegacyProperty : FallbackProperty<Long>(
        attributes {
            name("oldPropLong")
            readOnce()
            fromConfig(legacyConfig)
        },
        attributes {
            name("newPropLong")
            readOnce()
            fromConfig(newConfig)
        }
    )

    // Same as above, but using a helper class which takes care of the
    // boilerplate
    class LegacyPropertyShort : LegacyFallbackProperty<Long>(
        Long::class,
        readOnce = true,
        legacyName = "oldPropLong",
        newName = "newPropLong"
    )

    // A property where one source is deprecated
    class LegacyDeprecatedProperty : FallbackProperty<Long>(
        attributes {
            name("oldPropLong")
            readOnce()
            fromConfig(legacyConfig)
            deprecated("'oldPropLong' is no longer supported, please use " +
                    "'newPropLong' in the new config file")
        },
        attributes {
            name("newPropLong")
            readOnce()
            fromConfig(newConfig)
        }
    )

    // This prop won't be found in legacy config and will fall back to newConfig
    class LegacyFallthroughProperty : FallbackProperty<Long>(
        attributes {
            name("nonexistent")
            readOnce()
            fromConfig(legacyConfig)
        },
        attributes {
            name("onlyNewProp")
            readOnce()
            fromConfig(newConfig)
            retrievedAs<Duration>() convertedBy { it.toMillis() }
        }
    )

    // A property that's never found
    // Accessing this will give the following error:
    // NoAcceptablePropertyInstanceFoundException: Unable to find or parse configuration property due to: [ConfigPropertyNotFoundException: Could not find value for property at 'notFound' in config legacyConfig, ConfigPropertyNotFoundException: Could not find value for property at 'notFound' in config newConfig]
    class NeverFoundProperty : LegacyFallbackProperty<Long>(
        Long::class,
        readOnce = true,
        legacyName = "notFound",
        newName = "notFound"
    )

    // Trying to retrieve a value as the incorrect type
    // Accessing this will give the following error:
    // ConfigValueParsingException: Value '41' (type Integer) at path 'oldPropInt' in config legacyConfig could not be cast to Lon
    class WrongTypeProperty : SimpleProperty<Long>(
        attributes {
            name("oldPropInt")
            readOnce()
            fromConfig(legacyConfig)
        }
    )

    // A property which tries to retrieve a value type that isn't supported by
    // the ConfigSource
    // Accessing this will give the following error:
    // ConfigurationValueTypeUnsupportedException: No getter found for value of type class org.jitsi.utils.configk.examples.ExampleProperties$Companion$Foo
    class Foo
    class UnsupportedValueTypeProperty : SimpleProperty<Foo>(
        attributes {
            name("propName")
            readOnce()
            fromConfig(newConfig)
        }
    )
}

/**
 * A helper class which assumes the property is held in [legacyConfig] under
 * one name and [newConfig] under another name
 */
internal abstract class LegacyFallbackProperty <T : Any>(
    valueType: KClass<T>,
    readOnce: Boolean,
    legacyName: String,
    newName: String
) : FallbackProperty<T>(
    attributes(valueType) {
        if (readOnce) readOnce() else readEveryTime()
        name(legacyName)
        fromConfig(legacyConfig)
    },
    attributes(valueType) {
        if (readOnce) readOnce() else readEveryTime()
        name(newName)
        fromConfig(newConfig)
    }
)

internal fun main() {
    println("simpleProperty = ${ExampleProperties.BasicProperty().value}")
    println("transformingProperty = ${ExampleProperties.TransformingProperty().value}")
    println("legacyProperty = ${ExampleProperties.LegacyProperty().value}")
    println("legacyPropertyUsingHelper = ${ExampleProperties.LegacyPropertyShort().value}")
    println("legacyDeprecatedProperty = ${ExampleProperties.LegacyDeprecatedProperty().value}")
    println("legacyFallthroughProperty = ${ExampleProperties.LegacyFallthroughProperty().value}")
    try {
        ExampleProperties.NeverFoundProperty().value
    } catch (t: Throwable) {
        println("neverFoundProperty: $t")
    }
    try {
        ExampleProperties.WrongTypeProperty().value
    } catch (t: Throwable) {
        println("wrongTypeProperty: $t")
    }
    try {
        ExampleProperties.UnsupportedValueTypeProperty().value
    } catch (t: Throwable) {
        println("unsupportedValueTypeProperty: $t")
    }
}
