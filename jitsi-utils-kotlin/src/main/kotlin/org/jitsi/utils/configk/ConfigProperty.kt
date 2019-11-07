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

package org.jitsi.utils.configk

interface ConfigProperty<T : Any>

abstract class AbstractConfigProperty<T : Any> : ConfigProperty<T> {
    /**
     * Get the calculated value of this configuration property
     * (this should be the final result after checking appropriate
     * configuration sources).  Implementors should throw
     * [TODO()] exception if no value for the property was found
     */
    protected abstract fun getPropertyValue(): T
}

// We need 2 distinct types here so that we can model 'value' as
// either T or T?
abstract class RequiredConfigProperty<T : Any> : AbstractConfigProperty<T>() {
    val value: T
        get() = getPropertyValue()
}

abstract class OptionalConfigProperty<T : Any> : AbstractConfigProperty<T>() {
    val value: T?
        get() = getOrNull { getPropertyValue() }
}
