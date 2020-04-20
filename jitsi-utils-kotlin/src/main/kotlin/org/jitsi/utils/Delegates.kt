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

import org.jitsi.utils.properties.ReadWriteIntProperty
import org.jitsi.utils.properties.ReadWriteLongProperty
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate which runs a callback whenever the setter is called and it results in the value changing.
 */
inline fun <T> observableWhenChanged(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Unit):
        ReadWriteProperty<Any?, T> =
    Delegates.observable(initialValue) {
        property, oldValue, newValue -> if (oldValue != newValue) onChange(property, oldValue, newValue)
    }

inline fun observableIntWhenChanged(initialValue: Int, crossinline onChange: (property: KProperty<*>, oldValue: Int, newValue: Int) -> Unit):
    ReadWriteIntProperty<Any?> =
    org.jitsi.utils.properties.Delegates.observableInt(initialValue) { property, oldValue, newValue ->
        if (oldValue != newValue) onChange(property, oldValue, newValue)
    }

inline fun observableLongWhenChanged(initialValue: Long, crossinline onChange: (property: KProperty<*>, oldValue: Long, newValue: Long) -> Unit):
    ReadWriteLongProperty<Any?> =
    org.jitsi.utils.properties.Delegates.observableLong(initialValue) { property, oldValue, newValue ->
        if (oldValue != newValue) onChange(property, oldValue, newValue)
    }
