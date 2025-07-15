/*
 * Jicofo, the Jitsi Conference Focus.
 *
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
package org.jitsi.utils

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jitsi.utils.time.FakeClock

class RateLimitTest : ShouldSpec() {
    init {
        context("RateLimit test") {
            val clock = FakeClock()
            val rateLimit = RateLimit(clock = clock)

            should("allow 1st request") {
                rateLimit.accept() shouldBe true
            }
            should("not allow next request immediately") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(5.secs)
            should("not allow next request after 5 seconds") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(6.secs)
            should("allow 2nd request after 11 seconds") {
                rateLimit.accept() shouldBe true
            }
            should("not allow 3rd request after 11 seconds") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(10.secs)
            should("allow 3rd request after 21 seconds") {
                rateLimit.accept() shouldBe true
            }
            clock.elapse(11.secs)
            should("not allow more than 3 request within the last minute (31 second)") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(10.secs)
            should("not allow more than 3 request within the last minute (41 second)") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(10.secs)
            should("not allow more than 3 request within the last minute (51 second)") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(10.secs)
            should("allow the 4th request after 60 seconds have passed since the 1st (61 second)") {
                rateLimit.accept() shouldBe true
            }
            clock.elapse(5.secs)
            should("not allow the 5th request in 66th second") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(5.secs)
            should("allow the 5th request in 71st second") {
                rateLimit.accept() shouldBe true
            }
            val then = clock.instant()
            clock.elapse(10.secs)
            should("allow a passed-in time to override the current clock time") {
                rateLimit.accept(then) shouldBe false
            }
            should("still read the current clock time afterwards") {
                rateLimit.accept() shouldBe true
            }
        }

        context("A RateLimit with parameters") {
            val clock = FakeClock()
            val rateLimit = RateLimit(
                clock = clock,
                defaultMinInterval = 10.ms,
                maxRequests = 3,
                interval = 500.ms
            )
            should("allow 1st request") {
                rateLimit.accept() shouldBe true
            }
            should("not allow next request immediately") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(5.ms)
            should("not allow next request after 5 ms") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(6.ms)
            should("allow 2nd request after 11 ms") {
                rateLimit.accept() shouldBe true
            }
            should("not allow 3rd request after 11 ms") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(10.ms)
            should("allow 3rd request after 21 ms") {
                rateLimit.accept() shouldBe true
            }
            clock.elapse(11.ms)
            should("not allow more than 3 request within the last 500 ms (31 ms)") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(100.ms)
            should("not allow more than 3 request within the last 500 ms (131 ms)") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(300.ms)
            should("not allow more than 3 request within the last 500 ms (431 ms)") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(70.ms)
            should("allow the 4th request after 500 ms have passed since the 1st (501 ms)") {
                rateLimit.accept() shouldBe true
            }
            clock.elapse(5.ms)
            should("not allow the 5th request in 506th ms") {
                rateLimit.accept() shouldBe false
            }
            clock.elapse(5.ms)
            should("allow the 5th request in 511st ms") {
                rateLimit.accept() shouldBe true
            }
            val then = clock.instant()
            clock.elapse(500.ms)
            should("allow a passed-in time to override the current clock time") {
                rateLimit.accept(then) shouldBe false
            }
            should("still read the current clock time afterwards") {
                rateLimit.accept() shouldBe true
            }
            clock.elapse(15.ms)
            should("allow a passed-in minimum interval to override the default minimum interval") {
                rateLimit.accept(minInterval = 20.ms) shouldBe false
                rateLimit.accept(minInterval = 10.ms) shouldBe true
            }
            clock.elapse(10.ms)
            should("still use the defaultMinTime afterwards") {
                rateLimit.accept() shouldBe true
            }
        }
    }
}
