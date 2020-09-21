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
package org.jitsi.utils.stats

import org.jitsi.utils.ms
import java.lang.IllegalArgumentException
import java.time.Clock
import java.time.Duration

/**
 * Tracks an average rate (of values added via [update]) over a sliding window. The data is kept in a circular buffer
 * of buckets with a configurable size, which can be efficient in both CPU and memory use. Useful for e.g. calculating
 * the bitrate or packet rate of a stream.
 */
open class RateTracker @JvmOverloads constructor(
    /**
     * The number of buckets.
     */
    private val numBuckets: Int,
    /**
     * The size of each bucket in milliseconds.
     */
    private val bucketSize: Duration = 1.ms,
    private val clock: Clock = Clock.systemUTC()
) {
    constructor(
        /**
         * The duration of the window for which values will be kept.
         */
        windowSize: Duration,
        /**
         * The duration of each bucket in the window. This must divide [windowSize] evenly.
         */
        bucketSize: Duration = 1.ms,
        clock: Clock = Clock.systemUTC()) : this(
        numBuckets = (windowSize.toMillis() / bucketSize.toMillis()).toInt(),
        bucketSize = bucketSize,
        clock = clock) {

        if (bucketSize.toMillis() * numBuckets.toLong() != windowSize.toMillis()) {
            throw IllegalArgumentException(
                "The bucketSize (${bucketSize.toMillis()} ms) must divide the window size " +
                    "(${windowSize.toMillis()} ms) evenly.")
        }
    }
    /**
     * Total count recorded in buckets.
     */
    private var accumulatedCount: Long = 0

    /**
     * Counters are kept in buckets (circular buffer), with one bucket per [bucketSizeMs] milliseconds.
     */
    private val buckets: LongArray = LongArray(numBuckets + 1)

    /**
     * Bucket index of oldest counter recorded in buckets.
     */
    private var oldestIndex = 0

    /**
     * Oldest time recorded in buckets. One time tick corresponds to [bucketSizeMs] milliseconds of time on the [clock].
     */
    private var oldestTime: Long = 0

    /**
     * The size of the window in milliseconds.
     */
    private val windowSizeMs = numBuckets * bucketSize.toMillis()

    /**
     * The size of a singe bucket in milliseconds.
     */
    private val bucketSizeMs = bucketSize.toMillis()

    /**
     * Convert a [clock] time to the local time representation (one tick is [bucketSizeMs] milliseconds).
     */
    private fun coerceMs(timeMs: Long) = timeMs / bucketSizeMs

    @Synchronized
    private fun eraseOld(
        /**
         * The timestamp in ticks of [bucketSizeMs].
         */
        now: Long
    ) {
        val newOldestTime = now - buckets.size + 1
        if (newOldestTime <= oldestTime) return
        while (oldestTime < newOldestTime) {
            val countInOldestBucket = buckets[oldestIndex]
            accumulatedCount -= countInOldestBucket
            buckets[oldestIndex] = 0L
            if (++oldestIndex >= buckets.size) {
                oldestIndex = 0
            }
            ++oldestTime
            if (accumulatedCount == 0L) {
                // This guarantees we go through all the buckets at most once,
                // even if newOldestTime is far greater than oldestTime.
                break
            }
        }
        oldestTime = newOldestTime
    }

    /**
     * Get the rate in units per second.
     */
    @Synchronized
    open fun getRate(nowMs: Long): Long {
        eraseOld(coerceMs(nowMs))
        return (accumulatedCount.toDouble() * 1000 / windowSizeMs + 0.5f).toLong()
    }
    val rate: Long
        get() = getRate(clock.millis())

    @Synchronized
    @JvmOverloads
    fun getAccumulatedCount(nowMs: Long = clock.millis()): Long {
        eraseOld(coerceMs(nowMs))
        return accumulatedCount
    }

    @Synchronized
    @JvmOverloads
    fun update(count: Long = 1, nowMs: Long = clock.millis()) {
        val now = coerceMs(nowMs)
        if (now < oldestTime) // Too old data is ignored.
            return
        eraseOld(now)
        val nowOffset = (now - oldestTime).toInt()
        var index = oldestIndex + nowOffset
        if (index >= buckets.size) index -= buckets.size
        buckets[index] = buckets[index] + count
        accumulatedCount += count
    }
}

/**
 * Keep this for backward compatibility before we migrate away from it.
 */
open class RateStatistics @JvmOverloads constructor(
    windowSizeMs: Int = 1000,
    scale: Float = 8000f,
    val clock: Clock = Clock.systemUTC()) {

    val tracker = RateTracker(windowSizeMs, 1.ms, clock)
    val scale = scale / (windowSizeMs - 1)

    val rate: Long
        get() = getRate(clock.millis())
    open fun getRate(nowMs: Long = clock.millis()) = (tracker.getRate(nowMs) * scale + 0.5).toLong()

    @JvmOverloads
    fun getAccumulatedCount(nowMs: Long = clock.millis()) = tracker.getAccumulatedCount(nowMs)

    @JvmOverloads
    fun update(count: Int = 1, nowMs: Long = clock.millis()) = tracker.update(count.toLong(), nowMs)
}
