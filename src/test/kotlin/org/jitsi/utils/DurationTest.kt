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

package org.jitsi.utils

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class DurationTest : ShouldSpec() {
    init {
        context("the duration helpers should work") {
            5.nanos shouldBe Duration.ofNanos(5)
            5.micros shouldBe Duration.ofNanos(5_000)
            5.ms shouldBe Duration.ofMillis(5)
            5.secs shouldBe Duration.ofSeconds(5)
            5.mins shouldBe Duration.ofMinutes(5)
            5.hours shouldBe Duration.ofHours(5)
            5.days shouldBe Duration.ofDays(5)

            1.days * 2 shouldBe Duration.ofDays(2)
            2 * 4.hours shouldBe Duration.ofHours(8)
            10.days / 2.days shouldBe 5.0
            5.hours / 2.hours shouldBe 2.5

            4.ms.toMicros() shouldBe 4000
            2200.nanos.toMicros() shouldBe 2
            2900.nanos.toMicros() shouldBe 2

            2200.nanos.toRoundedMicros() shouldBe 2
            2500.nanos.toRoundedMicros() shouldBe 3
            2900.nanos.toRoundedMicros() shouldBe 3

            2200.micros.toRoundedMillis() shouldBe 2
            2500.micros.toRoundedMillis() shouldBe 3
            2900.micros.toRoundedMillis() shouldBe 3
        }
    }
}
