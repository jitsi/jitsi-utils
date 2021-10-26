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

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.doubles.percent
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.jitsi.utils.FakeClock
import org.jitsi.utils.ms
import org.jitsi.utils.secs

class RateStatisticsTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf
    private val fakeClock = FakeClock()
    private val windowSize = 1.secs
    private val rateStatistics = RateStatistics(windowSize.toMillis().toInt(), clock = fakeClock)

    init {
        should("work correctly") {
            (0..9).forEach {
                rateStatistics.update(1)
                fakeClock.elapse(10.ms)
            }
            rateStatistics.getAccumulatedCount() shouldBe 10
            rateStatistics.rate.toDouble() shouldBe averageBpsRange(rateStatistics.getAccumulatedCount())

            fakeClock.elapse(500.ms)
            (0..9).forEach {
                rateStatistics.update(1)
                fakeClock.elapse(10.ms)
            }
            rateStatistics.getAccumulatedCount() shouldBe 20
            rateStatistics.rate.toDouble().shouldBe(averageBpsRange(rateStatistics.getAccumulatedCount()))

            fakeClock.elapse(500.ms)
            rateStatistics.getAccumulatedCount() shouldBe 10
            rateStatistics.rate.toDouble().shouldBe(averageBpsRange(rateStatistics.getAccumulatedCount()))

            fakeClock.elapse(5.secs)
            rateStatistics.getAccumulatedCount() shouldBe 0
            rateStatistics.rate shouldBe 0
        }
    }

    /**
     * Return a matcher for [bytes]/duration in bits per second with a 10% tolerance, where duration is the time
     * elapsed on [fakeClock] (but at most [windowSize]).
     * I.e. simulate calculating the average bitrate with a tolerance. We need the tolerance because [RateStatistics]
     * loses precision when converting from bits to bytes.
     */
    private fun averageBpsRange(bytes: Long) =
        (8000.toDouble() * bytes / fakeClock.millis().coerceAtMost(windowSize.toMillis())) plusOrMinus 10.percent
}
