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

package org.jitsi.utils.configk2

import org.jitsi.utils.configk.ConfigProperty
import org.jitsi.utils.configk.RetrieverInterface
import org.jitsi.utils.configk.readOnce
import org.reflections.Reflections
import org.reflections.scanners.FieldAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import java.time.Duration

fun legacyConfig(): Any = Any()
fun newConfig(): Any = Any()

abstract class SimpleBase<T : Any>(
    private val retriever: RetrieverInterface<T>
) : ConfigProperty<T> {

    final override val value: T
        get() = retriever.retrieve()
}

fun <T : Any>simpleProp(retriever: RetrieverInterface<T>): SimpleBase<T> =
    object : SimpleBase<T>(retriever) {}

fun main() {
    val reflections = Reflections(ConfigurationBuilder()
            .filterInputsBy(FilterBuilder().includePackage("org.jitsi.utils.configk2"))
            .setUrls(ClasspathHelper.forPackage("org.jitsi.utils.configk2"))
            .setScanners(
                SubTypesScanner(),
                TypeAnnotationsScanner(),
                FieldAnnotationsScanner()
            )
    )

}
