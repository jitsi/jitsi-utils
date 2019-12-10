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
import kotlin.reflect.KClass

/**
 * A config property which reads a single value from a single [ConfigSource]
 */
abstract class SimpleProperty <T : Any>(
    val attrs: ConfigPropertyAttributes<T>
) : ConfigProperty<T> {
    private val getter =
        getReadStrategy(attrs.readOnce, attrs.supplier)

    override val value: T
        get() = getter.get().getOrThrow()
}

//TODO: move this to dsl/helpers file--it's not tied to SimpleProperty.
// rename to attrs/withAttrs/fromAttrs?
