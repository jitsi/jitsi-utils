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

/**
 * Maintain a queue of values which fall inside the given window size.
 *
 * Eviction is done after adding an element in [add], and can be forced
 * via [forceEviction].  Any time an element is evicted, the [evictionHandler]
 * method is called with the evicted value.  Forcing eviction before any access
 * is done is required, as it's possible there are stale values in the collection.
 */
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

    /**
     * Force the eviction process to run.  It's important this be invoked
     * if there's a chance values have gone out of the window since the last
     * call to [add].
     */
    fun forceEviction() = evict()

    fun values(): Collection<T> = queue.reversed().map { it.value }

    private fun evict() {
        while (queue.isNotEmpty() && evictionPredicate(queue.last)) {
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
