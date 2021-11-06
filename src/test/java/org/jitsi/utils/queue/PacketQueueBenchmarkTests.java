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
package org.jitsi.utils.queue;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.jitsi.utils.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.*;

/**
 * Contains test for checking performance aspects of various
 * PacketQueue configuration
 *
 * @author Yura Yaroshevich
 */
@EnabledIfSystemProperty(named="org.jitsi.utils.doPerf", matches=".*")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PacketQueueBenchmarkTests
{
    /**
     * Number of iteration to run benchmark and compute
     * average execution time.
     */
    private static final int numberOfBenchmarkIterations = 100;

    /**
     * Simulates number of concurrent existing PacketQueue instances inside
     * application. In case of JVB it is linear to number of connected peers
     * to JVB instance.
     */
    private static final int numberOfQueues = 800;

    /**
     * Simulates number of messages processed via single PacketQueue
     */
    private static final int numberOfItemsInQueue = 1000;

    /**
     * Simulates the computation "weight" of single item processed by
     * PacketQueue
     */
    private static final int singleQueueItemProcessingWeight = 100;

    @Test
    public void testMultiplePacketQueueThroughputWithThreadPerQueue()
        throws Exception
    {
        final String id = "ThreadPerQueuePool";
        /*
         * This test roughly simulates initial implementation of PacketQueue
         * when each PacketQueue instance has it's own processing thread
         */
        measureBenchmark(id, () -> {
            final ExecutorService executorService
                = Executors.newFixedThreadPool(numberOfQueues,
                new CustomizableThreadFactory(id, true));
            Duration duration = runBenchmark(
                id,
                executorService,
                -1, /* Disable cooperative multi-tasking mode,
                 which is not relevant when each queue has it's own processing
                 thread*/
                false);
            executorService.shutdownNow();
            return duration;
        });
    }

    @Test
    public void testMultiplePacketQueueThroughputWithCachedThreadPerQueue()
        throws Exception
    {
        final String id = "CachedThreadPerQueuePool";
        /*
         * This test is slight modification of previous test, but now threads
         * are re-used between PacketQueues when possible.
         */
        measureBenchmark(id, () -> {
            final ExecutorService executorService
                = Executors.newCachedThreadPool(new CustomizableThreadFactory(id, true));
            Duration duration = runBenchmark(
                id,
                executorService,
                -1, /* Disable cooperative multi-tasking mode,
                 which is not relevant when each queue has it's own processing
                 thread*/
                false);
            executorService.shutdownNow();
            return duration;
        });
    }

    @Test
    public void testMultiplePacketQueueThroughputWithFixedSizePool()
        throws Exception
    {
        final String id = "FixedSizeCPUBoundPool";
        /*
         * This test creates pool with limited number of threads, all
         * PacketQueues share threads in cooperative multi-tasking mode.
         */
        measureBenchmark(id, () -> {
            final ExecutorService executorService
                = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    new CustomizableThreadFactory(id, true));
            Duration duration = runBenchmark(
                id,
                executorService,
                50, /* Because queues will share executor
                with limited number of threads, so configure cooperative
                multi-tasking mode*/
                false);
            executorService.shutdownNow();
            return duration;
        });
    }

    @Test
    public void testMultiplePacketQueueThroughputWithForkJoinPool()
        throws Exception
    {
        final String id = "ForkJoinCPUBoundPool";
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
        measureBenchmark(id, () -> {
            final ExecutorService executorService
                = Executors.newWorkStealingPool(
                    Runtime.getRuntime().availableProcessors());
            Duration duration = runBenchmark(
                id,
                executorService,
                50, /* Because queues will share executor
                with limited number of threads, so configure cooperative
                multi-tasking mode*/
                false);
            executorService.shutdownNow();
            return duration;
        });
    }

    @Test
    public void testMultiplePacketQueueThroughputWithThreadPerQueueWithStatistics()
        throws Exception
    {
        final String id = "ThreadPerQueuePoolWithStatistics";
        /*
         * This test roughly simulates initial implementation of PacketQueue
         * when each PacketQueue instance has it's own processing thread
         */
        measureBenchmark("ThreadPerQueuePoolWithStatistics", () -> {
            final ExecutorService executorService
                = Executors.newFixedThreadPool(numberOfQueues,
                new CustomizableThreadFactory(id, true));
            Duration duration = runBenchmark(
                id,
                executorService,
                -1, /* Disable cooperative multi-tasking mode,
                 which is not relevant when each queue has it's own processing
                 thread*/
                true);
            executorService.shutdownNow();
            return duration;
        });
    }

    @Test
    public void testMultiplePacketQueueThroughputWithCachedThreadPerQueueWithStatistics()
        throws Exception
    {
        final String id = "CachedThreadPerQueuePoolWithStatistics";
        /*
         * This test is slight modification of previous test, but now threads
         * are re-used between PacketQueues when possible.
         */
        measureBenchmark(id, () -> {
            final ExecutorService executorService
                = Executors.newCachedThreadPool(new CustomizableThreadFactory(
                    "CachedThreadPerQueuePoolWithStatistics", true));
            Duration duration = runBenchmark(
                id,
                executorService,
                -1, /* Disable cooperative multi-tasking mode,
                 which is not relevant when each queue has it's own processing
                 thread*/
                true);
            executorService.shutdownNow();
            return duration;
        });
    }

    @Test
    public void testMultiplePacketQueueThroughputWithFixedSizePoolWithStatistics()
        throws Exception
    {
        final String id = "FixedSizeCPUBoundPoolWithStatistics";
        /*
         * This test creates pool with limited number of threads, all
         * PacketQueues share threads in cooperative multi-tasking mode.
         */
        measureBenchmark(id, () -> {
            final ExecutorService executorService
                = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new CustomizableThreadFactory(id, true));
            Duration duration = runBenchmark(
                id,
                executorService,
                50, /* Because queues will share executor
                with limited number of threads, so configure cooperative
                multi-tasking mode*/
                true);
            executorService.shutdownNow();
            return duration;
        });
    }

    @Test
    public void testMultiplePacketQueueThroughputWithForkJoinPoolWithStatistics()
        throws Exception
    {
        final String id = "ForkJoinCPUBoundPoolWithStatistics";
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
        measureBenchmark(id, () -> {
            final ExecutorService executorService
                = Executors.newWorkStealingPool(
                Runtime.getRuntime().availableProcessors());
            Duration duration = runBenchmark(
                id,
                executorService,
                50, /* Because queues will share executor
                with limited number of threads, so configure cooperative
                multi-tasking mode*/
            true);
            executorService.shutdownNow();
            return duration;
        });
    }

    @AfterAll
    public static void printStats()
    {
        System.out.println("Statistics: " + QueueStatistics.Companion.getStatistics().toJSONString());
    }

    private Duration runBenchmark(
        final String id,
        final ExecutorService executor,
        final long maxSequentiallyPackets,
        final boolean withStatistics)
        throws InterruptedException
    {
        final CountDownLatch completionGuard
            = new CountDownLatch(numberOfItemsInQueue * numberOfQueues);

        final ArrayList<DummyQueue> queues = new ArrayList<>();

        PacketQueue.setEnableStatisticsDefault(withStatistics);

        for (int i = 0; i < numberOfQueues; i++) {
            queues.add(new DummyQueue(
                numberOfItemsInQueue,
                id,
                new PacketQueue.PacketHandler<DummyQueue.Dummy>()
                {
                    @Override
                    public boolean handlePacket(DummyQueue.Dummy pkt)
                    {
                        double result = 0;
                        // some dummy computationally exp
                        final int end
                            = pkt.id + singleQueueItemProcessingWeight;

                        for (int i = pkt.id; i < end; i++)
                        {
                            result += Math.log(Math.sqrt(i));
                        }
                        completionGuard.countDown();
                        return result > 0;
                    }

                    @Override
                    public long maxSequentiallyProcessedPackets()
                    {
                        return maxSequentiallyPackets;
                    }
                },
                executor));
        }

        long startTime = System.nanoTime();

        for (DummyQueue queue : queues)
        {
            for (int i = 0; i < numberOfItemsInQueue; i++)
            {
                queue.add(new DummyQueue.Dummy());
            }
        }

        completionGuard.await();
        long endTime = System.nanoTime();

        for (DummyQueue queue : queues) {
            queue.close();
        }

        return Duration.ofNanos(endTime - startTime);
    }

    private void measureBenchmark(String name, Callable<Duration> runWithDuration) throws Exception
    {
        final ArrayList<Duration> experimentDuration = new ArrayList<>();
        for (int i = 0; i < 1 + numberOfBenchmarkIterations; i++)
        {
            System.gc();
            final Duration duration = runWithDuration.call();
            if (i != 0)
            {
                experimentDuration.add(duration);
            }
        }

        long totalNanos = 0;
        for (Duration duration : experimentDuration)
        {
            totalNanos += duration.toNanos();
        }
        long averageNanos = totalNanos / experimentDuration.size();

        long sumSquares = 0;

        for (Duration duration : experimentDuration)
        {
            long diff = Math.abs(duration.toNanos() - averageNanos);
            sumSquares += diff * diff;
        }

        double stdDev
            = Math.sqrt((1.0 / (experimentDuration.size() - 1)) * sumSquares);

        System.out.println(name
            + " : avg = " + TimeUnit.NANOSECONDS.toMillis(averageNanos) + " ms"
            + ", std_dev = " + TimeUnit.NANOSECONDS.toMillis((long)stdDev) + " ms");
    }
}
