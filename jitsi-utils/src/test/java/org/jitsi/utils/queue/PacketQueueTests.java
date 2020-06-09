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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

/**
 * Test various aspects of {@link PacketQueue} implementation.
 *
 * @author Yura Yaroshevich
 */
public class PacketQueueTests
{
    @Test
    public void testAddingItemToQueueNotifiesBlockedThreadsImmediately()
        throws Exception
    {
        final DummyQueue dummyQueue = new DummyQueue(10);
        final CompletableFuture<DummyQueue.Dummy> dummyItem =
            CompletableFuture.supplyAsync(dummyQueue::get);

        try
        {
            // This block surrounded with try/catch necessary to ensure that
            // thread calling PacketQueue::get blocked before items is added
            // to queue. Giving 50ms for CompletableFuture to stuck on get.
            dummyItem.get(50, TimeUnit.MILLISECONDS);
            Assertions.fail("There is no items in queue, must not be here");
        }
        catch (TimeoutException e)
        {
            // no item is added during 50 ms into queue.
        }

        final DummyQueue.Dummy pushedItem = new DummyQueue.Dummy();

        dummyQueue.add(pushedItem);

        try
        {
            // checks that thread stuck in PacketQueue::get notified
            // "immediately" when item added to queue. Giving a few ms for
            // CompletableFuture to transit to completed state.
            final DummyQueue.Dummy poppedItem
                    = dummyItem.get(50, TimeUnit.MILLISECONDS);
            Assertions.assertEquals(pushedItem, poppedItem);
        }
        catch (TimeoutException e)
        {
            Assertions.fail("Expected that blocked thread notified immediately "
                + "about item added to queue");
        }
    }

    @Test
    public void testClosingQueueImmediatelyNotifiesAllThreadsBlockedOnGet()
        throws Exception
    {
        final DummyQueue dummyQueue = new DummyQueue(10);
        final ArrayList<CompletableFuture<DummyQueue.Dummy>>
            dummyItems = new ArrayList<>();
        for (int i = 0; i < ForkJoinPool.getCommonPoolParallelism(); i++)
        {
            dummyItems.add(CompletableFuture.supplyAsync(dummyQueue::get));
        }

        for (CompletableFuture<DummyQueue.Dummy> dummyItem : dummyItems)
        {
            try
            {
                // This block surrounded with try/catch necessary to ensure that
                // thread calling PacketQueue::get blocked before items is added
                // to queue. Giving 50ms for CompletableFuture to stuck on get.
                dummyItem.get(50, TimeUnit.MILLISECONDS);
                Assertions.fail("There is no items in queue, must not be here");
            }
            catch (TimeoutException e)
            {
                // no item is added during 50 ms into queue.
            }
        }

        dummyQueue.close();

        for (CompletableFuture<DummyQueue.Dummy> dummyItem : dummyItems)
        {
            try
            {
                // checks that thread stuck in PacketQueue::get notified
                // "immediately" when PacketQueue is stopped. Giving 1 ms for
                // CompletableFuture to transit to completed state.
                final DummyQueue.Dummy poppedItem = dummyItem.get(
                    1, TimeUnit.MILLISECONDS);
                Assertions.assertNull(poppedItem, "When PacketQueue is closed "
                    + "null must be returned");
            }
            catch (TimeoutException e)
            {
                Assertions
                    .fail("Expected that blocked thread notified immediately "
                        + "when queue is stopped");
            }
        }
    }

    @Test
    public void testAddingWhenCapacityReachedRemovesOldestItem()
    {
        final int capacity = 10;
        final DummyQueue dummyQueue = new DummyQueue(capacity);

        for (int i = 0; i < capacity + 1; i++)
        {
            DummyQueue.Dummy item = new DummyQueue.Dummy();
            item.id = i;

            dummyQueue.add(item);
        }

        for (int i = 0; i < capacity + 1; i++)
        {
            final DummyQueue.Dummy item = dummyQueue.poll();
            if (i == capacity)
            {
                Assertions.assertNull(item);
            }
            else
            {
                Assertions.assertNotEquals(0, item.id,
                    "Oldest item must be removed when "
                        + "item exceeding capacity added");
            }
        }
    }

