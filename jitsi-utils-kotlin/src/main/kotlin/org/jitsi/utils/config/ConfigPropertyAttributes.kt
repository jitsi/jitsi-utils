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

import kotlin.reflect.KClass

/**
 * Various attributes of a configuration property pulled from a specific
 * [ConfigSource]
 */
@Suppress("unused")
data class ConfigPropertyAttributes<T : Any>(
    /**
     * The 'path' at which this property lives (also known as the
     * property's 'name')
     */
    val keyPath: String,
    /**
     * The type of this property's value
     */
    val valueType: KClass<T>,
    /**
     * Whether or not the configuration property's value should be read
     * only a single time and cached (i.e. runtime changes to this
     * value will *not* be seen by the running code) or re-read from
     * the configuration source every time its accessed.
     */
    val readOnce: Boolean,
    /**
     * The [ConfigSource] instance from which this property will
     * be read
     */
    val configSource: ConfigSource,
    /**
     * A function which will retrieve the value of the property described
     * by the attributes here.  We keep this with the attributes themselves
     * because it's most natural to describe how a property's value is
     * retrieved/transformed/etc alongside its other attributes, and those
     * settings go into the creation of this supplier.
     */
    val supplier: () -> T,
    /**
     * A deprecation notice (if applicable) for this property
     */
    val deprecationNotice: DeprecationNotice? = null
) {
    /**
     * Whether or not this property has been marked as deprecated
     */
    val isDeprecated: Boolean = deprecationNotice != null
}

data class DeprecationNotice(val message: String)
