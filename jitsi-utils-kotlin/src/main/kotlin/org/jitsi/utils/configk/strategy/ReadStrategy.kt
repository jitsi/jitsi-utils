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

import org.jitsi.utils.configk.ConfigResult
import org.jitsi.utils.configk.configRunCatching

/**
 * A strategy which defines how a configuration property's value is read.
 */
//TODO: readfrequencystrategy
sealed class ReadStrategy<T : Any>(
    protected val configurationValueSupplier: () -> T
) {
    /**
     * Get this configuration property's value in the form
     * of a [ConfigResult<T>]: if the property was not found or
     * reading encountered an error, a [ConfigResult.NotFound]
     * will be returned
     */
    abstract fun get(): ConfigResult<T>
}

/**
 * Read a configuration property's value only once
 */
class ReadOnceStrategy<T : Any>(configurationValueSupplier: () -> T) : ReadStrategy<T>(configurationValueSupplier) {
    private val result: ConfigResult<T> = configRunCatching { configurationValueSupplier() }

    override fun get(): ConfigResult<T> = result
}

/**
 * Re-read a configuration property's value every time it is accessed
 */
class ReadEveryTimeStrategy<T : Any>(configurationValueSupplier: () -> T) : ReadStrategy<T>(configurationValueSupplier) {
    override fun get(): ConfigResult<T> = configRunCatching { configurationValueSupplier() }
}

/**
 * Based on whether or not a configuration property's value should be read only
 * a single time, return the appropriate [ReadStrategy] which will return a [ConfigResult<T]]
 */
fun <T : Any> getReadStrategy(readOnce: Boolean, configurationValueSupplier: () -> T): ReadStrategy<T>  {
    return if (readOnce) {
        ReadOnceStrategy(configurationValueSupplier)
    } else {
        ReadEveryTimeStrategy(configurationValueSupplier)
    }
}
