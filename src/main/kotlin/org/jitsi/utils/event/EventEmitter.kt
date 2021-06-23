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

open class EventEmitter<EventHandlerType> {
    protected val logger = createLogger()

    protected val eventHandlers: MutableList<EventHandlerType> = CopyOnWriteArrayList()

    fun addHandler(handler: EventHandlerType) {
        eventHandlers += handler
    }

    fun removeHandler(handler: EventHandlerType) {
        eventHandlers -= handler
    }

    fun fireEventSync(event: EventHandlerType.() -> Unit) {
        eventHandlers.forEach { wrap { it.apply(event) } }
    }

    protected fun wrap(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.warn("Exception from event handler: ${e.message}", e)
        }
    }
}

class AsyncEventEmitter<EventHandlerType>(private val executor: Executor) : EventEmitter<EventHandlerType>() {

    fun fireEventAsync(event: EventHandlerType.() -> Unit) {
        eventHandlers.forEach {
            executor.execute { wrap { it.apply(event) } }
        }
    }
}
