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

import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Helpers to create instances of [Duration] more easily, and Kotlin operators for it
 */

val Int.nanos: Duration
    get() = Duration.ofNanos(this.toLong())

val Int.micros: Duration
    get() = Duration.of(this.toLong(), ChronoUnit.MICROS)

val Int.ms: Duration
    get() = Duration.ofMillis(this.toLong())

val Int.secs: Duration
    get() = Duration.ofSeconds(this.toLong())

val Int.hours: Duration
    get() = Duration.ofHours(this.toLong())

val Int.mins: Duration
    get() = Duration.ofMinutes(this.toLong())

val Int.days: Duration
    get() = Duration.ofDays(this.toLong())

val Long.nanos: Duration
    get() = Duration.ofNanos(this)

val Long.micros: Duration
    get() = Duration.of(this, ChronoUnit.MICROS)

val Long.ms: Duration
    get() = Duration.ofMillis(this)

val Long.secs: Duration
    get() = Duration.ofSeconds(this)

val Long.hours: Duration
    get() = Duration.ofHours(this)

val Long.mins: Duration
    get() = Duration.ofMinutes(this)

val Long.days: Duration
    get() = Duration.ofDays(this)

operator fun Duration.times(x: Int): Duration = this.multipliedBy(x.toLong())
operator fun Duration.times(x: Long): Duration = this.multipliedBy(x)

operator fun Int.times(x: Duration): Duration = x.multipliedBy(this.toLong())
operator fun Long.times(x: Duration): Duration = x.multipliedBy(this)

operator fun Duration.div(other: Duration): Double = toNanos().toDouble() / other.toNanos()

/** Converts this duration to the total length in milliseconds, rounded to nearest. */
fun Duration.toRoundedMillis(): Long {
    var millis = this.toMillis()
    val remainder = nano % NANOS_PER_MILLI
    if (remainder > 499_999) {
        millis++
    }
    return millis
}

/**
 * Converts this duration to the total length in microseconds.
 *
 * If this duration is too large to fit in a `long` microseconds, then an
 * exception is thrown.
 *
 * If this duration has greater than microseconds precision, then the conversion
 * will drop any excess precision information as though the amount in nanoseconds
 * was subject to integer division by one thousand.
 *
 * @return the total length of the duration in microseconds
 * @throws ArithmeticException if numeric overflow occurs
 */
fun Duration.toMicros(): Long {
    var tempSeconds: Long = seconds
    var tempNanos: Long = nano.toLong()
    if (tempSeconds < 0) {
        // change the seconds and nano value to
        // handle Long.MIN_VALUE case
        tempSeconds += 1
        tempNanos -= NANOS_PER_SECOND
    }
    var micros = Math.multiplyExact(tempSeconds, MICROS_PER_SECOND)
    micros = Math.addExact(micros, tempNanos / NANOS_PER_MICRO)
    return micros
}

/** Converts this duration to the total length in microseconds, rounded to nearest. */
fun Duration.toRoundedMicros(): Long {
    var micros = this.toMicros()
    val remainder = nano % NANOS_PER_MICRO
    if (remainder > 499) {
        micros++
    }
    return micros
}

private const val NANOS_PER_MICRO = 1_000
private const val NANOS_PER_MILLI = 1_000_000
private const val MICROS_PER_SECOND = 1_000_000
private const val NANOS_PER_SECOND = 1_000_000_000

fun Duration.roundUpTo(resolution: Duration): Duration {
    assert(resolution > Duration.ZERO)
    return Duration.ofNanos((toNanos() + resolution.toNanos() - 1) / resolution.toNanos()) * resolution.toNanos()
}

fun Duration.roundDownTo(resolution: Duration): Duration {
    assert(resolution > Duration.ZERO)
    return Duration.ofNanos(toNanos() / resolution.toNanos()) * resolution.toNanos()
}
