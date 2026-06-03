/*
 * Copyright @ 2018 - present 8x8, Inc.
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

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * An [ObjectNode] factory that preserves insertion order (Jackson's [ObjectNode]
 * is backed by [java.util.LinkedHashMap] by default).
 *
 * Use [OrderedJsonObject] as a drop-in replacement for the old json-simple–based
 * class of the same name.  Callers that previously used bracket-assignment syntax
 * (`obj["key"] = value`) should migrate to [ObjectNode.put] / [ObjectNode.set].
 */
@Suppress("FunctionName")
fun OrderedJsonObject(): ObjectNode = JsonNodeFactory.instance.objectNode()
