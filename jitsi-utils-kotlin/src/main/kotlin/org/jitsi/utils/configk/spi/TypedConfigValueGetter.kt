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

package org.jitsi.utils.configk.spi

import kotlin.reflect.KClass

/**
 * If we define a common 'Config' interface with getters
 * for each type (getInt, getBoolean, etc.) we:
 * 1) Have to define a method for every type any config source
 *    might implement
 * 2) Need to re-implement more complex 'common denominator'
 *    types (config libs often have some sort of `JSONObject` equivalent)
 *    and then there needs to be conversions back and forth between
 *    the library's type and the common type defined here
 *
 * Instead, we define this SPI interface which defines a method
 * which, given a property value type, returns a function which
 * takes in some config library instance and a configuration
 * property path as a String.  This way, implementors can provide
 * an implementation of this service which handles all types the
 * underlying config library supports.
 */
interface TypedConfigValueGetter {
    /**
     * Given the type of the configuration value [T], return a
     * function which will take in some config instance and a path
     * and return the value of the property at path as type [T].
     */
    fun<T : Any> getSupplier(clazz: KClass<T>): (Any, String) -> T
}