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
 * Base interface that can be used for implementing property delegates of read-only properties
 * of int primitive type. This is the int primitive specialization for kotlin.properties.ReadOnlyProperty<T, U>.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param T the type of object which owns the delegated property.
 */
public interface ReadOnlyIntProperty<in T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public operator fun getValue(thisRef: T, property: KProperty<*>): Int
}

/**
 * Base interface that can be used for implementing property delegates of read-write properties
 * of long primitive type. This is the int primitive specialization for kotlin.properties.ReadWriteProperty<T, U>.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param T the type of object which owns the delegated property.
 */
public interface ReadWriteIntProperty<in T> : ReadOnlyIntProperty<T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public override operator fun getValue(thisRef: T, property: KProperty<*>): Int

    /**
     * Sets the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @param value the value to set.
     */
    public operator fun setValue(thisRef: T, property: KProperty<*>, value: Int)
}

/**
 * Base interface that can be used for implementing property delegates of read-only properties
 * of long primitive type. This is the long primitive specialization for kotlin.properties.ReadOnlyProperty<T, U>.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param T the type of object which owns the delegated property.
 */
public interface ReadOnlyLongProperty<in T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public operator fun getValue(thisRef: T, property: KProperty<*>): Long
}

/**
 * Base interface that can be used for implementing property delegates of read-write properties
 * of long primitive type. This is the long primitive specialization for kotlin.properties.ReadWriteLongProperty<T, U>.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param T the type of object which owns the delegated property.
 */
public interface ReadWriteLongProperty<in T> : ReadOnlyLongProperty<T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public override operator fun getValue(thisRef: T, property: KProperty<*>): Long

    /**
     * Sets the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @param value the value to set.
     */
    public operator fun setValue(thisRef: T, property: KProperty<*>, value: Long)
}
