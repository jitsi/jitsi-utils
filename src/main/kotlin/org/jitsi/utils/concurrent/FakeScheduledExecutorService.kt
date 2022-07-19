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

package org.jitsi.utils.concurrent

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.jitsi.utils.time.FakeClock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Delayed
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A fake [ScheduledExecutorService] which gives control over when scheduled tasks are run without requiring a
 * separate thread
 */
class FakeScheduledExecutorService(
    val clock: FakeClock = FakeClock()
) : ScheduledExecutorService {
    private var jobs = JobsTimeline()

    override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        val nextRunTime = clock.instant().plus(Duration.ofMillis(unit.toMillis(initialDelay)))
        val job = FixedRateJob(command, nextRunTime, Duration.ofMillis(unit.toMillis(period)))
        jobs.add(job)

        return EmptyFuture { job.cancelled = true }
    }

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
//        println("scheduling job with a delay of $delay $unit from time ${clock.instant()}")
        val nextRunTime = clock.instant().plus(Duration.ofNanos(unit.toNanos(delay)))
        val job = Job(command, nextRunTime)
        jobs.add(job)

        return EmptyFuture { job.cancelled = true }
    }

    override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        val nextRunTime = clock.instant().plus(Duration.ofMillis(unit.toMillis(initialDelay)))
        val job = FixedDelayJob(command, nextRunTime, Duration.ofMillis(unit.toMillis(delay)))
        jobs.add(job)

        return EmptyFuture { job.cancelled = true }
    }

    // Note that, when a job is cancelled, we don't remove it from the timeline, we just mark it
    // as cancelled.  We only want to reflect non-cancelled jobs in the count here, so we filter
    // them
    fun numPendingJobs(): Int = jobs.filter { !it.cancelled }.size

    /**
     * Run the next pending task and advance the clock to that time
     */
    fun runOne() {
        while (jobs.isNotEmpty() && jobs[0].cancelled) {
            jobs.removeAt(0)
        }
        if (jobs.isNotEmpty()) {
            val nextJob = jobs.removeAt(0)
            if (clock.instant() < nextJob.nextRunTime) {
                clock.setTime(nextJob.nextRunTime)
            }
            nextJob.run()
            if (nextJob is RecurringJob) {
                nextJob.updateNextRuntime(clock.instant())
                jobs.add(nextJob)
            }
        }
    }

    /**
     * Run pending tasks until [endTime], or until there are no pending tasks
     * in the queue, advancing the clock with each task.
     */
    fun runUntil(endTime: Instant) {
        while (jobs.isNotEmpty() && clock.instant() <= endTime && jobs.first().nextRunTime <= endTime) {
            runOne()
        }
    }

    /**
     * Runs all jobs that are due to run by the current time.  This may include a single job multiple times if the
     * time since the last run is longer than 2 times the job's period
     */
    fun run() {
        val now = clock.instant()
        if (jobs.isNotEmpty()) {
            val job = jobs.removeAt(0)
            if (!job.cancelled) {
                if (job.ready(now)) {
                    job.run()
                    if (job is RecurringJob) {
                        job.updateNextRuntime(now)
                        jobs.add(job)
                    }
                    run()
                } else {
                    // The job wasn't ready yet, re-add it to the front of the queue
                    jobs.add(0, job)
                }
            }
        }
    }

    /* Methods required by ScheduledExecutorService, but not needed by our unit tests. */

    override fun execute(command: Runnable) { schedule(command, 0, TimeUnit.MILLISECONDS) }

    override fun shutdown() = TODO("Not yet implemented")
    override fun shutdownNow(): MutableList<Runnable> = TODO("Not yet implemented")
    override fun isShutdown(): Boolean = TODO("Not yet implemented")
    override fun isTerminated(): Boolean = TODO("Not yet implemented")
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = TODO("Not yet implemented")
    override fun <T : Any?> submit(task: Callable<T>): Future<T> = TODO("Not yet implemented")
    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> = TODO("Not yet implemented")
    override fun submit(task: Runnable): Future<*> = TODO("Not yet implemented")
    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> =
        TODO("Not yet implemented")
    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): MutableList<Future<T>> = TODO("Not yet implemented")
    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T = TODO("Not yet implemented")
    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T =
        TODO("Not yet implemented")
    override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit): ScheduledFuture<V> =
        TODO("Not yet implemented")
}

/**
 * Model a Job that is track in the [FakeScheduledExecutorService]
 */
internal open class Job(val command: Runnable, var nextRunTime: Instant) {
    var cancelled = false

    fun run() = command.run()

    fun ready(now: Instant): Boolean = nextRunTime <= now

    override fun toString(): String = with(StringBuffer()) {
        append("next run time: $nextRunTime")

        toString()
    }
}

internal abstract class RecurringJob(command: Runnable, nextRunTime: Instant) : Job(command, nextRunTime) {
    abstract fun updateNextRuntime(now: Instant)
}

internal class FixedRateJob(
    command: Runnable,
    nextRunTime: Instant,
    val period: Duration
) : RecurringJob(command, nextRunTime) {
    override fun updateNextRuntime(now: Instant) {
        nextRunTime += period
    }
}

internal class FixedDelayJob(
    command: Runnable,
    nextRunTime: Instant,
    val period: Duration
) : RecurringJob(command, nextRunTime) {
    override fun updateNextRuntime(now: Instant) {
        nextRunTime = now + period
    }
}

/**
 * An [ArrayList] which keeps a list of [Job]s sorted according to their next run tim
 */
internal class JobsTimeline : ArrayList<Job>() {
    override fun add(element: Job): Boolean {
        val result = super.add(element)
        sortBy(Job::nextRunTime)
        return result
    }
}

/**
 * A simple implementation of [ScheduledFuture<Unit>] which allows passing a handler
 * to be invoked on cancellation.
 */
@SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
private class EmptyFuture(
    private val cancelHandler: () -> Unit
) : ScheduledFuture<Unit> {
    private var cancelled = false
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancelHandler()
        cancelled = true
        return true
    }

    override fun get() {}
    override fun get(timeout: Long, unit: TimeUnit) {}
    override fun getDelay(unit: TimeUnit): Long = TODO("Not yet implemented")
    override fun isCancelled(): Boolean = cancelled
    override fun isDone(): Boolean = TODO("Not yet implemented")
    override fun compareTo(other: Delayed?): Int = TODO("Not yet implemented")
}
