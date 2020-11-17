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
package org.jitsi.utils

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.atomic.AtomicLong

internal class AtomicExtensionsTest : ShouldSpec() {
    init {
        context("increaseAndGet") {
            val l = AtomicLong()
            l.maxAssign(10) shouldBe 10
            l.maxAssign(5) shouldBe 10
            l.get() shouldBe 10
            l.maxAssign(20) shouldBe 20
        }
        context("decreaseAndGet") {
            val l = AtomicLong(100)
            l.minAssign(10) shouldBe 10
            l.minAssign(25) shouldBe 10
            l.get() shouldBe 10
            l.minAssign(5) shouldBe 5
        }
    }
}
