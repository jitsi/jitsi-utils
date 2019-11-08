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

import io.kotlintest.IsolationMode
import io.kotlintest.specs.ShouldSpec
import io.kotlintest.shouldBe
import org.jitsi.utils.configk.PropValueRetriever
import org.jitsi.utils.configk.getOrThrow
import org.jitsi.utils.configk.readEveryTime
import org.jitsi.utils.configk.readOnce

class PropValueRetrieverTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    var numTimesCalled = 0
    private val intSupplier = { numTimesCalled++ }

    init {
        "A read-once property" {
            val prop = PropValueRetriever(readOnce(), intSupplier)

            "when accessed multiple times" {
                repeat(5) { prop.result.getOrThrow() }
                should("only have called the supplier once") {
                    numTimesCalled shouldBe 1
                }
            }
        }
        "A read-every-time property" {
            val prop = PropValueRetriever(readEveryTime(), intSupplier)
            "when accessed multiple times" {
                repeat(5) { prop.result.getOrThrow() }
                should("have called the supplier each time") {
                    numTimesCalled shouldBe 5
                }
            }
        }
    }

}