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

package org.jitsi.utils.configk.strategy

import io.kotlintest.IsolationMode
import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.configk.dsl.multiProperty
import org.jitsi.utils.configk.dsl.property
import org.jitsi.utils.configk.testutils.TestConfigSource
import java.time.Duration

class ConfigPropertyTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        val newConfig = TestConfigSource(mapOf(
            "newPropInt" to 42,
            "newPropLong" to 43L,
            "onlyNewProp" to Duration.ofSeconds(10)
        ))
        val legacyConfig = TestConfigSource(mapOf(
            "oldPropInt" to 41,
            "oldPropLong" to 44L,
            "onlyOldProp" to 10
        ))
        "Simple, read-once property" {
            val property = property<Int> {
                name("newPropInt")
                readOnce()
                fromConfig(newConfig)
            }
            should("read the correct value") {
                property.value shouldBe 42
            }
            should("only query the value once") {
                repeat (5) { property.value }
                newConfig.numGetsCalled shouldBe 1
            }
        }
        "Simple, read-every-time property" {
            val property = property<Int> {
                name("newPropInt")
                readEveryTime()
                fromConfig(newConfig)
            }
            should("read the correct value") {
                property.value shouldBe 42
            }
            should("read new values if they're changed") {
                newConfig.props["newPropInt"] = 43
                property.value shouldBe 43
            }
            should("query the value every time") {
                repeat (5) { property.value }
                newConfig.numGetsCalled shouldBe 5
            }
        }
        "Converting, read-once property" {
            var numTimesConverterCalled = 0
            val property = property<Long> {
                name("onlyNewProp")
                readOnce()
                fromConfig(newConfig)
                retrievedAs<Duration>() convertedBy { numTimesConverterCalled++; it.toMillis() }
            }
            should("read the correct value") {
                property.value shouldBe 10000
            }
            should("only query the value and converter once") {
                repeat (5) { property.value }
                newConfig.numGetsCalled shouldBe 1
                numTimesConverterCalled shouldBe 1
            }
        }
        "Converting, read-every-time property" {
            var numTimesConverterCalled = 0
            val property = property<Long> {
                name("onlyNewProp")
                readEveryTime()
                fromConfig(newConfig)
                retrievedAs<Duration>() convertedBy { numTimesConverterCalled++; it.toMillis() }
            }
            should("read the correct value") {
                property.value shouldBe 10000
            }
            should("only query the value and converter once") {
                repeat (5) { property.value }
                newConfig.numGetsCalled shouldBe 5
                numTimesConverterCalled shouldBe 5
            }
        }
        "Multi, read-once property" {
            val property = multiProperty<Long> {
                property {
                    name("oldPropLong")
                    readOnce()
                    fromConfig(legacyConfig)
                }
                property {
                    name("newPropLong")
                    readOnce()
                    fromConfig(newConfig)
                }
            }
            should("read the correct value") {
                property.value shouldBe 44
            }
            should("only query the value once and stop") {
                repeat (5) { property.value }
                legacyConfig.numGetsCalled shouldBe 1
                newConfig.numGetsCalled shouldBe 1
            }
        }
        "Multi, read-every-time property" {
            val property = multiProperty<Long> {
                property {
                    name("oldPropLong")
                    readEveryTime()
                    fromConfig(legacyConfig)
                }
                property {
                    name("newPropLong")
                    readEveryTime()
                    fromConfig(newConfig)
                }
            }
            should("read the correct value") {
                property.value shouldBe 44
            }
            should("reflect changes") {
                legacyConfig.props.remove("oldPropLong")
                property.value shouldBe 43
            }
            should("query the value every time") {
                repeat (5) { property.value }
                legacyConfig.numGetsCalled shouldBe 5
                // NOTE: A Multi property walks through each inner property
                // and stops at the first one that gives a value.  Since
                // read-every-time properties don't query the value on
                // creation, that means we never end up checking newConfig
                // for this value (since it exists in old config)
                newConfig.numGetsCalled shouldBe 0
            }
        }
    }
}


