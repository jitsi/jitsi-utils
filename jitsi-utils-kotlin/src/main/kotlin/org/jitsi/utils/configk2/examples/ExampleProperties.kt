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

import org.jitsi.utils.configk2.dsl.multiProperty
import org.jitsi.utils.configk2.dsl.property
import java.time.Duration

class ExampleProperties {
    companion object {
        val simpleProperty = property<Int> {
            name("simpleProperty")
            readOnce()
            fromConfig(newConfig())
        }

        val transformingProperty = property<Long> {
            name("transformingProperty")
            readOnce()
            fromConfig(newConfig())
            retrievedAs<Duration>() convertedBy { it.toMillis() }
        }

        val legacyProperty = multiProperty<Long> {
            property {
                name("legacyName")
                readOnce()
                fromConfig(legacyConfig())
            }
            property {
                name("newName")
                readOnce()
                fromConfig(newConfig())
                retrievedAs<Duration>() convertedBy { it.toMillis() }
            }
        }
    }
}

fun main() {
    println("simpleProperty = ${ExampleProperties.simpleProperty.value}")
    println("transformingProperty = ${ExampleProperties.transformingProperty.value}")
    println("legacyProperty = ${ExampleProperties.legacyProperty.value}")
}