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
import java.time.Instant

class InstantTest : ShouldSpec() {
    init {
        context("The instant helpers should work") {
            Instant.ofEpochMilli(123).toEpochMicro() shouldBe 123000L
            instantOfEpochMicro(123456).toEpochMilli() shouldBe 123L
            instantOfEpochMicro(123987).toEpochMilli() shouldBe 123L

            instantOfEpochMicro(2501).formatMilli() shouldBe "2.501"

            instantOfEpochMicro(123456).toRoundedEpochMilli() shouldBe 123L
            instantOfEpochMicro(123987).toRoundedEpochMilli() shouldBe 124L

            Instant.ofEpochMilli(123).isFinite() shouldBe true
            Instant.ofEpochMilli(123).isInfinite() shouldBe false
            Instant.MAX.isInfinite() shouldBe true
            Instant.MIN.isInfinite() shouldBe true
            Instant.MAX.isFinite() shouldBe false
            Instant.MIN.isFinite() shouldBe false
            NEVER.isInfinite() shouldBe true
            NEVER.isFinite() shouldBe false

            max(Instant.ofEpochMilli(123), Instant.ofEpochMilli(456)) shouldBe Instant.ofEpochMilli(456)
            max(Instant.ofEpochMilli(123), Instant.MAX) shouldBe Instant.MAX
            max(Instant.ofEpochMilli(123), Instant.MIN) shouldBe Instant.ofEpochMilli(123)

            min(Instant.ofEpochMilli(123), Instant.ofEpochMilli(456)) shouldBe Instant.ofEpochMilli(123)
            min(Instant.ofEpochMilli(123), Instant.MAX) shouldBe Instant.ofEpochMilli(123)
            min(Instant.ofEpochMilli(123), Instant.MIN) shouldBe Instant.MIN
        }
    }
}
