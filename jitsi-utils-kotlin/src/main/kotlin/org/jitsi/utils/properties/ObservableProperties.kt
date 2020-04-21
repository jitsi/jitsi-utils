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

/**
 * Implements the core logic of a int property delegate for a read/write property that calls callback functions
 * when changed.
 * @param initialValue the initial value of the property.
 */
public abstract class ObservableIntProperty(initialValue: Int) : ReadWriteIntProperty<Any?> {
    private var value = initialValue

    /**
     *  The callback which is called before a change to the property value is attempted.
     *  The value of the property hasn't been changed yet, when this callback is invoked.
     *  If the callback returns `true` the value of the property is being set to the new value,
     *  and if the callback returns `false` the new value is discarded and the property remains its old value.
     */
    protected open fun beforeChange(property: KProperty<*>, oldValue: Int, newValue: Int): Boolean = true

    /**
     * The callback which is called after the change of the property is made. The value of the property
     * has already been changed when this callback is invoked.
     */
    protected open fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int) {}

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

/**
 * Implements the core logic of a long property delegate for a read/write property that calls callback functions
 * when changed.
 * @param initialValue the initial value of the property.
 */
public abstract class ObservableLongProperty(initialValue: Long) : ReadWriteLongProperty<Any?> {
    private var value = initialValue

    /**
     *  The callback which is called before a change to the property value is attempted.
     *  The value of the property hasn't been changed yet, when this callback is invoked.
     *  If the callback returns `true` the value of the property is being set to the new value,
     *  and if the callback returns `false` the new value is discarded and the property remains its old value.
     */
    protected open fun beforeChange(property: KProperty<*>, oldValue: Long, newValue: Long): Boolean = true

    /**
     * The callback which is called after the change of the property is made. The value of the property
     * has already been changed when this callback is invoked.
     */
    protected open fun afterChange(property: KProperty<*>, oldValue: Long, newValue: Long) {}

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
