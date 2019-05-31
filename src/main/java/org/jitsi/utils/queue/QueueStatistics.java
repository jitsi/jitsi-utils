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
package org.jitsi.utils.queue;

import org.jitsi.utils.stats.*;
import org.json.simple.*;

import java.util.concurrent.atomic.*;

public class QueueStatistics
{
    /**
     * The scale to use for {@link RateStatistics}. 1000 means units
     * (e.g. packets) per second.
     */
    private static final int SCALE = 1000;

    /**
     * The interval for which to calculate rates in milliseconds.
     */
    private static final int INTERVAL_MS = 5000;

    /**
     * Rate of addition of packets in pps.
     */
    private final RateStatistics addRate
            = new RateStatistics(INTERVAL_MS, SCALE);

    /**
     * Rate of removal of packets in pps.
     */
    private final RateStatistics removeRate
            = new RateStatistics(INTERVAL_MS, SCALE);

    /**
     * Rate of packets being dropped in pps.
     */
    private final RateStatistics dropRate
            = new RateStatistics(INTERVAL_MS, SCALE);

    /**
     * Total packets added to the queue.
     */
    private AtomicInteger totalPacketsAdded = new AtomicInteger();

    /**
     * Total packets removed to the queue.
     */
    private AtomicInteger totalPacketsRemoved = new AtomicInteger();

    /**
     * Total packets dropped from the queue.
     */
    private AtomicInteger totalPacketsDropped = new AtomicInteger();

    /**
     * The time the first packet was added.
     */
    private long firstPacketAddedMs = -1;

    /**
     * Initializes a new {@link QueueStatistics} instance.
     * 
     * @param id Identifier to distinguish the log output of multiple
     *            {@link QueueStatistics} instances.
     */
    public QueueStatistics()
    {
    }

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    public JSONObject getStats()
    {
        JSONObject stats = new JSONObject();
        long now = System.currentTimeMillis();
        stats.put("added", totalPacketsAdded.get());
        stats.put("removed", totalPacketsRemoved.get());
        stats.put("dropped", totalPacketsDropped.get());
        stats.put("add_rate", addRate.getRate(now));
        stats.put("remove_rate", removeRate.getRate(now));
        stats.put("drop_rate", dropRate.getRate(now));
        double duration = (now - firstPacketAddedMs) / 1000d;
        stats.put("duration_s", duration);
        stats.put("average_remove_rate_pps", totalPacketsRemoved.get() / duration);

        return stats;
    }

    /**
     * Registers the addition of a packet.
     * @param now the time (in milliseconds since the epoch) at which the
     * packet was added.
     */
    void add(long now)
    {
        if (firstPacketAddedMs < 0)
        {
            firstPacketAddedMs = now;
        }
        addRate.update(1, now);
        totalPacketsAdded.incrementAndGet();
    }

    /**
     * Registers the removal of a packet.
     * @param now the time (in milliseconds since the epoch) at which the
     * packet was removed.
     */
    void remove(long now)
    {
        removeRate.update(1, now);
        totalPacketsRemoved.incrementAndGet();
    }

    /**
     * Registers that a packet was dropped.
     * @param now the time (in milliseconds since the epoch) at which the
     * packet was dropped.
     */
    void drop(long now)
    {
        dropRate.update(1, now);
        totalPacketsDropped.incrementAndGet();
    }

}
