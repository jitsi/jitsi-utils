package org.jitsi.utils

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Deque
import java.util.LinkedList

/**
 * Rate limiting which works as follows:
 * - must be at least [minInterval] gap between the requests
 * - no more than [maxRequests] requests within the [interval]
 */
class RateLimit(
    /** Never accept a request unless at least [minInterval] has passed since the last request */
    private val minInterval: Duration = 10.secs,
    /** Accept at most [maxRequests] per [interval]. */
    private val maxRequests: Int = 3,
    /** Accept at most [maxRequests] per [interval]. */
    private val interval: Duration = 60.secs,
    private val clock: Clock = Clock.systemUTC()
) {
    /** Stores the timestamps of requests that have been received. */
    private val requests: Deque<Instant> = LinkedList()

    /** Return true if the request should be accepted and false otherwise. */
    fun accept(): Boolean {
        val now = clock.instant()
        val previousRequest = requests.peekLast()
        if (previousRequest == null) {
            requests.add(now)
            return true
        }

        if (Duration.between(previousRequest, now) < minInterval) {
            return false
        }

        // Allow only [maxRequests] requests within the last [interval]
        requests.removeIf { Duration.between(it, now) > interval }
        if (requests.size >= maxRequests) {
            return false
        }
        requests.add(now)
        return true
    }
}
