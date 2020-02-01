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

package org.jitsi.utils

import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReference

/**
 * [MutableOutcome] models the outcome of some process which is not yet known.  Once
 * known, it will not change.  It allows subscribing to both the success and
 * failure of the outcome.  Subscribers are notified once the outcome has
 * been reached, or instantly if the outcome has already been reached.
 *
 * Until an outcome is reached, the state is 'unknown' (it has neither succeeded
 * nor failed).  This can be tested explicitly with [isKnown]).
 */
abstract class Outcome {
    private val lock = Any()
    private val subscribers = mutableListOf<Subscriber>()
    private val succeeded = AtomicReference<Boolean>()

    /**
     * Return true if the outcome was successful, false if it failed or if
     * the outcome is not yet known.
     */
    open fun hasSucceeded() = succeeded.get() == true

    /**
     * Return true if the outcome failed, false if it succeeded orr if
     * the outcome is not yet known.
     */
    open fun hasFailed() = succeeded.get() == false

    /**
     * Returns true if the outcome has been reached (success OR failure), false
     * if it has not yet been reached.
     */
    open fun isKnown() = succeeded.get() != null

    /**
     * Ask to be notified when the outcome has been determined as successful.
     * [handler] will be invoked immediately if the outcome already succeeded.
     */
    fun onSuccess(handler: () -> Unit) {
        var notifyHandler = false
        synchronized(lock) {
            when (succeeded.get()) {
                true -> notifyHandler = true
                null -> subscribers.add(Subscriber(handler, OutcomeType.SUCCESS))
                false -> {}
            }
        }
        if (notifyHandler) {
            handler()
        }
    }

    /**
     * Ask to be notified when the outcome has been determined as a failure.
     * [handler] will be invoked immediately if the outcome already failed.
     */
    fun onFailure(handler: () -> Unit) {
        var notifyHandler = false
        synchronized(lock) {
            when (succeeded.get()) {
                false -> notifyHandler = true
                null -> subscribers.add(Subscriber(handler, OutcomeType.FAILURE))
                true -> {}
            }
        }
        if (notifyHandler) {
            handler()
        }
    }

    /**
     * Mark this [MutableOutcome] as having succeeded.
     */
    protected fun doSucceeded() {
        if (succeeded.compareAndSet(null, true)) {
            synchronized(lock) {
                subscribers.filter {
                    it.outcomeType == OutcomeType.SUCCESS
                }
                        .forEach { it.handler() }
            }
        }
    }

    /**
     * Mark this [MutableOutcome] has having failed.
     */
    protected fun doFailed() {
        if (succeeded.compareAndSet(null, false)) {
            synchronized(lock) {
                subscribers.filter {
                    it.outcomeType == OutcomeType.FAILURE
                }
                        .forEach { it.handler() }
            }
        }
    }

    open operator fun plus(other: Outcome): MultiOutcome =
        MultiOutcome(this, other)

    protected enum class OutcomeType {
        SUCCESS,
        FAILURE
    }
    private inner class Subscriber(val handler: () -> Unit, val outcomeType: OutcomeType)
}

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class MutableOutcome : Outcome() {
    /**
     * Mark this [MutableOutcome] as having succeeded.
     */
    fun succeeded() = doSucceeded()

    /**
     * Mark this [MutableOutcome] has having failed.
     */
    fun failed() = doFailed()
}

/**
 * An aggregate of 2 or more r[Outcome]s.  Once a [MultiOutcome] is
 * created, no more can be added as that would potentially change the state
 * of the outcome.
 */
class MultiOutcome(outcome1: Outcome, outcome2: Outcome, vararg additionalOutcomes: Outcome) : Outcome() {
    private val outcomes = mutableListOf(outcome1, outcome2, *additionalOutcomes)
    override fun hasSucceeded(): Boolean = outcomes.all(Outcome::hasSucceeded)

    override fun hasFailed() = outcomes.any(Outcome::hasFailed)

    override fun isKnown(): Boolean = hasFailed() || outcomes.all(Outcome::isKnown)

    override operator fun plus(other: Outcome): MultiOutcome =
        throw IllegalStateException("Cannot add another outcome to a MultiOutcome")
}
