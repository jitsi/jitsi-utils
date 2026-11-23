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
import java.time.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Test various aspects of {@link PacketQueue} implementation.
 *
 * @author Yura Yaroshevich
 */
public class PacketQueueTests
{
    @Test
    public void testClosingQueueImmediatelyStopsExecutors()
        throws Exception
    {
        final AtomicInteger tasksExecuted = new AtomicInteger(0);
        final BlockedExecutor blockedExecutor
            = new BlockedExecutor();

        final DummyQueue dummyQueue = new DummyQueue(10,
            pkt -> {
                tasksExecuted.incrementAndGet();
                return true;
            },
            blockedExecutor);

        for (int i = 0; i < 10; i++)
        {
            dummyQueue.add(new DummyQueue.Dummy());
        }

        /* No tasks executed */
        Assertions.assertEquals(tasksExecuted.get(), 0);

        dummyQueue.close();

        blockedExecutor.start();

        // The executor is single-threaded and runs tasks in FIFO order, so when this task completes, every task that
        // the queue submitted before close() has already run (or was cancelled).
        blockedExecutor.submit(() -> { }).get(5, TimeUnit.SECONDS);

        /* No tasks executed */
        Assertions.assertEquals(tasksExecuted.get(), 0);

        dummyQueue.close();

        blockedExecutor.shutdown();

        dummyQueue.close();

        /* Still no tasks executed */
        Assertions.assertEquals(tasksExecuted.get(), 0);
    }

