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
package org.jitsi.utils.queue

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.jitsi.utils.OrderedJsonObject
import org.jitsi.utils.stats.BucketStats
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder
import kotlin.collections.ArrayList

class QueueStatistics(queueSize: Int, val clock: Clock) {
    /**
     * Total packets added to the queue.
     */
    private val totalPacketsAdded = LongAdder()

    /**
     * Total packets removed to the queue.
     */
    private val totalPacketsRemoved = LongAdder()

    /**
     * Total packets dropped from the queue.
     */
    private val totalPacketsDropped = LongAdder()

    /**
     * The time the first packet was added.
     */
    private var firstPacketAdded: Instant? = null

    /**
     * Statistics about queue lengths
     */
    private val queueLengthStats = BucketStats(getQueueLengthBucketSizes(queueSize), "_queue_size_at_remove", "")

    /**
     * Statistics about the time that packets were waiting in the queue.
     */
    private val queueWaitStats = if (TRACK_TIMES) BucketStats(waitBucketSizes, "_queue_wait_time_ms", " ms") else null

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    val stats: OrderedJsonObject
        get() {
            val stats = OrderedJsonObject()
            val now = clock.instant()
            stats["added_packets"] = totalPacketsAdded.sum()
            stats["removed_packets"] = totalPacketsRemoved.sum()
            stats["dropped_packets"] = totalPacketsDropped.sum()
            if (firstPacketAdded != null) {
                val duration = Duration.between(firstPacketAdded, now)
                val duration_s = duration.toNanos() / 1e9
                stats["duration_s"] = duration_s
                val packetsRemoved = totalPacketsRemoved.sum().toDouble()
                stats["average_remove_rate_pps"] = packetsRemoved / duration_s
            }
            stats["queue_size_at_remove"] = queueLengthStats.toJson()
            queueWaitStats?.let { stats["queue_wait_time"] = it.toJson() }
            return stats
        }

    /**
     * Registers the addition of a packet.
     */
    fun added() {
        if (firstPacketAdded == null) {
            firstPacketAdded = clock.instant()
        }
        totalPacketsAdded.increment()
    }

    /**
     * Registers the removal of a packet.
     */
    fun removed(queueSize: Int, waitTime: Duration?) {
        totalPacketsRemoved.increment()
        queueLengthStats.addValue(queueSize.toLong())
        if (waitTime != null) {
            queueWaitStats?.addValue(waitTime.toMillis()) /* TODO: measure in nanos? */
        }
    }

    /**
     * Registers that a packet was dropped.
     */
    fun dropped() {
        totalPacketsDropped.increment()
    }

    companion object {
        /** Whether specific per-queue statistics should be kept. */
        @JvmField
        @field:SuppressFBWarnings("MS_SHOULD_BE_FINAL")
        var DEBUG = false

        /** Whether queue dwell times should be tracked. */
        @JvmField
        @field:SuppressFBWarnings("MS_SHOULD_BE_FINAL")
        var TRACK_TIMES = false

        private val queueStatsById = ConcurrentHashMap<String, QueueStatistics>()

        internal fun globalStatsFor(queue: PacketQueue<*>, clock: Clock) = queueStatsById.computeIfAbsent(queue.id()) {
            /* Assume all queues with the same ID have the same capacity and can use the same size buckets. */
            QueueStatistics(queue.capacity(), clock)
        }

        fun getStatistics() = OrderedJsonObject().apply { queueStatsById.entries.forEach { put(it.key, it.value.stats) } }

        /**
         * Calculate the capacity statistics buckets for a given queue capacity.
         */
        private fun getQueueLengthBucketSizes(capacity: Int): List<Long> {
            val boundedCapacity = capacity.coerceAtMost(16384)
            val list = ArrayList<Long>()
            list.add(0L)
            var i = 1L

            while (i < boundedCapacity) {
                list.add(i)
                i *= 4
            }
            if (capacity == boundedCapacity) {
                val half = (capacity / 2).toLong()
                if (half > list.last()) {
                    list.add(half)
                }

                val threeQuarters = (capacity * 3 / 4).toLong()
                if (threeQuarters > list.last()) {
                    list.add(threeQuarters)
                }
            } else {
                list.add(boundedCapacity.toLong())
            }

            list.add(Long.MAX_VALUE)

            return list
        }

        /**
         * The queue waiting time bucket sizes.
         */
        private val waitBucketSizes = listOf(0, 2L, 5, 20, 50, 200, 500, 1000, Long.MAX_VALUE)
    }
}

class QueueStatisticsObserver<T>(
    val queue: PacketQueue<T>,
    val clock: Clock
) : PacketQueue.Observer<T> {
    /**
     * A map of the time when objects were put in the queue
     */
    private val insertionTime = if (QueueStatistics.TRACK_TIMES) Collections.synchronizedMap(IdentityHashMap<Any, Instant>()) else null

    private val localStats = if (QueueStatistics.DEBUG) QueueStatistics(queue.capacity(), clock) else null
    private val globalStats = QueueStatistics.globalStatsFor(queue, clock)

    /** Calling [PacketQueue.size] turns out to cause surprising amounts of thread contention,
     * so mirror the queue size here.
     */
    private val queueSize = AtomicInteger(0)

    override fun added(pkt: T) {
        queueSize.incrementAndGet()
        insertionTime?.put(pkt, clock.instant())

        localStats?.added()
        globalStats.added()
    }

    /**
     * Registers the removal of a packet.
     */
    override fun removed(pkt: T) {
        val queueLength = queueSize.decrementAndGet()
        val wait = insertionTime?.get(pkt)?.run {
            val now = clock.instant()
            Duration.between(this, now)
        }

        localStats?.removed(queueLength, wait)
        globalStats.removed(queueLength, wait)
    }

    /**
     * Registers that a packet was dropped.
     */
    override fun dropped(pkt: T) {
        queueSize.decrementAndGet()
        insertionTime?.remove(pkt) /* TODO: track this time in stats? */

        localStats?.dropped()
        globalStats.dropped()
    }

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    override fun getStats(): OrderedJsonObject? = localStats?.stats
}
