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
import io.kotest.matchers.doubles.plusOrMinus
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

            1.days * 2.5 shouldBe Duration.ofHours(60)
            2.5 * 1.hours shouldBe Duration.ofMinutes(150)

            10.days / 2.days shouldBe 5.0
            5.hours / 2.hours shouldBe 2.5

            10.secs / 2 shouldBe 5.secs
            5.secs / 2 shouldBe 2500.ms

            10.secs / 2.0 shouldBe 5.secs
            5.secs / 2.0 shouldBe 2500.ms

            -(2200.ms) shouldBe (-2200).ms
            -(0.ms) shouldBe 0.ms

            4.ms.toMicros() shouldBe 4000
            2200.nanos.toMicros() shouldBe 2
            2900.nanos.toMicros() shouldBe 2

            2200.nanos.toRoundedMicros() shouldBe 2
            2500.nanos.toRoundedMicros() shouldBe 3
            2900.nanos.toRoundedMicros() shouldBe 3

            2200.micros.toRoundedMillis() shouldBe 2
            2500.micros.toRoundedMillis() shouldBe 3
            2900.micros.toRoundedMillis() shouldBe 3

            2200.ms.toDouble() shouldBe 2.2
            0.ms.toDouble() shouldBe 0.0
            (-(2200.ms)).toDouble() shouldBe -2.2

            2500.micros.toDoubleMillis() shouldBe 2.5
            0.micros.toDoubleMillis() shouldBe 0.0
            (-(2500.micros)).toDoubleMillis() shouldBe -2.5

            2200.micros.toDoubleMillis() shouldBe (2.2 plusOrMinus 1e-9)
            (-(2200.micros)).toDoubleMillis() shouldBe (-2.2 plusOrMinus 1e-9)

            abs(2200.micros) shouldBe 2200.micros
            abs(-(2200.micros)) shouldBe 2200.micros
            abs((-2200).micros) shouldBe 2200.micros
            abs(Duration.ZERO) shouldBe Duration.ZERO

            durationOfDoubleSeconds(2.2) shouldBe 2200.ms

            durationOfDoubleSeconds(Long.MAX_VALUE.toDouble()).seconds shouldBe Long.MAX_VALUE

            durationOfDoubleSeconds(-2.5) shouldBe (-2500).ms

            durationOfDoubleSeconds(2.999_999_999) shouldBe 2_999_999_999.nanos
            durationOfDoubleSeconds(2.999_999_999_5) shouldBe 3.secs

            2200.micros.isFinite() shouldBe true
            MAX_DURATION.isFinite() shouldBe false
            MIN_DURATION.isFinite() shouldBe false

            2200.micros.isInfinite() shouldBe false
            MAX_DURATION.isInfinite() shouldBe true
            MIN_DURATION.isInfinite() shouldBe true

            max(2200.ms, 3000.ms) shouldBe 3000.ms
            max(2200.ms, MAX_DURATION) shouldBe MAX_DURATION
            max(2200.ms, MIN_DURATION) shouldBe 2200.ms

            min(2200.ms, 3000.ms) shouldBe 2200.ms
            min(2200.ms, MAX_DURATION) shouldBe 2200.ms
            min(2200.ms, MIN_DURATION) shouldBe MIN_DURATION

            listOf(1, 2, 3).sumOf { it.ms } shouldBe 6.ms

            0.ms.coerceIn(1.ms, 10.ms) shouldBe 1.ms
            5.ms.coerceIn(1.ms, 10.ms) shouldBe 5.ms
            50.ms.coerceIn(1.ms, 10.ms) shouldBe 10.ms

            2501.micros.formatMilli() shouldBe "2.501"
        }
        context("roundUpTo") {
            should("return same value when duration is already a multiple of resolution") {
                val duration = Duration.ofMillis(100)
                val resolution = Duration.ofMillis(20)

                duration.roundUpTo(resolution) shouldBe duration
            }

            should("round up to next multiple when not already aligned") {
                val duration = Duration.ofMillis(101)
                val resolution = Duration.ofMillis(20)

                duration.roundUpTo(resolution) shouldBe Duration.ofMillis(120)
            }

            should("round up to next multiple when just below target") {
                val duration = Duration.ofMillis(99)
                val resolution = Duration.ofMillis(20)

                duration.roundUpTo(resolution) shouldBe Duration.ofMillis(100)
            }

            should("handle zero duration") {
                val duration = Duration.ZERO
                val resolution = Duration.ofMillis(20)

                duration.roundUpTo(resolution) shouldBe Duration.ZERO
            }

            should("handle different resolution units") {
                val duration = Duration.ofNanos(1_500_000) // 1.5ms
                val resolution = Duration.ofMillis(1)

                duration.roundUpTo(resolution) shouldBe Duration.ofMillis(2)
            }
        }

        context("roundDownTo") {
            should("return same value when duration is already a multiple of resolution") {
                val duration = Duration.ofMillis(100)
                val resolution = Duration.ofMillis(20)

                duration.roundDownTo(resolution) shouldBe duration
            }

            should("round down to previous multiple when not already aligned") {
                val duration = Duration.ofMillis(101)
                val resolution = Duration.ofMillis(20)

                duration.roundDownTo(resolution) shouldBe Duration.ofMillis(100)
            }

            should("round down to previous multiple when just above target + resolution") {
                val duration = Duration.ofMillis(119)
                val resolution = Duration.ofMillis(20)

                duration.roundDownTo(resolution) shouldBe Duration.ofMillis(100)
            }

            should("handle zero duration") {
                val duration = Duration.ZERO
                val resolution = Duration.ofMillis(20)

                duration.roundDownTo(resolution) shouldBe Duration.ZERO
            }

            should("handle different resolution units") {
                val duration = Duration.ofNanos(1_500_000) // 1.5ms
                val resolution = Duration.ofMillis(1)

                duration.roundDownTo(resolution) shouldBe Duration.ofMillis(1)
            }
        }
    }
}
