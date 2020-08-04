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

/**
 * Helpers to create instance of [Duration] more easily
 */

val Number.nanos: Duration
    get() = Duration.ofNanos(this.toLong())

val Number.ms: Duration
    get() = Duration.ofMillis(this.toLong())

val Number.secs: Duration
    get() = Duration.ofSeconds(this.toLong())

val Number.hours: Duration
    get() = Duration.ofHours(this.toLong())

val Number.mins: Duration
    get() = Duration.ofMinutes(this.toLong())

val Number.days: Duration
    get() = Duration.ofDays(this.toLong())

operator fun Duration.times(x: Int): Duration = Duration.ofNanos(toNanos() * x)
operator fun Duration.div(other: Duration): Double = toNanos().toDouble() / other.toNanos()
