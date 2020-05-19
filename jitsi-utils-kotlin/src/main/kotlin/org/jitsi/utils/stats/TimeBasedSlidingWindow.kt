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

package org.jitsi.utils.stats

import java.time.Clock
import java.time.Duration
import java.time.Instant

open class TimeBasedSlidingWindow<T : Any>(
    private val windowSize: Duration,
    private val evictionHandler: (T) -> Unit = {},
    private val clock: Clock = Clock.systemUTC()
) {
    private val queue = java.util.ArrayDeque<TimeEntry>()

    private val evictionPredicate: (TimeEntry) -> Boolean = { entry ->
        Duration.between(entry.insertionTime, clock.instant()) > windowSize
    }

    fun add(value: T) {
        queue.addFirst(TimeEntry(value))
        evict()
    }

    fun values(): Collection<T> = queue.reversed().map { it.value }

    private fun evict() {
        while (evictionPredicate(queue.last)) {
            evictionHandler(queue.last.value)
            queue.removeLast()
        }
    }

    inner class TimeEntry(
        val value: T,
        val insertionTime: Instant
    ) {
        constructor(value: T) : this(value, clock.instant())
    }
}

class SlidingWindowAverage(
    windowSize: Duration,
    clock: Clock = Clock.systemUTC()
) {
    private val slidingWindow = TimeBasedSlidingWindow(windowSize, this::onEviction, clock)
    private var currSum: Long = 0
    private var numElements = 0

    /**
     * Get the average of the values currently contained within the sliding
     * window.
     */
    @Synchronized fun get(): Double {
        if (numElements == 0) {
            return 0.0
        }
        return currSum / numElements.toDouble()
    }

    @Synchronized fun add(value: Long) {
        slidingWindow.add(value)
        currSum += value
        numElements++
    }

    @Synchronized private fun onEviction(value: Long) {
        currSum -= value
        numElements--
    }
}

fun main() {
    val clock = FakeClock()
    val windowedAverage = SlidingWindowAverage(
        Duration.ofSeconds(5),
        clock
    )
    clock.elapse(Duration.ofSeconds(1))
    repeat (10) {
        windowedAverage.add(it.toLong())
        clock.elapse(Duration.ofSeconds(1))
    }
    println(windowedAverage)

    println(windowedAverage.get())
}
