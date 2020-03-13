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

package org.jitsi.utils.queue

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.jitsi.utils.queue.PacketQueue.PacketHandler
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.Ignore
import org.junit.runners.MethodSorters
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Contains test for checking performance aspects of various
 * PacketQueue configurations, with statistics
 */
@Ignore("Check only performance aspect of PacketQueue")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class QueueStatisticsBenchmarkTest {
    @Test
    fun testMultiplePacketQueueThroughputWithThreadPerQueueWithStatistics() {
        /*
         * This test roughly simulates initial implementation of PacketQueue
         * when each PacketQueue instance has it's own processing thread
         */
        measureBenchmark("ThreadPerQueuePool (With Statistics)") {
            val executorService = Executors.newFixedThreadPool(numberOfQueues)
            val duration = runBenchmark(
                executorService,
                -1 /* Disable cooperative multi-tasking mode,
                     which is not relevant when each queue has it's own processing
                     thread*/,
                "ThreadPerQueuePool")
            executorService.shutdownNow()
            duration
        }
    }

    @Test
    fun testMultiplePacketQueueThroughputWithCachedThreadPerQueueWithStatistics() {
        /*
         * This test is slight modification of previous test, but now threads
         * are re-used between PacketQueues when possible.
         */
        measureBenchmark("CachedThreadPerQueuePool (With Statistics)") {
            val executorService = Executors.newCachedThreadPool()
            val duration = runBenchmark(
                executorService,
                -1 /* Disable cooperative multi-tasking mode,
                     which is not relevant when each queue has it's own processing
                     thread*/,
                "CachedThreadPerQueuePool")
            executorService.shutdownNow()
            duration
        }
    }

    @Test
    fun testMultiplePacketQueueThroughputWithFixedSizePoolWithStatistics() {
        /*
         * This test creates pool with limited number of threads, all
         * PacketQueues share threads in cooperative multi-tasking mode.
         */
        measureBenchmark("FixedSizeCPUBoundPool (With Statistics)") {
            val executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors())
            val duration = runBenchmark(
                executorService,
                50 /* Because queues will share executor
                    with limited number of threads, so configure cooperative
                    multi-tasking mode*/,
                "FixedSizeCPUBoundPool")
            executorService.shutdownNow()
            duration
        }
    }

    @Test
    fun testMultiplePacketQueueThroughputWithForkJoinPoolWithStatistics() {
        /*
         * This test check proposed change to PacketQueue implementation when
         * all created PacketQueues share single ExecutorService with limited
         * number of threads. Execution starvation is resolved by implementing
         * cooperative multi-tasking when each PacketQueue release it's thread
         * borrowed for ExecutorService so other PacketQueue instances can
         * proceed with execution.
         * This modification has noticeable better performance when executed
         * on system which is already loaded by other concurrent tasks.
         */
        measureBenchmark("ForkJoinCPUBoundPool (With Statistics)") {
            val executorService = Executors.newWorkStealingPool(
                Runtime.getRuntime().availableProcessors())
            val duration = runBenchmark(
                executorService,
                50 /* Because queues will share executor
                    with limited number of threads, so configure cooperative
                    multi-tasking mode*/,
                "ForkJoinCPUBoundPool")
            executorService.shutdownNow()
            duration
        }
    }

    @Throws(InterruptedException::class)
    private fun runBenchmark(
        executor: ExecutorService,
        maxSequentiallyPackets: Long,
        id: String
    ): Duration {
        val completionGuard = CountDownLatch(numberOfItemsInQueue * numberOfQueues)
        val queues = ArrayList<DummyQueue>()
        for (i in 0 until numberOfQueues) {
            val q = DummyQueue(
                numberOfItemsInQueue,
                object : PacketHandler<DummyQueue.Dummy> {
                    override fun handlePacket(pkt: DummyQueue.Dummy): Boolean {
                        var result = 0.0
                        // some dummy computationally exp
                        val end = pkt.id + singleQueueItemProcessingWeight
                        for (i in pkt.id until end) {
                            result += Math.log(Math.sqrt(i.toDouble()))
                        }
                        completionGuard.countDown()
                        return result > 0
                    }

                    override fun maxSequentiallyProcessedPackets(): Long {
                        return maxSequentiallyPackets
                    }
                },
                executor,
                id)
            val s = QueueStatistics(q)
            q.setObserver(s)
            queues.add(q)
        }
        val startTime = System.nanoTime()
        for (queue in queues) {
            for (i in 0 until numberOfItemsInQueue) {
                queue.add(DummyQueue.Dummy())
            }
        }
        completionGuard.await()
        val endTime = System.nanoTime()
        for (queue in queues) {
            queue.close()
        }
        return Duration.ofNanos(endTime - startTime)
    }

    @Test
    /* N.B. the fact that this comes lexicographically after the other tests is important! */
    fun testQueueStatistics() {
        val stats = QueueStatistics.getStatistics()
        stats["DummyQueueThreadPerQueuePool"] shouldNotBe null
        stats["DummyQueueCachedThreadPerQueuePool"] shouldNotBe null
        stats["DummyQueueForkJoinCPUBoundPool"] shouldNotBe null
        stats["DummyQueueForkJoinCPUBoundPool"] shouldNotBe null

        stats.size shouldBe 4

        println(stats.toJSONString())
    }

    @Throws(Exception::class)
    private fun measureBenchmark(name: String, runWithDuration: () -> Duration) {
        val experimentDuration = ArrayList<Duration>()
        for (i in 0 until 1 + numberOfBenchmarkIterations) {
            System.gc()
            val duration = runWithDuration.invoke()
            if (i != 0) {
                experimentDuration.add(duration)
            }
        }
        var totalNanos: Long = 0
        for (duration in experimentDuration) {
            totalNanos += duration.toNanos()
        }
        val averageNanos = totalNanos / experimentDuration.size
        var sumSquares: Long = 0
        for (duration in experimentDuration) {
            val diff = Math.abs(duration.toNanos() - averageNanos)
            sumSquares = diff * diff
        }
        val stdDev = Math.sqrt(1.0 / (experimentDuration.size - 1) * sumSquares)
        println(name +
            " : avg = " + TimeUnit.NANOSECONDS.toMillis(averageNanos) + " ms" +
            ", std_dev = " + TimeUnit.NANOSECONDS.toMillis(stdDev.toLong()) + " ms")
    }

    companion object {
        /**
         * Number of iteration to run benchmark and compute
         * average execution time.
         */
        private const val numberOfBenchmarkIterations = 100

        /**
         * Simulates number of concurrent existing PacketQueue instances inside
         * application. In case of JVB it is linear to number of connected peers
         * to JVB instance.
         */
        private const val numberOfQueues = 800

        /**
         * Simulates number of messages processed via single PacketQueue
         */
        private const val numberOfItemsInQueue = 1000

        /**
         * Simulates the computation "weight" of single item processed by
         * PacketQueue
         */
        private const val singleQueueItemProcessingWeight = 300
    }
}
