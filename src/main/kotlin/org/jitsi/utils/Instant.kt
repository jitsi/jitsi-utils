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

package org.jitsi.utils

import java.time.Clock
import java.time.Instant

/**
 * Helpers to create instances of [Instant] more easily, and Kotlin operators for it
 */

/**
 * Converts this instant to the number of microseconds from the epoch
 * of 1970-01-01T00:00:00Z.
 *
 *
 * If this instant represents a point on the time-line too far in the future
 * or past to fit in a `long` microseconds, then an exception is thrown.
 *
 * If this instant has greater than microseconds precision, then the conversion
 * will drop any excess precision information as though the amount in nanoseconds
 * was subject to integer division by one thousand.
 *
 * @return the number of microseconds since the epoch of 1970-01-01T00:00:00Z
 * @throws ArithmeticException if numeric overflow occurs
 */
fun Instant.toEpochMicro(): Long {
    if (epochSecond < 0 && nano > 0) {
        val millis = Math.multiplyExact(epochSecond + 1, 1_000_000).toLong()
        val adjustment: Long = (nano / 1_000 - 1).toLong()
        return Math.addExact(millis, adjustment)
    } else {
        val millis = Math.multiplyExact(epochSecond, 1_000_000).toLong()
        return Math.addExact(millis, (nano / 1000).toLong())
    }
}

/**
 * Obtains an instance of {@code Instant} using microseconds from the
 * epoch of 1970-01-01T00:00:00Z.
 * <p>
 * The seconds and nanoseconds are extracted from the specified microseconds.
 *
 * @param epochMicro  the number of microseconds from 1970-01-01T00:00:00Z
 * @return an instant, not null
 * @throws DateTimeException if the instant exceeds the maximum or minimum instant
 */
fun instantOfEpochMicro(epochMicro: Long): Instant {
    val secs = Math.floorDiv(epochMicro, 1_000_000)
    val mos = Math.floorMod(epochMicro, 1_000_000).toLong()
    return Instant.ofEpochSecond(secs, mos * 1000)
}

@JvmField
val NEVER: Instant = Instant.MIN
fun Instant.formatMilli(): String = TimeUtils.formatTimeAsFullMillis(this.epochSecond, this.nano)

/**
 * Like [Instant.toEpochMilli], but rounded to nearest rather than rounded to zero.
 *
 * This is needed to be bit-exact with WebRTC unit tests, since libwebrtc clocks round this way
 */
fun Instant.toRoundedEpochMilli(): Long {
    var ret = toEpochMilli()
    val remainder = Math.floorMod(nano, 1_000_000)
    if (remainder > 499_999) {
        ret++
    }
    return ret
}

fun Instant.isInfinite(): Boolean = (this == Instant.MAX || this == Instant.MIN)
fun Instant.isFinite(): Boolean = !this.isInfinite()

/**
 * Like [Clock.millis], but rounded to nearest rather than rounded to zero.
 */
fun Clock.roundedMillis() = this.instant().toRoundedEpochMilli()

/**
 * Returns the maximum of two [Instant]s
 */
fun max(a: Instant, b: Instant): Instant {
    return if (a >= b) a else b
}

/**
 * Returns the minimum of two [Instant]s
 */
fun min(a: Instant, b: Instant): Instant {
    return if (a <= b) a else b
}
