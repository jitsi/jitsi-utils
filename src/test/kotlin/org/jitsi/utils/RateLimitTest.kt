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
        }
    }
}