    @Test
    public void testAddingWhenCapacityReachedRemovesOldestItem()
        throws Exception
    {
        final int capacity = 10;
        final BlockedExecutor blockedExecutor
            = new BlockedExecutor();
        // The handler runs on the executor thread, where a failed assertion is only logged. Record the IDs there and
        // assert on them from the test thread.
        final List<Integer> handledIds = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch allHandled = new CountDownLatch(capacity);

        final DummyQueue dummyQueue = new DummyQueue(capacity,
            pkt -> {
                handledIds.add(pkt.id);
                allHandled.countDown();
                return true;
            },
            blockedExecutor);

        final List<Integer> expectedIds = new ArrayList<>();
        for (int i = 0; i < capacity + 1; i++)
        {
            DummyQueue.Dummy item = new DummyQueue.Dummy();
            item.id = i;

            dummyQueue.add(item);
            if (i > 0)
            {
                expectedIds.add(i);
            }
        }

        blockedExecutor.start();

        Assertions.assertTrue(allHandled.await(10, TimeUnit.SECONDS), "Expected all queued items to be handled");
        // The executor is single-threaded and FIFO, so this task runs after the reader has exited. No more items can
        // be handled after it completes.
        blockedExecutor.submit(() -> { }).get(5, TimeUnit.SECONDS);

        // Item 0 was removed from the queue when item 10 was added at full capacity.
        Assertions.assertEquals(expectedIds, new ArrayList<>(handledIds));

        dummyQueue.close();
        blockedExecutor.shutdown();
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

        final boolean completed = queueCompletion.await(5, TimeUnit.SECONDS);
        Assertions
            .assertTrue(completed, "Expected all queued items are handled at this time point");

        Future<?> executorCompletion = singleThreadExecutor.submit(() -> {
            // do nothing, just pump Runnable via executor's thread to
            // verify it's not stuck
        });

        try
        {
            executorCompletion.get(5, TimeUnit.SECONDS);
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

        // The executor does not run any task until start() is called, so both queues are full before the first item
        // is handled. If the executor ran while items were still being added, a reader could yield and be
        // rescheduled before the other queue had submitted its reader, and then handle two items in a row.
        final BlockedExecutor singleThreadExecutor
            = new BlockedExecutor();

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

        singleThreadExecutor.start();

        final boolean completed
            = completionGuard.await(10, TimeUnit.SECONDS);
        Assertions
            .assertTrue(completed, "Expected all queued items are handled "
                + "at this time point");

        Assertions.assertTrue(
            queuesEvenlyProcessed.get(),
            "Queues sharing same thread with configured cooperative"
                + " multi-tasking must yield execution to be processed evenly");

        singleThreadExecutor.shutdownNow();
    }

    /**
     * Two queues share one executor thread while producers add items concurrently. Each executor task must handle
     * at most maxSequentiallyProcessedPackets items before it yields, and all items must be handled without close().
     * The executor is held until each producer has added some items, so the first reader of each queue always finds
     * more items than the limit, also when the executor is faster than the producers.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 3 })
    public void testQueuesYieldWhileItemsArrive(int maxSequentiallyProcessedPackets)
        throws Exception
    {
        final int itemsPerQueue = 10_000;
        final AtomicInteger itemsInCurrentTask = new AtomicInteger();
        final AtomicInteger maxItemsInOneTask = new AtomicInteger();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        {
            @Override
            protected void beforeExecute(Thread t, Runnable r)
            {
                itemsInCurrentTask.set(0);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                maxItemsInOneTask.accumulateAndGet(itemsInCurrentTask.get(), Math::max);
            }
        };

        final CountDownLatch allHandled = new CountDownLatch(2 * itemsPerQueue);
        final PacketQueue.PacketHandler<DummyQueue.Dummy> handler = new PacketQueue.PacketHandler<DummyQueue.Dummy>()
        {
            @Override
            public boolean handlePacket(DummyQueue.Dummy pkt)
            {
                itemsInCurrentTask.incrementAndGet();
                allHandled.countDown();
                return true;
            }

            @Override
            public long maxSequentiallyProcessedPackets()
            {
                return maxSequentiallyProcessedPackets;
            }
        };

        // The capacity holds all items, so no item is dropped.
        final DummyQueue queue1 = new DummyQueue(itemsPerQueue, handler, executor);
        final DummyQueue queue2 = new DummyQueue(itemsPerQueue, handler, executor);

        // The first task on the executor waits until each producer has added backlogItems items. The readers are
        // queued behind it, so each of them starts with a backlog, and the producers keep adding while they run.
        final int backlogItems = 100;
        final CountDownLatch backlogReady = new CountDownLatch(2);
        final AtomicBoolean backlogFormed = new AtomicBoolean();
        executor.execute(() -> {
            try
            {
                backlogFormed.set(backlogReady.await(30, TimeUnit.SECONDS));
            }
            catch (InterruptedException ignored)
            {
            }
        });

        final CountDownLatch start = new CountDownLatch(1);
        final List<Thread> producers = new ArrayList<>();
        for (DummyQueue queue : Arrays.asList(queue1, queue2))
        {
            Thread producer = new Thread(() -> {
                try
                {
                    start.await();
                }
                catch (InterruptedException e)
                {
                    return;
                }
                for (int i = 0; i < itemsPerQueue; i++)
                {
                    queue.add(new DummyQueue.Dummy());
                    if (i == backlogItems - 1)
                    {
                        backlogReady.countDown();
                    }
                }
            });
            producers.add(producer);
            producer.start();
        }

        start.countDown();
        for (Thread producer : producers)
        {
            producer.join(30_000);
            Assertions.assertFalse(producer.isAlive(), "Producer did not finish");
        }

        Assertions.assertTrue(allHandled.await(30, TimeUnit.SECONDS), "Expected all queued items to be handled");

        // Wait for afterExecute() of the last task.
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        Assertions.assertTrue(backlogFormed.get(), "The producers did not add the backlog items");
        // A reader that starts with a backlog handles exactly the limit before it yields. A higher value means that a
        // reader did not yield.
        Assertions.assertEquals(maxSequentiallyProcessedPackets, maxItemsInOneTask.get(),
            "Maximum number of items that one executor task handled");
        Assertions.assertEquals(0, queue1.size());
        Assertions.assertEquals(0, queue2.size());

        queue1.close();
        queue2.close();
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
            = completionGuard.await(60, TimeUnit.SECONDS);
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
    {
        final BlockedExecutor blockedExecutor
            = new BlockedExecutor();

        final List<DummyQueue.Dummy> releasedPackets = new ArrayList<>();

        final int queueCapacity = 1;

        final DummyQueue queue = new DummyQueue(queueCapacity,
            pkt -> true,
            blockedExecutor)
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

        blockedExecutor.start();

        Assertions.assertEquals(
            itemsToEnqueue - queueCapacity, releasedPackets.size());

        int seed = 1;
        for (DummyQueue.Dummy releasedPacket : releasedPackets)
        {
            Assertions.assertEquals(seed, releasedPacket.id);
            seed++;
        }

        blockedExecutor.shutdown();
    }
    
    @Test
    public void testReleasePacketCalledForPacketsInQueueWhenClosing()
        throws Exception
    {
        final BlockedExecutor blockedExecutor
            = new BlockedExecutor();

        final List<DummyQueue.Dummy> releasedPackets = new ArrayList<>();

        final int queueCapacity = 10;

        final DummyQueue queue = new DummyQueue(queueCapacity,
            pkt -> true,
            blockedExecutor)
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
        blockedExecutor.start();
        Assertions.assertEquals(queueCapacity, releasedPackets.size());

        int seed = 1;
        for (DummyQueue.Dummy releasedPacket : releasedPackets)
        {
            Assertions.assertEquals(seed, releasedPacket.id);
            seed++;
        }

        blockedExecutor.shutdown();
    }

    @Test
    public void testCloseReleasesQueuedItems()
    {
        final BlockedExecutor blockedExecutor = new BlockedExecutor();
        final List<DummyQueue.Dummy> released = new ArrayList<>();
        final DummyQueue dummyQueue = new DummyQueue(10, pkt -> true, blockedExecutor)
        {
            @Override
            protected void releasePacket(Dummy pkt)
            {
                released.add(pkt);
            }
        };

        final List<DummyQueue.Dummy> added = new ArrayList<>();
        for (int i = 0; i < 3; i++)
        {
            DummyQueue.Dummy dummy = new DummyQueue.Dummy();
            Assertions.assertTrue(dummyQueue.tryAdd(dummy));
            added.add(dummy);
        }
        Assertions.assertTrue(released.isEmpty());

        /* The executor never ran, so all items are still queued and must be released by close(). */
        dummyQueue.close();
        Assertions.assertEquals(added, released);

        blockedExecutor.shutdown();
    }

