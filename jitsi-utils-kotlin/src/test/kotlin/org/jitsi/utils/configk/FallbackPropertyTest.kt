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

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.configk.exception.ConfigPropertyNotFoundException
import org.jitsi.utils.configk.exception.NoAcceptablePropertyInstanceFoundException
import kotlin.reflect.KProperty

internal class FallbackPropertyTest : ShouldSpec() {

    init {
        "A fallback property with multiple sources" {
            "where one contains a result" {
                val testProp = FoundPropWithMultipleSources()
                should("find the result correctly") {
                    testProp.value shouldBe 42
                }
            }
            "where multiple contain results" {
                val testProp = MultipleFoundProps()
                should("use the first result") {
                    testProp.value shouldBe 42
                }
            }
            "where none contain a result" {
                val testProp = NotFoundProp()
                should("throw an exception") {
                    shouldThrow<NoAcceptablePropertyInstanceFoundException> {
                        testProp.value
                    }
                }
            }
        }
    }

    class FoundPropWithMultipleSources : FallbackConfigProperty<Int>() {
        val propOne: ConfigResult<Int> by NotFoundDelegate()
        val propTwo: ConfigResult<Int> by FoundDelegate(42)

        override val propertyPriority: List<ConfigResult<Int>> = listOf(
            propOne,
            propTwo
        )
    }

    class MultipleFoundProps : FallbackConfigProperty<Int>() {
        val propOne: ConfigResult<Int> by FoundDelegate(42)
        val propTwo: ConfigResult<Int> by FoundDelegate(43)
        val propThree: ConfigResult<Int> by FoundDelegate(44)

        override val propertyPriority: List<ConfigResult<Int>> = listOf(
            propOne,
            propTwo,
            propThree
        )
    }

    class NotFoundProp : FallbackConfigProperty<Int>() {
        val propOne: ConfigResult<Int> by NotFoundDelegate()
        val propTwo: ConfigResult<Int> by NotFoundDelegate()

        override val propertyPriority: List<ConfigResult<Int>> = listOf(
            propOne,
            propTwo
        )
    }

    class NotFoundDelegate<T : Any> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): ConfigResult<T> {
            return configRunCatching {
                throw ConfigPropertyNotFoundException("not found")
            }
        }
    }

    class FoundDelegate<T : Any>(val value: T) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): ConfigResult<T> {
            return ConfigResult.found(value)
        }
    }
}