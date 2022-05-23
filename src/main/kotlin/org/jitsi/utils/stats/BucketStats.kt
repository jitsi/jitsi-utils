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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.jitsi.utils.OrderedJsonObject
import org.jitsi.utils.maxAssign
import org.jitsi.utils.minAssign
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder

@SuppressFBWarnings(value = ["SF_SWITCH_NO_DEFAULT"], justification = "False positive with kotlin's 'when'.")
open class BucketStats(thresholdsNoMax: LongArray, val averageMaxLabel: String = "", val bucketLabel: String = "") {
    init {
        if (!thresholdsNoMax.contentEquals(thresholdsNoMax.sortedArray())) {
            throw IllegalArgumentException("Thresholds must be sorted: ${thresholdsNoMax.joinToString()}")
        }
    }
    private val totalValue = LongAdder()
    private val totalCount = LongAdder()
    private val average: Double
        get() = totalValue.sum() / totalCount.sum().toDouble()
    private val maxValue = AtomicLong(0)
    private val minValue = AtomicLong(0)
    private val buckets = Buckets(thresholdsNoMax)

    fun addValue(value: Long) {
        totalValue.add(value)
        maxValue.maxAssign(value)
        minValue.minAssign(value)
        totalCount.increment()
        buckets.addValue(value)
    }

    open fun toJson(format: Format = Format.Separate) = OrderedJsonObject().apply {
        val snapshot = snapshot
        put("average$averageMaxLabel", snapshot.average)
        put("max$averageMaxLabel", snapshot.maxValue)
        put("min$averageMaxLabel", snapshot.minValue)
        put("total_value", snapshot.totalValue)
        put("total_count", snapshot.totalCount)

        put("buckets", getBucketsJson(snapshot.buckets, format))
    }

    val snapshot: Snapshot
        get() = Snapshot(
            average = average,
            maxValue = maxValue.get(),
            minValue = minValue.get(),
            totalValue = totalValue.sum(),
            totalCount = totalCount.sum(),
            buckets = buckets.snapshot
        )

    data class Snapshot(
        val average: Double,
        val maxValue: Long,
        val minValue: Long,
        val totalValue: Long,
        val totalCount: Long,
        val buckets: Buckets.Snapshot
    )

    open fun getBucketsJson(b: Buckets.Snapshot, format: Format) = OrderedJsonObject().apply {
        when (format) {
            Format.Separate -> b.buckets.forEach {
                val f = if (it.first.first == Long.MIN_VALUE) "min" else "${it.first.first}"
                val s = if (it.first.second == Long.MAX_VALUE) "max" else "${it.first.second}"
                val key = "${f}_to_$s"
                this["$key$bucketLabel"] = it.second
            }
            Format.CumulativeLeft -> {
                var sum = 0L
                b.buckets.forEach {
                    if (it.first.second != Long.MAX_VALUE) {
                        sum += it.second
                        val key = "min_to_${it.first.second}"
                        this["$key$bucketLabel"] = sum
                    }
                }
            }
            Format.CumulativeRight -> {
                var sum = 0L
                b.buckets.reversed().forEach {
                    if (it.first.first != Long.MIN_VALUE) {
                        sum += it.second
                        val key = "${it.first.first}_to_max"
                        this["$key$bucketLabel"] = sum
                    }
                }
            }
        }

        put("p99_upper_bound", b.p99bound)
        put("p999_upper_bound", b.p999bound)
    }

    /** How to format the JSON output. */
    enum class Format {
        /** Include individual buckets, e.g. (min, 0], (0, 10], (10, 20], (20, max) */
        Separate,
        /** Combine buckets summing from the left, e.g. (min, 0], (min, 10], (min, 20] */
        CumulativeLeft,
        /** Combine buckets summing from the right, e.g. (0, max), (10, max), (20, max) */
        CumulativeRight
    }
}

class Buckets(thresholdsNoMax: LongArray) {
    private val thresholds = longArrayOf(Long.MIN_VALUE, *thresholdsNoMax, Long.MAX_VALUE)
    // The bucket for (thresholds[i], thresholds[i+1]].
    private val thresholdCounts = Array(thresholds.size - 1) { LongAdder() }
    val snapshot: Snapshot
        get() {
            val bucketCounts = Array(thresholdCounts.size) {
                i ->
                Pair(Pair(thresholds[i], thresholds[i + 1]), thresholdCounts[i].sum())
            }

            var p99 = Long.MAX_VALUE
            var p999 = Long.MAX_VALUE
            var sum: Long = 0
            val totalCount = bucketCounts.sumOf { it.second }
            bucketCounts.forEach {
                sum += it.second
                if (it.first.second < p99 && sum > 0.99 * totalCount) p99 = it.first.second
                if (it.first.second < p999 && sum > 0.999 * totalCount) p999 = it.first.second
            }

            // Not enough data
            if (totalCount < 100 || p99 == Long.MAX_VALUE) p99 = -1
            if (totalCount < 1000 || p999 == Long.MAX_VALUE) p999 = -1
            return Snapshot(bucketCounts, p99, p999)
        }

    private fun findBucket(value: Long): LongAdder {
        // The vast majority of values are in the first bucket, so linear search is likely faster than binary.
        // Subclasses can override this if necessary.
        for (i in thresholdCounts.indices) {
            if (value <= thresholds[i + 1]) return thresholdCounts[i]
        }
        return thresholdCounts.last()
    }

    fun addValue(value: Long) = findBucket(value).increment()

    data class Snapshot(
        val buckets: Array<Pair<Pair<Long, Long>, Long>>,
        val p99bound: Long,
        val p999bound: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Snapshot

            if (!buckets.contentEquals(other.buckets)) return false

            return true
        }

        override fun hashCode(): Int {
            return buckets.contentHashCode()
        }
    }
}