    @Test
    public void testAddAfterCloseIsRejected()
    {
        final BlockedExecutor blockedExecutor = new BlockedExecutor();
        final List<DummyQueue.Dummy> released = new ArrayList<>();
        final DummyQueue dummyQueue = new DummyQueue(10, pkt -> true, blockedExecutor)
        {
            @Override
            protected void releasePacket(Dummy pkt)
            {
                released.add(pkt);
            }
        };

        dummyQueue.close();

        /* The caller keeps ownership of a rejected item: it is neither queued nor released. */
        Assertions.assertFalse(dummyQueue.tryAdd(new DummyQueue.Dummy()));
        Assertions.assertEquals(0, dummyQueue.size());
        Assertions.assertTrue(released.isEmpty());

        blockedExecutor.shutdown();
    }

    /**
     * Adds items from several threads while another thread closes the queue, and checks that every item which
     * tryAdd() accepted was either handled or released exactly once, and that no rejected item was.
     */
    @Test
    public void testConcurrentAddAndClose()
        throws Exception
    {
        final int producers = 8;
        final int itemsPerProducer = 500;
        final int iterations = 50;

        for (int iteration = 0; iteration < iterations; iteration++)
        {
            final ExecutorService executor = Executors.newFixedThreadPool(2);
            final ConcurrentHashMap<DummyQueue.Dummy, AtomicInteger> handled = new ConcurrentHashMap<>();
            final ConcurrentHashMap<DummyQueue.Dummy, AtomicInteger> released = new ConcurrentHashMap<>();
            final DummyQueue dummyQueue = new DummyQueue(
                64,
                pkt -> {
                    handled.computeIfAbsent(pkt, k -> new AtomicInteger()).incrementAndGet();
                    return true;
                },
                executor)
            {
                @Override
                protected void releasePacket(Dummy pkt)
                {
                    released.computeIfAbsent(pkt, k -> new AtomicInteger()).incrementAndGet();
                }
            };

            final ConcurrentHashMap<DummyQueue.Dummy, Boolean> accepted = new ConcurrentHashMap<>();
            final CountDownLatch start = new CountDownLatch(1);
            // Tripped by a producer part way through, so that close() runs while items are still being added. That
            // producer then waits for close() to complete before continuing, so its remaining adds are guaranteed
            // to be rejected (the other producers keep racing with close()).
            final CountDownLatch closeNow = new CountDownLatch(1);
            final CountDownLatch closeDone = new CountDownLatch(1);
            final int closeAfterItems = 1 + (iteration * 97) % (itemsPerProducer / 2);
            final List<Thread> threads = new ArrayList<>();
            for (int p = 0; p < producers; p++)
            {
                Thread t = new Thread(() -> {
                    try
                    {
                        start.await();
                    }
                    catch (InterruptedException e)
                    {
                        return;
                    }
                    for (int i = 0; i < itemsPerProducer; i++)
                    {
                        DummyQueue.Dummy dummy = new DummyQueue.Dummy();
                        accepted.put(dummy, dummyQueue.tryAdd(dummy));
                        if (i == closeAfterItems)
                        {
                            closeNow.countDown();
                            try
                            {
                                closeDone.await(10, TimeUnit.SECONDS);
                            }
                            catch (InterruptedException e)
                            {
                                return;
                            }
                        }
                    }
                    // In case this producer never reached closeAfterItems (it must not block the closer).
                    closeNow.countDown();
                });
                threads.add(t);
                t.start();
            }
            Thread closer = new Thread(() -> {
                try
                {
                    closeNow.await();
                }
                catch (InterruptedException e)
                {
                    return;
                }
                dummyQueue.close();
                closeDone.countDown();
            });
            threads.add(closer);
            closer.start();

            start.countDown();
            for (Thread t : threads)
            {
                t.join(10000);
                Assertions.assertFalse(
                    t.isAlive(),
                    "Thread did not finish: " + t.getName() + " " + Arrays.toString(t.getStackTrace()));
            }
            executor.shutdown();
            Assertions.assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            Assertions.assertEquals(producers * itemsPerProducer, accepted.size());
            int acceptedCount = 0;
            for (Map.Entry<DummyQueue.Dummy, Boolean> e : accepted.entrySet())
            {
                int h = handled.getOrDefault(e.getKey(), new AtomicInteger()).get();
                int r = released.getOrDefault(e.getKey(), new AtomicInteger()).get();
                if (e.getValue())
                {
                    acceptedCount++;
                    Assertions.assertEquals(1, h + r,
                        "Accepted item must be handled or released exactly once (handled=" + h + ", released=" + r
                            + ")");
                }
                else
                {
                    Assertions.assertEquals(0, h + r, "Rejected item must be neither handled nor released");
                }
            }
            /* Sanity check that the close happened while items were still being added. */
            Assertions.assertTrue(acceptedCount > 0);
            Assertions.assertTrue(accepted.containsValue(false), "close() must have run before production finished");
            Assertions.assertEquals(0, dummyQueue.size());
        }
    }

