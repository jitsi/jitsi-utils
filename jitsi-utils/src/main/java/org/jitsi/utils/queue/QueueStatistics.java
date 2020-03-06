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

import java.util.*;
import java.util.concurrent.atomic.*;

public class QueueStatistics implements PacketQueue.Observer
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
    private LongAdder totalPacketsAdded = new LongAdder();

    /**
     * Total packets removed to the queue.
     */
    private LongAdder totalPacketsRemoved = new LongAdder();

    /**
     * Total packets dropped from the queue.
     */
    private LongAdder totalPacketsDropped = new LongAdder();

    /**
     * The time the first packet was added.
     */
    private long firstPacketAddedMs = -1;

    /**
     * The total queue length across all times packets were removed.
     */
    private LongAdder totalSize = new LongAdder();

    /**
     * The total lengths of time that packets were waiting in the queue.
     */
    private LongAdder totalWait = new LongAdder();

    /**
     * The queue being monitored
     */
    private PacketQueue<?> queue;

    /**
     * A map of the time when objects were put in the queue
     */
    private final Map<Object, Long> insertionTime =
        Collections.synchronizedMap(new IdentityHashMap<>());

    /**
     * Initializes a new {@link QueueStatistics} instance.
     */
    public QueueStatistics(PacketQueue<?> q)
    {
        queue = q;
    }

    /**
     * Gets a snapshot of the stats in JSON format.
     */
    public JSONObject getStats()
    {
        JSONObject stats = new JSONObject();
        long now = System.currentTimeMillis();
        stats.put("added", totalPacketsAdded.sum());
        stats.put("removed", totalPacketsRemoved.sum());
        stats.put("dropped", totalPacketsDropped.sum());
        stats.put("add_rate", addRate.getRate(now));
        stats.put("remove_rate", removeRate.getRate(now));
        stats.put("drop_rate", dropRate.getRate(now));
        double duration = (now - firstPacketAddedMs) / 1000d;
        stats.put("duration_s", duration);
        stats.put("average_remove_rate_pps", totalPacketsRemoved.sum() / duration);
        stats.put("average_queue_size_at_remove", totalSize.sum() / (double)totalPacketsRemoved.sum());
        stats.put("average_queue_wait_time", totalWait.sum() / (double)totalPacketsRemoved.sum());

        return stats;
    }

    /**
     * Registers the addition of a packet.
     */
    @Override
    public void added(Object o)
    {
        long now = System.currentTimeMillis();
        if (firstPacketAddedMs < 0)
        {
            firstPacketAddedMs = now;
        }
        addRate.update(1, now);
        totalPacketsAdded.increment();
        insertionTime.put(o, now);
    }

    /**
     * Registers the removal of a packet.
     */
    @Override
    public void removed(Object o)
    {
        long now = System.currentTimeMillis();
        removeRate.update(1, now);
        totalPacketsRemoved.increment();
        totalSize.add(queue.size());

        Long then = insertionTime.remove(o);
        if (then != null) {
            long wait = now - then;
            totalWait.add(wait);
        }
    }

    /**
     * Registers that a packet was dropped.
     */
    @Override
    public void dropped(Object o)
    {
        long now = System.currentTimeMillis();
        dropRate.update(1, now);
        totalPacketsDropped.increment();
        insertionTime.remove(o); /* TODO: track this time in stats? */
    }

}
