package org.jitsi.utils

import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import java.util.concurrent.atomic.AtomicLong

internal class AtomicExtensionsTest : ShouldSpec() {
    init {
        "increaseAndGet" {
            val l = AtomicLong()
            should("work correctly") {
                l.increaseAndGet(10) shouldBe 10
                l.increaseAndGet(5) shouldBe 10
                l.get() shouldBe 10
                l.increaseAndGet(20) shouldBe 20
            }
        }
        "decreaseAndGet" {
            val l = AtomicLong(100)
            should("work correctly") {
                l.decreaseAndGet(10) shouldBe 10
                l.decreaseAndGet(25) shouldBe 10
                l.get() shouldBe 10
                l.decreaseAndGet(5) shouldBe 5
            }
        }
    }
}

