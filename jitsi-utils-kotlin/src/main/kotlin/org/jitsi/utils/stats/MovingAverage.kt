/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.utils.stats

import java.time.Clock
import java.time.Duration

/**
 * Keeps track of a moving average of the values added which fall within the
 * given window size.
 */
class MovingAverage<T : Number>(
    windowSize: Duration,
    clock: Clock = Clock.systemUTC()
) {
    private val slidingWindow = TimeBasedSlidingWindow(windowSize, this::onEviction, clock)
    private var currSum: Long = 0
    private var numElements = 0

    /**
     * Get the average of the values currently contained within the sliding
     * window.
     */
    @Synchronized fun get(): Double {
        // Make sure the window is up-to-date
        slidingWindow.forceEviction()
        if (numElements == 0) {
            return 0.0
        }
        return currSum / numElements.toDouble()
    }

    /**
     * Add a value to this moving average
     */
    @Synchronized fun add(value: T) {
        slidingWindow.add(value)
        currSum += value.toLong()
        numElements++
    }

    /**
     * Handle an eviction notice for [value] from the underlying
     * [slidingWindow].
     */
    @Synchronized private fun onEviction(value: T) {
        currSum -= value.toLong()
        numElements--
    }
}
