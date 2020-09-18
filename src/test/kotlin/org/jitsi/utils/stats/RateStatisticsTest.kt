/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.utils.stats

import io.kotlintest.IsolationMode
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.FakeClock
import org.jitsi.utils.ms

class RateStatisticsTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf
    private val fakeClock = FakeClock()
    private val rateStatistics = RateStatistics(1000, 8000f, fakeClock)

    init {
        should("work correctly") {
            (0..9).forEach {
                rateStatistics.update(1)
                fakeClock.elapse(10.ms)
            }
            rateStatistics.accumulatedCount shouldBe 10
            rateStatistics.rate shouldBe 10 * 8 /* bits per byte */

            fakeClock.elapse(500.ms)
            (0..9).forEach {
                rateStatistics.update(1)
                fakeClock.elapse(10.ms)
            }
            rateStatistics.accumulatedCount shouldBe 20
            rateStatistics.rate shouldBe 20 * 8 /* bits per byte */

            fakeClock.elapse(500.ms)
            rateStatistics.accumulatedCount shouldBe 10
            rateStatistics.rate shouldBe 10 * 8 /* bits per byte */

            fakeClock.elapse(5.seconds)
            rateStatistics.accumulatedCount shouldBe 0
            rateStatistics.rate shouldBe 0
        }
    }
}
