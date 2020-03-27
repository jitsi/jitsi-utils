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

import org.jitsi.utils.OrderedJsonObject
import org.jitsi.utils.stats.BucketStats
import org.jitsi.utils.stats.RateStatistics
import org.json.simple.JSONObject
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.concurrent.atomic.LongAdder
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

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
    private val queueWaitStats = if (QueueObserver.trackTimes) BucketStats(waitBucketSizes, "_queue_wait_time_ms", " ms") else null

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    val stats: OrderedJsonObject
        get() {
            val stats = OrderedJsonObject()
            val now = clock.instant()
            stats["added"] = totalPacketsAdded.sum()
            stats["removed"] = totalPacketsRemoved.sum()
            stats["dropped"] = totalPacketsDropped.sum()
            val duration = Duration.between(firstPacketAdded, now)
            val duration_s = duration.toNanos() / 1e9
            stats["duration_s"] = duration_s
            val packetsRemoved = totalPacketsRemoved.sum().toDouble()
            stats["average_remove_rate_pps"] = packetsRemoved / duration_s
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
        /**
         * The scale to use for [RateStatistics]. 1000 means units
         * (e.g. packets) per second.
         */
        private const val SCALE = 1000

        /**
         * The interval for which to calculate rates in milliseconds.
         */
        private const val INTERVAL_MS = 5000

        /**
         * Calculate the statistics buckets for a given queue capacity.
         */
        private fun getQueueLengthBucketSizes(capacity: Int): LongArray {
            val list = ArrayList<Long>()
            list.add(0L)
            var i = 1L

            while (i < capacity) {
                list.add(i)
                i *= 4
            }
            val half = (capacity / 2).toLong()
            if (half > list.last()) {
                list.add(half)
            }

            val threeQuarters = (capacity * 3 / 4).toLong()
            if (threeQuarters > list.last()) {
                list.add(threeQuarters)
            }

            return list.toLongArray()
        }

        /**
         * The queue waiting time bucket sizes.
         */
        private val waitBucketSizes = longArrayOf(2, 5, 20, 50, 200, 500, 1000)
    }
}

class QueueObserver
@JvmOverloads
constructor(
    /**
     * The queue being monitored
     */
    private val queue: PacketQueue<*>,
    private val clock: Clock = Clock.systemUTC()
) : PacketQueue.Observer {
    /**
     * A map of the time when objects were put in the queue
     */
    private val insertionTime = if (trackTimes) Collections.synchronizedMap(IdentityHashMap<Any, Instant>()) else null

    private val localStats = if (debug) QueueStatistics(queue.capacity(), clock) else null
    private val globalStats = globalStatsFor(queue, clock)

    override fun added(o: Any) {
        insertionTime?.put(o, clock.instant())

        localStats?.added()
        globalStats.added()
    }

    /**
     * Registers the removal of a packet.
     */
    override fun removed(o: Any) {
        val queueLength = queue.size()
        val wait = insertionTime?.get(o)?.run {
            val now = clock.instant()
            Duration.between(this, now)
        }

        localStats?.removed(queueLength, wait)
        globalStats.removed(queueLength, wait)
    }

    /**
     * Registers that a packet was dropped.
     */
    override fun dropped(o: Any) {
        insertionTime?.remove(o) /* TODO: track this time in stats? */

        localStats?.dropped()
        globalStats.dropped()
    }

    val stats: OrderedJsonObject?
        get() = localStats?.stats

    val queueDebugState: JSONObject
        get() {
            val d = queue.debugState
            stats?.let { d["statistics"] = it }
            return d
        }

    companion object {
        var debug = false
        var trackTimes = false

        private val queueStatsById = ConcurrentHashMap<String, QueueStatistics>()

        private fun globalStatsFor(queue: PacketQueue<*>, clock: Clock) = queueStatsById.computeIfAbsent(queue.id()) {
            /* Assume all queues with the same ID have the same capacity and can use the same size buckets. */
            QueueStatistics(queue.capacity(), clock)
        }

        fun getStatistics(): OrderedJsonObject {
            val stats = OrderedJsonObject()
            with(stats) {
                for (entry in queueStatsById.entries) {
                    put(entry.key, entry.value.stats)
                }
            }
            return stats
        }
    }
}
