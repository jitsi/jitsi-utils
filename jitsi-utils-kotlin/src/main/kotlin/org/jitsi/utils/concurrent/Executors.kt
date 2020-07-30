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

package org.jitsi.utils.concurrent

import org.jitsi.utils.logging2.LoggerImpl
import org.json.simple.JSONObject
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import kotlin.reflect.KFunction0

abstract class TaskServiceWrapper<T : ExecutorService>(
    val name: String,
    protected val delegate: T
) {
    private val logger = LoggerImpl(name)
    private val numExceptions = LongAdder()

    protected fun wrapTask(task: Runnable): Runnable {
        return Runnable {
            try {
                task.run()
            } catch (t: Throwable) {
                logger.warn("Uncaught exception: $t")
                numExceptions.increment()
                throw t
            }
        }
    }

    fun getStatsJson(): JSONObject = JSONObject().apply {
        put("executor_class", delegate::class.simpleName)
        put("num_exceptions", numExceptions.sum())

        val ex = delegate as? ThreadPoolExecutor ?: return@apply
        put("pool_size", ex.poolSize)
        put("active_task_count", ex.activeCount)
        put("completed_task_count", ex.completedTaskCount)
        put("core_pool_size", ex.corePoolSize)
        put("maximum_pool_size", ex.maximumPoolSize)
        put("largest_pool_size", ex.largestPoolSize)
        put("queue_class", ex.queue.javaClass.simpleName)
        put("pending_task_count", ex.queue.size)
    }

    fun safeShutdown(timeout: Duration) = delegate.apply {
        shutdown()
        if (!awaitTermination(timeout.toMillis() / 2, TimeUnit.MILLISECONDS)) {
            shutdownNow()
            if (!awaitTermination(timeout.toMillis() / 2, TimeUnit.MILLISECONDS)) {
                throw ExecutorShutdownTimeoutException()
            }
        }
    }
}

class ExecutorShutdownTimeoutException : Exception("Timed out trying to shutdown executor service")

class SafeExecutor(
    name: String,
    delegate: ExecutorService
) : TaskServiceWrapper<ExecutorService>(name, delegate) {

    fun execute(task: Runnable) = delegate.execute(wrapTask(task))
    fun execute(f: KFunction0<Unit>) = execute(Runnable { f.invoke() })
    fun execute(f: () -> Unit) = execute(Runnable { f.invoke() })
    fun <T> unsafeSubmit(task: Callable<T>): Future<T> = delegate.submit(task)
    fun <T> unsafeSubmit(task: Runnable, result: T): Future<T>? = delegate.submit(task, result)
    fun unsafeSubmit(task: Runnable): Future<*>? = delegate.submit(task)
}

class SafeScheduledExecutor(
    name: String,
    delegate: ScheduledExecutorService
) : TaskServiceWrapper<ScheduledExecutorService>(name, delegate) {

    fun schedule(command: Runnable, delay: Long, unit: TimeUnit) {
        unsafeSchedule(wrapTask(command), delay, unit)
    }

    fun unsafeSchedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> =
        delegate.schedule(command, delay, unit)

    fun unsafeSchedule(command: KFunction0<Unit>, delay: Duration): ScheduledFuture<*> =
        unsafeSchedule(Runnable { command() }, delay.toMillis(), TimeUnit.MILLISECONDS)

    fun unsafeScheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> = delegate.scheduleAtFixedRate(command, initialDelay, period, unit)

    fun unsafeScheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> = delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit)
}
