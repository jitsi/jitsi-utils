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

package org.jitsi.utils.stats

/**
 * A queue which will evict old elements based on [evictionPredicate].
 * [entryCreator] provides a translation between the raw element type ([T])
 * and a wrapper type ([U]) which can contain extra bookkeeping information
 * (such as insertion time).  Eviction is performed on *any* access to the
 * collection (it's done before the operation for read operations, and after
 * for write operations).  For operations which return a boolean value to
 * denote whether or not a change was actually applied, changes from evictions
 * are *not* counted.
 *
 * [U] must implement [EvictingQueue.Entry] to provide a means of extracting
 * the raw type [T] from an instance of [U].
 */
@ExperimentalStdlibApi
open class EvictingQueue<T : Any, U : EvictingQueue.Entry<T>>(
    private val entryCreator: (T) -> U,
    private val evictionPredicate: (U) -> Boolean
) : MutableCollection<T> {
    private val queue = ArrayDeque<U>()
    private val x = ArrayList<U>()

    override val size: Int
        get() = queue.size

    override fun add(element: T): Boolean {
        val entry = entryCreator(element)
        queue.addFirst(entry)
        evict()
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean {
        elements.map(entryCreator).forEach(queue::addFirst)
        evict()
        return true
    }

    override fun clear() = queue.clear()

    override fun contains(element: T): Boolean {
        evict()
        return queue.find { it == element } != null
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        evict()
        return elements.all(this::contains)
    }

    override fun isEmpty(): Boolean {
        evict()
        return queue.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        evict()
        return Iter()
    }

    override fun remove(element: T): Boolean {
        evict()
        queue.find { it.value == element }?.let {
            queue.remove(it)
            return true
        } ?: return false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        evict()
        if (isEmpty() || elements.isEmpty()) {
            return false
        }
        return filterInPlace { elements.contains(it) }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        evict()
        if (isEmpty() || elements.isEmpty()) {
            return false
        }
        return filterInPlace { !elements.contains(it) }
    }

    private inline fun filterInPlace(predicate: (T) -> Boolean): Boolean {
        var modified = false
        val iter = iterator()
        while (iter.hasNext()) {
            val value = iter.next()
            if (predicate(value)) {
                iter.remove()
                modified = true
            }
        }
        return modified
    }

    protected open fun onEviction(entry: U) {}

    /**
     * Remove any entries from the back of the list which return true
     * when passed to the [evictionPredicate] until we hit an entry that
     * doesn't.
     */
    private fun evict() {
        while (evictionPredicate(queue.last())) {
            onEviction(queue.last())
            queue.removeLast()
        }
    }

    inner class Iter : MutableIterator<T> {
        private val iter = queue.iterator()
        override fun hasNext(): Boolean = iter.hasNext()

        override fun remove() = iter.remove()

        override fun next(): T = iter.next().value
    }

    interface Entry<T> {
        val value: T
    }
}
