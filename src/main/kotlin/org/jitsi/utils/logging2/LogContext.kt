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
package org.jitsi.utils.logging2

import java.lang.ref.WeakReference

/**
 * Maintains a map of key-value pairs (both Strings) which holds arbitrary context to use as a prefix for log messages.
 * Sub-contexts can be created and will inherit any context values from their ancestors' context.
 */
class LogContext private constructor(
    /** All context inherited from the 'ancestors' of this [LogContext] */
    ancestorsContext: Map<String, String>,
    /** The context held by this specific LogContext. */
    private var context: Map<String, String>
) {
    @JvmOverloads
    constructor(context: Map<String, String> = emptyMap()) : this(
        context = context.toMap(),
        ancestorsContext = emptyMap()
    )

    constructor(key: String, value: String) : this(context = mapOf(key to value))

    private var ancestorsContext: Map<String, String> = ancestorsContext
        set(newValue) {
            field = newValue
            updateFormattedContext()
        }

    /**
     * The formatted String representing the total context (the combination of the ancestors' context and this
     * context)
     */
    var formattedContext: String = formatContext(ancestorsContext + context)
        private set

    /** Child [LogContext]s of this [LogContext] (which will be notified anytime this context changes) */
    private val childContexts: MutableList<WeakReference<LogContext>> = ArrayList()

    @Synchronized
    private fun updateFormattedContext() {
        val combined = ancestorsContext + context
        formattedContext = formatContext(combined)
        updateChildren(combined)
    }

    @Synchronized
    fun createSubContext(childContextData: Map<String, String>) = LogContext(
        ancestorsContext + context,
        childContextData
    ).also {
        childContexts.add(WeakReference(it))
    }

    fun addContext(key: String, value: String) = addContext(mapOf(key to value))

    @Synchronized
    fun addContext(addedContext: Map<String, String>) {
        context = context + addedContext
        updateFormattedContext()
    }

    /** Notify children of changes in this context */
    @Synchronized
    private fun updateChildren(newAncestorContext: Map<String, String>) = childContexts.apply {
        removeIf { it.get() == null }
        forEach { it.get()?.ancestorsContext = newAncestorContext }
    }

    override fun toString() = formattedContext

    companion object {
        const val CONTEXT_START_TOKEN = "["
        const val CONTEXT_END_TOKEN = "]"

        private fun formatContext(context: Map<String, String>): String {
            val s = context.entries.joinToString(separator = " ") { "${it.key}=${it.value}" }
            return if (s.isEmpty()) "" else "$CONTEXT_START_TOKEN$s$CONTEXT_END_TOKEN"
        }
    }
}
