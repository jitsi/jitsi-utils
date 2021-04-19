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

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import org.jitsi.utils.OrderedJsonObject
import org.jitsi.utils.maxAssign
import java.lang.IllegalArgumentException

open class BucketStats(thresholdsNoMax: LongArray, val averageMaxLabel: String, val bucketLabel: String) {
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
    private val buckets = Buckets(thresholdsNoMax)

    fun addValue(value: Long) {
        if (value >= 0) {
            totalValue.add(value)
            maxValue.maxAssign(value)
            totalCount.increment()
            buckets.addValue(value)
        }
    }

    fun toJson() = OrderedJsonObject().apply {
        val snapshot = snapshot
        put("average$averageMaxLabel", snapshot.average)
        put("max$averageMaxLabel", snapshot.maxValue)
        put("total_count", snapshot.totalCount)

        put("buckets", getBucketsJson(snapshot.buckets))
    }

    val snapshot: Snapshot
        get() = Snapshot(
            average = average,
            maxValue = maxValue.get(),
            totalCount = totalCount.sum(),
            buckets = buckets.snapshot
        )

    data class Snapshot(
        val average: Double,
        val maxValue: Long,
        val totalCount: Long,
        val buckets: Buckets.Snapshot
    )

    fun getBucketsJson(b: Buckets.Snapshot) = OrderedJsonObject().apply {
        for (i in 0..b.buckets.size - 2) {
            put("<= ${b.buckets[i].first}$bucketLabel", b.buckets[i].second)
        }

        val indexOfSecondToLast = b.buckets.size - 2
        put("> ${b.buckets[indexOfSecondToLast].first}$bucketLabel", b.buckets.last().second)

        put("p99<=", b.p99bound)
        put("p999<=", b.p999bound)
    }
}

class Buckets(thresholdsNoMax: LongArray) {
    private val thresholds = longArrayOf(*thresholdsNoMax, Long.MAX_VALUE)
    private val thresholdCounts = Array(thresholds.size + 1) { LongAdder() }
    val snapshot: Snapshot
        get() {
            val bucketCounts = Array(thresholds.size) { i -> Pair(thresholds[i], thresholdCounts[i].sum()) }

            var p99 = Long.MAX_VALUE
            var p999 = Long.MAX_VALUE
            var sum: Long = 0
            val totalCount = bucketCounts.map { it.second }.sum()
            bucketCounts.forEach {
                sum += it.second
                if (it.first < p99 && sum > 0.99 * totalCount) p99 = it.first
                if (it.first < p999 && sum > 0.999 * totalCount) p999 = it.first
            }

            // Not enough data
            if (totalCount < 100 || p99 == Long.MAX_VALUE) p99 = -1
            if (totalCount < 1000 || p999 == Long.MAX_VALUE) p999 = -1
            return Snapshot(bucketCounts, p99, p999)
        }

    private fun findBucket(value: Long): LongAdder {
        // The vast majority of values are in the first bucket, so linear search is likely faster than binary.
        for (i in thresholds.indices) {
            if (value <= thresholds[i]) return thresholdCounts[i]
        }
        return thresholdCounts.last()
    }

    fun addValue(value: Long) = findBucket(value).increment()

    data class Snapshot(
        val buckets: Array<Pair<Long, Long>>,
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
