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

package org.jitsi.utils.config.helpers

import org.jitsi.utils.config.ConfigPropertyAttributes
import org.jitsi.utils.config.ConfigPropertyAttributesBuilder
import kotlin.reflect.KClass

inline fun <reified T : Any> attributes(
    noinline block: ConfigPropertyAttributesBuilder<T>.() -> Unit
): ConfigPropertyAttributes<T> = attributes(T::class, block)

fun <T : Any> attributes(
    valueType: KClass<T>,
    block: ConfigPropertyAttributesBuilder<T>.() -> Unit
): ConfigPropertyAttributes<T> {
    return with (ConfigPropertyAttributesBuilder(valueType)) {
        block()
        build()
    }
}
