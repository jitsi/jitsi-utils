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
import java.util.Collections
import java.util.concurrent.atomic.LongAdder
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

open class QueueStatisticsBase(queueSize: Int) {
    /**
     * Rate of addition of packets in pps.
     */
    private val addRate = RateStatistics(INTERVAL_MS, SCALE.toFloat())

    /**
     * Rate of removal of packets in pps.
     */
    private val removeRate = RateStatistics(INTERVAL_MS, SCALE.toFloat())

    /**
     * Rate of packets being dropped in pps.
     */
    private val dropRate = RateStatistics(INTERVAL_MS, SCALE.toFloat())

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
    private var firstPacketAddedMs: Long = -1

    /**
     * Statistics about queue lengths
     */
    private val queueLengthStats = BucketStats(lengthBucketSizes(queueSize), "_queue_size_at_remove", "")

    /**
     * Statistics about the time that packets were waiting in the queue.
     */
    private val queueWaitStats = BucketStats(waitBucketSizes, "_queue_wait_time_ms", " ms")

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    val stats: OrderedJsonObject
        get() {
            val stats = OrderedJsonObject()
            val now = System.currentTimeMillis()
            stats["added"] = totalPacketsAdded.sum()
            stats["removed"] = totalPacketsRemoved.sum()
            stats["dropped"] = totalPacketsDropped.sum()
            stats["add_rate"] = addRate.getRate(now)
            stats["remove_rate"] = removeRate.getRate(now)
            stats["drop_rate"] = dropRate.getRate(now)
            val duration = (now - firstPacketAddedMs) / 1000.0
            stats["duration_s"] = duration
            val packetsRemoved = totalPacketsRemoved.sum().toDouble()
            stats["average_remove_rate_pps"] = packetsRemoved / duration
            stats["queue_size_at_remove"] = queueLengthStats.toJson()
            stats["queue_wait_time"] = queueWaitStats.toJson()
            return stats
        }

    /**
     * Registers the addition of a packet.
     */
    fun added(now: Long) {
        if (firstPacketAddedMs < 0) {
            firstPacketAddedMs = now
        }
        addRate.update(1, now)
        totalPacketsAdded.increment()
    }

    /**
     * Registers the removal of a packet.
     */
    fun removed(now: Long, queueSize: Int, waitTime: Long?) {
        totalPacketsRemoved.increment()
        queueLengthStats.addValue(queueSize.toLong())
        if (waitTime != null) {
            queueWaitStats.addValue(waitTime)
        }
    }

    /**
     * Registers that a packet was dropped.
     */
    fun dropped(now: Long) {
        dropRate.update(1, now)
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
         * Calculate the statistics buckets for a given queue length
         */
        private fun lengthBucketSizes(size: Int): LongArray {
            val list = ArrayList<Long>()
            list.add(0L)
            var i = 1L

            while (i < size) {
                list.add(i)
                i *= 4
            }
            val half = (size / 2).toLong()
            if (half > list.last()) {
                list.add(half)
            }

            val threeQuarters = (size * 3 / 4).toLong()
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

class QueueStatistics(
    /**
     * The queue being monitored
     */
    private val queue: PacketQueue<*>
) : QueueStatisticsBase(queue.capacity()), PacketQueue.Observer {
    /**
     * A map of the time when objects were put in the queue
     */
    private val insertionTime = Collections.synchronizedMap(IdentityHashMap<Any, Long>())

    override fun added(o: Any) {
        val now = System.currentTimeMillis()
        insertionTime[o] = now
        super.added(now)
        globalStatsFor(queue).added(now)
    }

    /**
     * Registers the removal of a packet.
     */
    override fun removed(o: Any) {
        val now = System.currentTimeMillis()
        val queueLength = queue.size()
        val then = insertionTime.remove(o)
        val wait = if (then != null) { now - then } else null
        super.removed(now, queueLength, wait)
        globalStatsFor(queue).removed(now, queueLength, wait)
    }

    /**
     * Registers that a packet was dropped.
     */
    override fun dropped(o: Any) {
        val now = System.currentTimeMillis()
        insertionTime.remove(o) /* TODO: track this time in stats? */
        super.dropped(now)
        globalStatsFor(queue).dropped(now)
    }

    companion object {
        val queueStatsById = ConcurrentHashMap<String, QueueStatisticsBase>()

        fun globalStatsFor(queue: PacketQueue<*>) = queueStatsById.computeIfAbsent(queue.id()) {
            /* Assume all queues with the same ID have the same capacity and can use the same size buckets. */
            QueueStatisticsBase(queue.capacity())
        }

        fun getStatistics(): OrderedJsonObject {
            val stats = OrderedJsonObject()
            with(stats) {
                synchronized(queueStatsById) {
                    for (entry in queueStatsById.entries) {
                        put(entry.key, entry.value.stats)
                    }
                }
            }
            return stats
        }
    }
}
