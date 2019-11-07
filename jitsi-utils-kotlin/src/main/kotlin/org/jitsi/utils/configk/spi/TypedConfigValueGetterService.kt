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

import java.util.ServiceLoader
import kotlin.reflect.KClass

class TypedConfigValueGetterService private constructor() {
    private val loader: ServiceLoader<TypedConfigValueGetter> =
        ServiceLoader.load(TypedConfigValueGetter::class.java)

    fun<T : Any> getGetter(clazz: KClass<T>): (Any, String) -> T {
        return loader.asSequence()
                .map { it.getSupplier(clazz) }
                .first()
    }

    companion object {
        private val configValueSupplierFactoryService = TypedConfigValueGetterService()

        @JvmStatic
        fun <T : Any> getterFor(clazz: KClass<T>) =
            configValueSupplierFactoryService.getGetter(clazz)
    }
}