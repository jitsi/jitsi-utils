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

package org.jitsi.utils.configk

/**
 * A [ConfigResult] represents the result of a 'search' for a configuration
 * property's value.  If the property was found and the value parsed
 * successfully as type [T], use [ConfigResult.found] to indicate a found
 * result.  Otherwise [ConfigResult.notFound] can be used to hold the
 * exception.
 */
sealed class ConfigResult<T : Any> {
    class PropertyFound<T : Any>(val value: T) : ConfigResult<T>() {
        override fun equals(other: Any?): Boolean =
            other is PropertyFound<*> && value == other.value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): String = "Found($value)"
    }

    class PropertyNotFound<T : Any>(val exception: Throwable) : ConfigResult<T>() {
        override fun equals(other: Any?): Boolean =
            other is PropertyNotFound<*> && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "NotFound($exception)"
    }

    companion object {
        fun<T : Any> found(value: T): ConfigResult<T> = PropertyFound(value)
        fun<T : Any> notFound(exception: Throwable): ConfigResult<T> = PropertyNotFound(exception)
    }
}

fun<T : Any> ConfigResult<T>.getOrThrow(): T {
    return when (this) {
        is ConfigResult.PropertyNotFound -> throw this.exception
        is ConfigResult.PropertyFound -> this.value
    }
}

fun<T : Any> ConfigResult<T>.isFound(): Boolean =
    this is ConfigResult.PropertyFound

fun<T : Any> ConfigResult<T>.getOrElse(onFailure: (exception: Throwable) -> T): T {
    return when (this) {
        is ConfigResult.PropertyNotFound -> throw this.exception
        is ConfigResult.PropertyFound -> this.value
    }
}

fun<T : Any, R> ConfigResult<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: Throwable) -> R
): R {
    return when (this) {
        is ConfigResult.PropertyNotFound -> onFailure(this.exception)
        is ConfigResult.PropertyFound -> onSuccess(this.value)
    }
}

inline fun<T : Any> configRunCatching(block: () -> T): ConfigResult<T> {
    return try {
        ConfigResult.found(block())
    } catch (t: Throwable) {
        ConfigResult.notFound(t)
    }
}

