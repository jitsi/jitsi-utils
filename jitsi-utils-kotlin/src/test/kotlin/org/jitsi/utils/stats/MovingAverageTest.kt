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

package org.jitsi.utils.stats

import io.kotlintest.IsolationMode
import io.kotlintest.matchers.doubles.plusOrMinus
import io.kotlintest.minutes
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.FakeClock

class MovingAverageTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private val fakeClock = FakeClock()

    private val average = MovingAverage<Int>(5.seconds, fakeClock)

    init {
        "the average" {
            should("only take into account values in the window") {
                // Add 4 elements at time 0
                (0..3).forEach {
                    average.add(it)
                }
                // Add 4 elements at time 6
                fakeClock.elapse(6.seconds)
                (4..7).forEach {
                    average.add(it)
                }
                average.get() shouldBe(5.5 plusOrMinus(.1))
            }
            should("not include values outside the window even if no adds have been done") {
                (0..3).forEach {
                    average.add(it)
                }
                fakeClock.elapse(1.minutes)
                average.get() shouldBe(0.0)
            }
        }
    }
}
