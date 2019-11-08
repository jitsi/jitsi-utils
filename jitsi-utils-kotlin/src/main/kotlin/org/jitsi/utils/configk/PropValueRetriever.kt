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

import org.jitsi.utils.configk.strategy.ReadFrequencyStrategy
import org.jitsi.utils.configk.strategy.getReadStrategy

/**
 * A retriever retrieves the value of a property
 * via a [ReadFrequencyStrategy] derived from the given
 * [ConfigPropertyAttributes].  Note that we don't know that
 * this retrieve will successfully find the property, so
 * the result is modeled as a [ConfigResult], which may
 * hold a found value or an exception.
 */
class PropValueRetriever<T : Any>(
    val attributes: ConfigPropertyAttributes,
    configValueSupplier: () -> T
) {
    private val readFrequencyStrategy: ReadFrequencyStrategy<T> =
        getReadStrategy(attributes.readOnce, configValueSupplier)

    val result: ConfigResult<T>
        get() = readFrequencyStrategy.get()
}