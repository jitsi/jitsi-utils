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
package org.jitsi.utils.stats;

import edu.umd.cs.findbugs.annotations.*;

/**
 * This originally comes from webrtc.org but has been moved here so that it can
 * be reused more easily.
 *
 * webrtc/modules/remote_bitrate_estimator/rate_statistics.cc
 * webrtc/modules/remote_bitrate_estimator/rate_statistics.h
 *
 * @author Lyubomir Marinov
 */
public class RateStatistics
{
    /**
     * Total count recorded in buckets.
     */
    private long accumulatedCount;

    /**
     * Counters are kept in buckets (circular buffer), with one bucket per
     * millisecond.
     */
    private final long[] buckets;

    /**
     * Bucket index of oldest counter recorded in buckets.
     */
    private int oldestIndex;

    /**
     * Oldest time recorded in buckets.
     */
    private long oldestTime;

    /**
     * To convert counts/ms to desired units.
     */
    private final float scale;

    /**
     * Initializes a new {@link RateStatistics} instance with a default scale
     * of 8000 (i.e. if the input is in bytes, the result will be in bits per
     * second).
     * @param windowSizeMs window size in ms for the rate estimation
     */
    public RateStatistics(int windowSizeMs)
    {
        this(windowSizeMs, 8000F);
    }

    /**
     * @param windowSizeMs window size in ms for the rate estimation
     * @param scale coefficient to convert counts/ms to desired units. For
     * example, if counts represents bytes, use <tt>8*1000</tt> to go to bits/s.
     */
    public RateStatistics(int windowSizeMs, float scale)
    { 
        buckets = new long[windowSizeMs + 1]; // N ms in (N+1) buckets.
        this.scale = scale / (buckets.length - 1);
    }

    private synchronized void eraseOld(long nowMs)
    {
        long newOldestTime = nowMs - buckets.length + 1;

        if (newOldestTime <= oldestTime)
            return;

        while (oldestTime < newOldestTime)
        {
            long countInOldestBucket = buckets[oldestIndex];

            accumulatedCount -= countInOldestBucket;
            buckets[oldestIndex] = 0L;
            if (++oldestIndex >= buckets.length)
            {
                oldestIndex = 0;
            }
            ++oldestTime;
            if (accumulatedCount == 0L)
            {
                // This guarantees we go through all the buckets at most once,
                // even if newOldestTime is far greater than oldestTime.
                break;
            }
        }
        oldestTime = newOldestTime;
    }

    public synchronized long getRate()
    {
        return getRate(System.currentTimeMillis());
    }

    public synchronized long getRate(long nowMs)
    {
        eraseOld(nowMs);
        return (long) (accumulatedCount * scale + 0.5F);
    }

    public synchronized long getAccumulatedCount()
    {
        return getAccumulatedCount(System.currentTimeMillis());
    }

    public synchronized long getAccumulatedCount(long nowMs)
    {
        eraseOld(nowMs);
        return accumulatedCount;
    }


    public synchronized void update(int count, long nowMs)
    {
        if (nowMs < oldestTime) // Too old data is ignored.
            return;

        eraseOld(nowMs);

        int nowOffset = (int) (nowMs - oldestTime);
        int index = oldestIndex + nowOffset;

        if (index >= buckets.length)
            index -= buckets.length;
        buckets[index] += count;
        accumulatedCount += count;
    }
}
