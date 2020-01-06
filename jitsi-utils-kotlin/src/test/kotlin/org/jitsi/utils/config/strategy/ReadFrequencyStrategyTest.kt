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

package org.jitsi.utils.config.strategy

import io.kotlintest.IsolationMode
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.config.ConfigResult
import org.jitsi.utils.config.exception.ConfigPropertyNotFoundException

class ReadFrequencyStrategyTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        val supplier = IntSupplier(42)
        val expectedResult = ConfigResult.found(42)
        "ReadOnceStrategy" {
            val strat = ReadOnceStrategy(supplier)
            should("get the correct result") {
                strat.get() shouldBe expectedResult
            }
            should("only invoke the supplier once") {
                repeat(5) { strat.get() }
                supplier.numTimesCalled shouldBe 1
            }
            should("read the value on creation") {
                supplier.result = 43
                strat.get() shouldBe expectedResult
            }
            should("handle it not being found") {
                // Have to create a new strat for this to get the not-found result
                supplier.notFound()
                val notFoundStrat = ReadOnceStrategy(supplier)
                notFoundStrat.get().shouldBeInstanceOf<ConfigResult.PropertyNotFound<Int>>()
            }
        }
        "ReadEveryTimeStrategy" {
            val strat = ReadEveryTimeStrategy(supplier)
            should("get the correct result") {
                strat.get() shouldBe expectedResult
            }
            should("invoke the supplier every time") {
                repeat(5) { strat.get() }
                supplier.numTimesCalled shouldBe 5
            }
            should("read the value every time") {
                strat.get() shouldBe expectedResult
                supplier.result = 43
                strat.get() shouldBe ConfigResult.found(43)
            }
            should("handle it not being found") {
                supplier.notFound()
                strat.get().shouldBeInstanceOf<ConfigResult.PropertyNotFound<Int>>()
            }
        }
    }

    private class IntSupplier(
        var result: Int
    ) : () -> Int {
        var numTimesCalled = 0
        private var found = true

        override fun invoke(): Int {
            numTimesCalled++
            return if (found) result else throw ConfigPropertyNotFoundException("not found")
        }

        fun notFound() { found = false }
    }
}
