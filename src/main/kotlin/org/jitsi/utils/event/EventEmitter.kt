/*
 * Copyright @ 2020 - present 8x8, Inc.
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

package org.jitsi.utils.event

import org.jitsi.utils.logging2.createLogger
import java.lang.Exception
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

interface EventEmitter<EventHandlerType> {
    /** Fire an event (it may be fired synchronously or asynchronously depending on the implementation). */
    fun fireEvent(event: EventHandlerType.() -> Unit)
    fun addHandler(handler: EventHandlerType)
    fun removeHandler(handler: EventHandlerType)
    val eventHandlers: List<EventHandlerType>
}

sealed class BaseEventEmitter<EventHandlerType>(
    initialHandlers: List<EventHandlerType>
) : EventEmitter<EventHandlerType> {
    private val logger = createLogger()

    override val eventHandlers: MutableList<EventHandlerType> = CopyOnWriteArrayList(initialHandlers)

    override fun addHandler(handler: EventHandlerType) {
        eventHandlers += handler
    }

    override fun removeHandler(handler: EventHandlerType) {
        eventHandlers -= handler
    }

    protected fun wrap(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.warn("Exception from event handler: ${e.message}", e)
        }
    }
}

/** An [EventEmitter] which fires events synchronously. */

class SyncEventEmitter<EventHandlerType>(initialHandlers: List<EventHandlerType>) :
    BaseEventEmitter<EventHandlerType>(initialHandlers) {

    constructor() : this(emptyList())

    override fun fireEvent(event: EventHandlerType.() -> Unit) {
        eventHandlers.forEach { wrap { it.apply(event) } }
    }
}

/** An [EventEmitter] which fires events asynchronously. */
class AsyncEventEmitter<EventHandlerType>(
    private val executor: Executor,
    initialHandlers: List<EventHandlerType> = emptyList()
) : BaseEventEmitter<EventHandlerType>(initialHandlers) {

    constructor(executor: Executor) : this(executor, emptyList())

    override fun fireEvent(event: EventHandlerType.() -> Unit) {
        eventHandlers.forEach {
            executor.execute { wrap { it.apply(event) } }
        }
    }
}