    /**
     * Deterministically exercises the window between tryAdd() deciding the queue is open and the item landing in the
     * underlying queue: close() runs (and drains) in that window, and the item must still end up released rather
     * than stranded. The observer's added() callback is invoked inside that window, so a test observer is used to
     * block there until close() has run.
     */
    @Test
    public void testCloseDuringAddReleasesItem()
        throws Exception
    {
        final BlockedExecutor blockedExecutor = new BlockedExecutor();
        final CountDownLatch insideAdd = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        final List<DummyQueue.Dummy> released = new ArrayList<>();
        final PacketQueue<DummyQueue.Dummy> queue = new PacketQueue<DummyQueue.Dummy>(
            10, true, "test", pkt -> true, blockedExecutor)
        {
            @Override
            protected Observer<DummyQueue.Dummy> createObserver(Clock clock)
            {
                return new Observer<DummyQueue.Dummy>()
                {
                    @Override
                    public void added(DummyQueue.Dummy pkt)
                    {
                        insideAdd.countDown();
                        try
                        {
                            closed.await(5, TimeUnit.SECONDS);
                        }
                        catch (InterruptedException ignored)
                        {
                        }
                    }

                    @Override
                    public void removed(DummyQueue.Dummy pkt) { }

                    @Override
                    public void dropped(DummyQueue.Dummy pkt) { }

                    @Override
                    public JsonNode getStats()
                    {
                        return null;
                    }
                };
            }

            @Override
            protected void releasePacket(DummyQueue.Dummy pkt)
            {
                synchronized (released)
                {
                    released.add(pkt);
                }
            }
        };

        final DummyQueue.Dummy dummy = new DummyQueue.Dummy();
        final AtomicBoolean result = new AtomicBoolean();
        Thread adder = new Thread(() -> result.set(queue.tryAdd(dummy)));
        adder.start();

        Assertions.assertTrue(insideAdd.await(5, TimeUnit.SECONDS));
        queue.close();
        closed.countDown();
        adder.join(5000);
        Assertions.assertFalse(adder.isAlive(), "tryAdd() did not return");

        /* tryAdd() had already committed to adding, so the queue owns the item and must have released it. */
        Assertions.assertTrue(result.get());
        Assertions.assertEquals(Collections.singletonList(dummy), released);
        Assertions.assertEquals(0, queue.size());

        blockedExecutor.shutdown();
    }

    /**
     * Once cancelled, the reader must not be rescheduled by a racing handleQueueItemsUntilEmpty(), otherwise items
     * can be handled after the queue was closed.
     */
    @Test
    public void testReaderIsNotRescheduledAfterCancel()
    {
        final AtomicInteger submissions = new AtomicInteger();
        final ExecutorService executor = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        {
            @Override
            public Future<?> submit(Runnable task)
            {
                submissions.incrementAndGet();
                return super.submit(task);
            }
        };
        final BlockingQueue<DummyQueue.Dummy> queue = new LinkedBlockingQueue<>();
        final AsyncQueueHandler<DummyQueue.Dummy> handler
            = new AsyncQueueHandler<>(queue, item -> { }, "test", executor, 0, false);

        handler.cancel();
        queue.add(new DummyQueue.Dummy());
        handler.handleQueueItemsUntilEmpty();

        Assertions.assertEquals(0, submissions.get());
        executor.shutdown();
    }
}
