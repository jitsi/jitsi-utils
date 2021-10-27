package org.jitsi.utils.stats

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

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jitsi.utils.FakeClock
import org.jitsi.utils.ms
import org.jitsi.utils.secs
import java.io.File
import java.time.Instant

class RateTrackerTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf
    private val fakeClock = FakeClock()

    init {
        should("work correctly with bucketSize=1ms") {
            val rateTracker = RateTracker(1.secs, clock = fakeClock)

            (1..1000).forEach {
                rateTracker.update(1)
                fakeClock.elapse(1.ms)
                rateTracker.getAccumulatedCount() shouldBe it
            }
            rateTracker.rate shouldBe 1000

            fakeClock.elapse(1.ms)
            rateTracker.rate shouldBe 999

            fakeClock.elapse(499.ms)
            rateTracker.rate shouldBe 500

            fakeClock.elapse(500.ms)
            rateTracker.rate shouldBe 0
        }
        should("work correctly with bucketSize > 1ms") {
            val rateTracker = RateTracker(1.secs, 100.ms, clock = fakeClock)

            (1..1000).forEach {
                rateTracker.update(1)
                fakeClock.elapse(1.ms)
                rateTracker.getAccumulatedCount() shouldBe it
            }
            rateTracker.rate shouldBe 1000

            fakeClock.elapse(50.ms)
            rateTracker.rate shouldBe 1000
            fakeClock.elapse(50.ms)
            rateTracker.rate shouldBe 900

            fakeClock.elapse(400.ms)
            rateTracker.rate shouldBe 500

            fakeClock.elapse(1000.ms)
            rateTracker.rate shouldBe 0
        }
        xcontext("Test with a stream capture") {
            data class ParsedLine(
                val timeMs: Long,
                val value: Long
            ) {
                constructor(line: String) : this(
                    (line.split(" ")[0].toDouble() * 1000).toLong(),
                    line.split(" ")[1].toLong()
                )
            }

            fun run(stream: List<ParsedLine>) {
                val clock = FakeClock()
                var tracker = RateTracker(5.secs, 10.ms, clock)
                stream.forEachIndexed { index, parsedLine ->
                    if (index % 500 == 0) {
                        tracker = RateTracker(5.secs, 10.ms, clock)
                    }
                    clock.setTime(Instant.ofEpochMilli(parsedLine.timeMs))
                    tracker.update(parsedLine.value)
                    println("${parsedLine.timeMs}\t${tracker.rate}")
                }
            }

            val stream = File("${System.getProperty("user.dir")}/src/test/resources/stream-180p.txt")
                .readLines().drop(1).map { ParsedLine(it) }.toList()
            run(stream)
        }
    }
}