    @Test
    public void testPacketQueueReaderThreadIsReleasedWhenPacketQueueEmpty()
        throws Exception
    {
        final ExecutorService singleThreadExecutor
            = Executors.newSingleThreadExecutor();

        final CountDownLatch queueCompletion = new CountDownLatch(1);

        final DummyQueue queue = new DummyQueue(
            10,
            pkt -> {
                queueCompletion.countDown();
                return true;
            },
            singleThreadExecutor);

        queue.add(new DummyQueue.Dummy());

        final boolean completed
            = queueCompletion.await(50, TimeUnit.MILLISECONDS);
        Assertions
            .assertTrue(completed, "Expected all queued items are handled "
                + "at this time point");

        Future<?> executorCompletion = singleThreadExecutor.submit(() -> {
            // do nothing, just pump Runnable via executor's thread to
            // verify it's not stuck
        });

        try
        {
            executorCompletion.get(10, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e)
        {
            Assertions.fail("Executors thread must be released by PacketQueue "
                + "when queue is empty");
        }

        singleThreadExecutor.shutdownNow();
    }

    @Test
    public void testPacketQueueCooperativeMultiTaskingWhenSharingExecutor()
        throws Exception
    {
        final int maxSequentiallyProcessedPackets = 1;

        final int queueCapacity = 10 * maxSequentiallyProcessedPackets;

        final CountDownLatch completionGuard
            = new CountDownLatch(2 * queueCapacity);

        final ExecutorService singleThreadExecutor
            = Executors.newSingleThreadExecutor();

        final AtomicInteger queue1Counter = new AtomicInteger();

        final AtomicInteger queue2Counter = new AtomicInteger();

        final AtomicBoolean queuesEvenlyProcessed
            = new AtomicBoolean(true);

        final BiFunction<AtomicInteger,
                         AtomicInteger,
                         PacketQueue.PacketHandler<DummyQueue.Dummy>>
            newPacketQueue = (AtomicInteger self, AtomicInteger other) ->
                new PacketQueue.PacketHandler<DummyQueue.Dummy>()
                {
                    @Override
                    public boolean handlePacket(DummyQueue.Dummy pkt)
                    {
                        int diff = Math.abs(
                            self.incrementAndGet() - other.get());

                        queuesEvenlyProcessed.set(queuesEvenlyProcessed.get()
                            && diff <= maxSequentiallyProcessedPackets());

                        completionGuard.countDown();

                        return false;
                    }

                    @Override
                    public long maxSequentiallyProcessedPackets()
                    {
                        return maxSequentiallyProcessedPackets;
                    }
                };

        final DummyQueue queue1 = new DummyQueue(
            queueCapacity,
            newPacketQueue.apply(queue1Counter, queue2Counter),
            singleThreadExecutor);

        final DummyQueue queue2 = new DummyQueue(
            queueCapacity,
            newPacketQueue.apply(queue2Counter, queue1Counter),
            singleThreadExecutor);

        for (int i = 0; i < queueCapacity; i++)
        {
            queue1.add(new DummyQueue.Dummy());
            queue2.add(new DummyQueue.Dummy());
        }

        final boolean completed
            = completionGuard.await(1, TimeUnit.SECONDS);
        Assertions
            .assertTrue(completed, "Expected all queued items are handled "
                + "at this time point");

        Assertions.assertTrue(
            queuesEvenlyProcessed.get(),
            "Queues sharing same thread with configured cooperative"
                + " multi-tasking must yield execution to be processed evenly");

        singleThreadExecutor.shutdownNow();
    }

    @Test
    public void testManyQueuesCanShareSingleThread()
        throws Exception
    {
        final ExecutorService singleThreadedExecutor
            = Executors.newSingleThreadExecutor();

        final int numberOfQueues = 1_000_000;

        final ArrayList<DummyQueue> queues = new ArrayList<>();

        final CountDownLatch completionGuard
            = new CountDownLatch(numberOfQueues);

        for (int i = 0; i < numberOfQueues; i++)
        {
            queues.add(new DummyQueue(1,
                pkt -> {
                    completionGuard.countDown();
                    return true;
                },
                singleThreadedExecutor));
        }

        final DummyQueue.Dummy dummyPacket = new DummyQueue.Dummy();
        for (DummyQueue queue : queues)
        {
            // Push item for processing to cause borrowing execution
            // thread from ExecutorService
            queue.add(dummyPacket);
        }

        final boolean completed
            = completionGuard.await(1, TimeUnit.SECONDS);
        Assertions
            .assertTrue(completed, "Expected all queued items are handled "
                + "at this time point");

        final List<Runnable> packetReaders
            = singleThreadedExecutor.shutdownNow();

        Assertions.assertEquals(0, packetReaders.size(),
            "Queues must not utilize thread when"
                + "there is no work.");

        for (DummyQueue queue : queues)
        {
            queue.close();
        }
    }

    @Test
    public void testReleasePacketCalledForPacketsPoppedDueToQueueOverflow()
        throws Exception
    {

        final ExecutorService singleThreadedExecutor
            = Executors.newSingleThreadExecutor();

        final List<DummyQueue.Dummy> releasedPackets = new ArrayList<>();

        final int queueCapacity = 1;

        final DummyQueue queue = new DummyQueue(queueCapacity)
        {
            @Override
            protected void releasePacket(Dummy pkt)
            {
                releasedPackets.add(pkt);
            }
        };

        final int itemsToEnqueue = 10;

        for (int i = 0; i < itemsToEnqueue; i++)
        {
            final DummyQueue.Dummy dummy = new DummyQueue.Dummy();
            dummy.id = i + 1;
            queue.add(dummy);
        }

        Assertions.assertEquals(
            itemsToEnqueue - queueCapacity, releasedPackets.size());

        int seed = 1;
        for (DummyQueue.Dummy releasedPacket : releasedPackets)
        {
            Assertions.assertEquals(seed, releasedPacket.id);
            seed++;
        }

        singleThreadedExecutor.shutdown();
    }

    @Test
    public void testReleasePacketCalledForPacketsInQueueWhenClosing()
        throws Exception
    {

        final ExecutorService singleThreadedExecutor
            = Executors.newSingleThreadExecutor();

        final List<DummyQueue.Dummy> releasedPackets = new ArrayList<>();

        final int queueCapacity = 10;

        final DummyQueue queue = new DummyQueue(queueCapacity)
        {
            @Override
            protected void releasePacket(Dummy pkt)
            {
                releasedPackets.add(pkt);
            }
        };

        // Fill the queue
        for (int i = 0; i < queueCapacity; i++)
        {
            final DummyQueue.Dummy dummy = new DummyQueue.Dummy();
            dummy.id = i + 1;
            queue.add(dummy);
        }

        queue.close();
        Assertions.assertEquals(queueCapacity, releasedPackets.size());

        int seed = 1;
        for (DummyQueue.Dummy releasedPacket : releasedPackets)
        {
            Assertions.assertEquals(seed, releasedPacket.id);
            seed++;
        }

        singleThreadedExecutor.shutdown();
    }
}