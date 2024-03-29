/*
 * Copyright @ 2018 - Present, 8x8 Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.utils

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate which runs a callback whenever the setter is called and it results in the value changing.
 */
inline fun <T> observableWhenChanged(
    initialValue: T,
    crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Unit
): ReadWriteProperty<Any?, T> = Delegates.observable(initialValue) { property, oldValue, newValue ->
    if (oldValue != newValue) onChange(property, oldValue, newValue)
}

/**
 * A delegate which runs a callback (with no arguments) whenever the setter is called and it results in the value
 * changing.
 */
inline fun <T> observableWhenChanged(initialValue: T, crossinline onChange: () -> Unit): ReadWriteProperty<Any?, T> =
    observableWhenChanged(initialValue) { _, _, _ -> onChange() }

class ResettableLazy<T>(private val initializer: () -> T) {
    private var lazyHolder = lazy { initializer() }
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = lazyHolder.value
    fun reset() {
        lazyHolder = lazy { initializer() }
    }
}
