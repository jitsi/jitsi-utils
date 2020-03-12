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

import org.jitsi.utils.stats.RateStatistics
import org.json.simple.JSONObject
import java.util.Collections
import java.util.concurrent.atomic.LongAdder
import java.util.IdentityHashMap

class QueueStatistics
/**
 * Initializes a new [QueueStatistics] instance.
 */(
     /**
      * The queue being monitored
      */
     private val queue: PacketQueue<*>
 ) : PacketQueue.Observer {
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
     * The total queue length across all times packets were removed.
     */
    private val totalSize = LongAdder()

    /**
     * The total lengths of time that packets were waiting in the queue.
     */
    private val totalWait = LongAdder()

    /**
     * A map of the time when objects were put in the queue
     */
    private val insertionTime = Collections.synchronizedMap(IdentityHashMap<Any, Long>())

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    val stats: JSONObject
        get() {
            val stats = JSONObject()
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
            stats["average_queue_size_at_remove"] = totalSize.sum() / packetsRemoved
            stats["average_queue_wait_time_ms"] = totalWait.sum() / packetsRemoved
            return stats
        }

    /**
     * Registers the addition of a packet.
     */
    override fun added(o: Any) {
        val now = System.currentTimeMillis()
        if (firstPacketAddedMs < 0) {
            firstPacketAddedMs = now
        }
        addRate.update(1, now)
        totalPacketsAdded.increment()
        insertionTime[o] = now
    }

    /**
     * Registers the removal of a packet.
     */
    override fun removed(o: Any) {
        val now = System.currentTimeMillis()
        removeRate.update(1, now)
        totalPacketsRemoved.increment()
        totalSize.add(queue.size().toLong())
        val then = insertionTime.remove(o)
        if (then != null) {
            val wait = now - then
            totalWait.add(wait)
        }
    }

    /**
     * Registers that a packet was dropped.
     */
    override fun dropped(o: Any) {
        val now = System.currentTimeMillis()
        dropRate.update(1, now)
        totalPacketsDropped.increment()
        insertionTime.remove(o) /* TODO: track this time in stats? */
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
    }
}
