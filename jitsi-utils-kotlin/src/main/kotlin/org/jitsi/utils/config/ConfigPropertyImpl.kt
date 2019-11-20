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

package org.jitsi.utils.config

import org.jitsi.utils.config.strategy.getReadStrategy
import org.jitsi.utils.logging2.LoggerImpl

/**
 * A generic [ConfigProperty] implementation which takes in
 * a set of attributes and, optionally, a custom supplier
 * and derives a read frequency strategy to retrieve
 * the configuration value via [get]
 */
class ConfigPropertyImpl<T : Any>(
    private val attrs: ConfigPropertyAttributes<T>,
    supplier: () -> T = attrs.supplier
) : ConfigProperty<T> {
    private val readFrequencyStrategy =
        getReadStrategy(attrs.readOnce, supplier)

    override val value: T
        get() {
            val result = readFrequencyStrategy.get()
            if (attrs.isDeprecated && result.isFound()) {
                logger.warn("Property ${attrs.keyPath} is marked as deprecated" +
                        " in config ${attrs.configSource.name} but has a value set.")
            }
            return result.getOrThrow()
        }

    companion object {
        private val logger = LoggerImpl("ConfigProperty")
    }
}