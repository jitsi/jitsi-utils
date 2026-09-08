/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Copyright @ 2015-Present 8x8, Inc.
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
package org.jitsi.utils

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Deque
import java.util.LinkedList

/**
 * Rate limiting which works as follows:
 * - must be at least [defaultMinInterval] gap between the requests
 * - no more than [maxRequests] requests within the [interval]
 */
class RateLimit(
    /** Never accept a request unless at least [defaultMinInterval] has passed since the last request */
    private val defaultMinInterval: Duration = 10.secs,
    /** Accept at most [maxRequests] per [interval]. */
    private val maxRequests: Int = 3,
    /** Accept at most [maxRequests] per [interval]. */
    private val interval: Duration = 60.secs,
    private val clock: Clock = Clock.systemUTC()
) {
    /** Stores the timestamps of requests that have been received. */
    private val requests: Deque<Instant> = LinkedList()

    /**
     * Return true if a request at [now] would be accepted, without recording it. Use this together with [record]
     * when a request has to pass more than one [RateLimit], so that a request rejected by one limit does not
     * count against the others.
     * [now] is the current time, if not provided [clock].instant() is used.
     * [minInterval] can be specified per call to support varying minimum intervals (e.g., based on round-trip
     * times).
     */
    @JvmOverloads
    fun wouldAccept(now: Instant = clock.instant(), minInterval: Duration = defaultMinInterval): Boolean {
        val previousRequest = requests.peekLast() ?: return true

        if (Duration.between(previousRequest, now) < minInterval) {
            return false
        }

        // Allow only [maxRequests] requests within the last [interval]
        return requests.count { Duration.between(it, now) <= interval } < maxRequests
    }

    /**
     * Record that a request was made at [now], so that it counts towards subsequent [wouldAccept] and [accept]
     * calls. This does not check whether the request would have been accepted; call [wouldAccept] first.
     * [now] is the current time, if not provided [clock].instant() is used.
     */
    @JvmOverloads
    fun record(now: Instant = clock.instant()) {
        requests.removeIf { Duration.between(it, now) > interval }
        requests.add(now)
    }

    /**
     * Return true if the request should be accepted and false otherwise. An accepted request is recorded, a
     * rejected one is not. Equivalent to [wouldAccept] followed by [record] when it returns true.
     * [now] is the current time, if not provided [clock].instant() is used.
     * [minInterval] can be specified per [accept] call to support varying minimum intervals
     *  (e.g., based on round-trip times).
     */
    @JvmOverloads
    fun accept(now: Instant = clock.instant(), minInterval: Duration = defaultMinInterval): Boolean {
        if (!wouldAccept(now, minInterval)) {
            return false
        }
        record(now)
        return true
    }
}
