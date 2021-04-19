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
import java.util.concurrent.atomic.AtomicInteger

class QueueStatistics {
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
    private val totalPacketsAdded = AtomicInteger()

    /**
     * Total packets removed to the queue.
     */
    private val totalPacketsRemoved = AtomicInteger()

    /**
     * Total packets dropped from the queue.
     */
    private val totalPacketsDropped = AtomicInteger()

    /**
     * The time the first packet was added.
     */
    private var firstPacketAddedMs: Long = -1

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    val stats: JSONObject
        get() {
            val stats = JSONObject()
            val now = System.currentTimeMillis()
            stats["added"] = totalPacketsAdded.get()
            stats["removed"] = totalPacketsRemoved.get()
            stats["dropped"] = totalPacketsDropped.get()
            stats["add_rate"] = addRate.getRate(now)
            stats["remove_rate"] = removeRate.getRate(now)
            stats["drop_rate"] = dropRate.getRate(now)
            val duration = (now - firstPacketAddedMs) / 1000.0
            stats["duration_s"] = duration
            stats["average_remove_rate_pps"] =
                totalPacketsRemoved.get() / duration
            return stats
        }

    /**
     * Registers the addition of a packet.
     * @param now the time (in milliseconds since the epoch) at which the
     * packet was added.
     */
    fun add(now: Long) {
        if (firstPacketAddedMs < 0) {
            firstPacketAddedMs = now
        }
        addRate.update(1, now)
        totalPacketsAdded.incrementAndGet()
    }

    /**
     * Registers the removal of a packet.
     * @param now the time (in milliseconds since the epoch) at which the
     * packet was removed.
     */
    fun remove(now: Long) {
        removeRate.update(1, now)
        totalPacketsRemoved.incrementAndGet()
    }

    /**
     * Registers that a packet was dropped.
     * @param now the time (in milliseconds since the epoch) at which the
     * packet was dropped.
     */
    fun drop(now: Long) {
        dropRate.update(1, now)
        totalPacketsDropped.incrementAndGet()
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
