/*
 * Copyright @ 2025 - present 8x8, Inc.
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
package org.jitsi.utils

import java.net.URI

class TemplatedUrl(
    private val template: String,
    inMap: Map<String, String> = emptyMap(),
    private val requiredKeys: Set<String> = emptySet(),
) {
    private val map = inMap.toMutableMap()

    /** Saves the given key:value pair in the map. */
    fun set(key: String, value: String) {
        map[key] = value
    }

    /** Resolve the template with the current map. */
    fun resolve() = doResolve(map)

    /** Resolve the template with the current map and a new key:value pair (does not save the new pair). */
    fun resolve(key: String, value: String) = resolve(mapOf(key to value))

    /** Resolve the template with the current map and a new map pair (does not save the new map). */
    fun resolve(newMap: Map<String, String>) = doResolve(map + newMap)

    private fun doResolve(map: Map<String, String>): URI {
        if (!requiredKeys.all { it in map }) {
            throw IllegalArgumentException("Missing required keys: ${requiredKeys - map.keys}")
        }
        var resolvedUrl = template
        for ((key, value) in map) {
            resolvedUrl = resolvedUrl.replace("{{$key}}", value)
        }
        return URI(resolvedUrl)
    }
}
