/*
 * Copyright @ 2020 - Present, 8x8 Inc
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

package org.jitsi.utils.properties

import kotlin.reflect.KProperty

public object Delegates {
    public inline fun observableInt(initialValue: Int, crossinline onChange: (property: KProperty<*>, oldValue: Int, newValue: Int) -> Unit):
        ReadWriteIntProperty<Any?> =
        object : ObservableIntProperty(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int) = onChange(property, oldValue, newValue)
        }

    public inline fun observableLong(initialValue: Long, crossinline onChange: (property: KProperty<*>, oldValue: Long, newValue: Long) -> Unit):
        ReadWriteLongProperty<Any?> =
        object : ObservableLongProperty(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: Long, newValue: Long) = onChange(property, oldValue, newValue)
        }
}
