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

public abstract class ObservableIntProperty(initialValue: Int) : ReadWriteIntProperty<Any?> {
    private var value = initialValue

    protected open fun beforeChange(property: KProperty<*>, oldValue: Int, newValue: Int): Boolean = true

    protected open fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int): Unit {}

    public override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return value
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        val oldValue = this.value
        if (!beforeChange(property, oldValue, value)) {
            return
        }
        this.value = value
        afterChange(property, oldValue, value)
    }
}

public abstract class ObservableLongProperty(initialValue: Long) : ReadWriteLongProperty<Any?> {
    private var value = initialValue

    protected open fun beforeChange(property: KProperty<*>, oldValue: Long, newValue: Long): Boolean = true

    protected open fun afterChange(property: KProperty<*>, oldValue: Long, newValue: Long): Unit {}

    public override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        return value
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        val oldValue = this.value
        if (!beforeChange(property, oldValue, value)) {
            return
        }
        this.value = value
        afterChange(property, oldValue, value)
    }
}