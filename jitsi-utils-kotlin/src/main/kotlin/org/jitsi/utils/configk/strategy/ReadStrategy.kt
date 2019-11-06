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

package org.jitsi.utils.configk.strategy

/**
 * A strategy which defines how a configuration property's value is read.
 */
sealed class ReadStrategy<T>(
    protected val configurationValueSupplier: () -> T
) {
    /**
     * Get this configuration property's value in the form
     * of a [Result<T>]: if the property was not found or
     * reading encountered an error, a [Result.failure]
     * will be returned
     */
    abstract fun get(): Result<T>
}

/**
 * Read a configuration property's value only once
 */
class ReadOnceStrategy<T>(configurationValueSupplier: () -> T) : ReadStrategy<T>(configurationValueSupplier) {
    private val result: Result<T> = runCatching { configurationValueSupplier() }

    override fun get(): Result<T> = result
}

/**
 * Re-read a configuration property's value every time it is accessed
 */
class ReadEveryTimeStrategy<T>(configurationValueSupplier: () -> T) : ReadStrategy<T>(configurationValueSupplier) {
    override fun get(): Result<T> = runCatching { configurationValueSupplier() }
}

/**
 * Based on whether or not a configuration property's value should be read only
 * a single time, return the appropriate [ReadStrategy] which will return a [Result<T]]
 */
fun <T> getReadStrategy(readOnce: Boolean, configurationValueSupplier: () -> T): ReadStrategy<T>  {
    return if (readOnce) {
        ReadOnceStrategy(configurationValueSupplier)
    } else {
        ReadEveryTimeStrategy(configurationValueSupplier)
    }
}
