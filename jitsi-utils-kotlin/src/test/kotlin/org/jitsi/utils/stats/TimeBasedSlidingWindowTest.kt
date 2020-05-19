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
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.seconds
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.FakeClock
import java.time.Duration
import java.time.Instant

class TimeBasedSlidingWindowTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private val fakeClock = FakeClock()

    private val evictedValues = mutableListOf<Int>()

    private val window = TimeBasedSlidingWindow<Int>(
        Duration.ofSeconds(5),
        { evictedValues.add(it) },
        fakeClock
    )

    init {
        "the window" {
            should("evict values that are outside the window") {
                // Add 4 elements at time 0
                (0..3).forEach {
                    window.add(it)
                }
                evictedValues shouldHaveSize 0
                // Add 4 elements at time 6
                fakeClock.setTime(Instant.ofEpochSecond(6))
                (4..7).forEach {
                    window.add(it)
                    fakeClock.elapse(1.seconds)
                }
                evictedValues shouldHaveSize 4
                evictedValues shouldContainExactly mutableListOf(0, 1, 2, 3)
                window.values() shouldContainExactly listOf(4, 5, 6, 7)
            }
        }
    }
}
