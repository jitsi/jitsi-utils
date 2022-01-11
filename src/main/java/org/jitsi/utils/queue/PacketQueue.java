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

import org.jitsi.utils.logging.*;
import org.json.simple.*;
import org.jetbrains.annotations.*;

import java.lang.*;
import java.lang.SuppressWarnings;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * An abstract queue of packets.
 *
 * @author Boris Grozev
 * @author Yura Yaroshevich
 */
public class PacketQueue<T>
{
    /**
     * The {@link Logger} used by the {@link PacketQueue} class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(PacketQueue.class);

    /**
     * The default value for the {@code enableStatistics} constructor argument.
     */
    private static boolean enableStatisticsDefault = false;

    /**
     * Sets the default value for the {@code enableStatistics} constructor
     * parameter.
     *
     * @param enable the value to set.
     */
    public static void setEnableStatisticsDefault(boolean enable)
    {
        enableStatisticsDefault = enable;
    }

    public static boolean getEnableStatisticsDefault()
    {
        return enableStatisticsDefault;
    }

    /**
     * The underlying {@link BlockingQueue} which holds packets.
     */
    @NotNull private final BlockingQueue<T> queue;

    /**
     * The {@link Observer} instance optionally used to collect and print
     * detailed statistics about this queue.
     */
    protected final Observer<T> observer;

    /**
     * The {@link AsyncQueueHandler} to perpetually read packets
     * from {@link #queue} on separate thread and handle them with provided
     * packet handler.
     */
    @NotNull private final AsyncQueueHandler<T> asyncQueueHandler;

    /**
     * A string used to identify this {@link PacketQueue} for logging purposes.
     */
    @NotNull private final String id;

    /**
     * Whether this queue has been closed. Field is denoted as volatile,
     * because it is set in one thread and could be read in while loop in other.
     */
    private volatile boolean closed = false;

    /**
     * The maximum number of items the queue can contain before it starts
     * dropping items.
     */
    private final int capacity;

    /**
     * Handles dropped packets and exceptions thrown while processing.
     */
    @NotNull
    private ErrorHandler errorHandler = new ErrorHandler(){};

    /**
     * Creates a queue observer.
     */
    protected Observer<T> createObserver(Clock clock)
    {
        return new QueueStatisticsObserver<>(this, clock);
    }

    /**
     * Initializes a new {@link PacketQueue} instance.
     * @param capacity the capacity of the queue.  {@link Integer#MAX_VALUE} for
     *                 unbounded.
     * @param enableStatistics whether detailed statistics should be gathered by
     * constructing an {@link Observer}.
     * (In the base {@link PacketQueue} class this will be a {@link QueueStatisticsObserver}
     * but subclasses can override this).
     * This might affect performance. A value of {@code null} indicates that
     * the default {@link #enableStatisticsDefault} value will be used.
     * @param id the ID of the packet queue, to be used for logging.
     * @param packetHandler An handler to be used by the queue for
     * packets read from it.  The queue will start its own tasks on
     * {@param executor}, which will read packets from the queue and execute
     * {@code handler.handlePacket} on them.
     * @param executor An executor service to use to execute
     * packetHandler for items added to queue.
     * @param clock If {@param enableStatistics} is true (or resolves as true),
     *              a clock to use to construct the {@link Observer}.
     */
    public PacketQueue(
            int capacity,
            Boolean enableStatistics,
            @NotNull String id,
            @NotNull PacketHandler<T> packetHandler,
            ExecutorService executor,
            Clock clock)
    {
        this(capacity, enableStatistics, id, packetHandler, executor, clock, true);
    }

    /**
     * Initializes a new {@link PacketQueue} instance.
     * @param capacity the capacity of the queue.  {@link Integer#MAX_VALUE} for
     *                 unbounded.
     * @param enableStatistics whether detailed statistics should be gathered by
     * constructing an {@link Observer}.
     * (In the base {@link PacketQueue} class this will be a {@link QueueStatisticsObserver}
     * but subclasses can override this).
     * This might affect performance. A value of {@code null} indicates that
     * the default {@link #enableStatisticsDefault} value will be used.
     * @param id the ID of the packet queue, to be used for logging.
     * @param packetHandler An handler to be used by the queue for
     * packets read from it.  The queue will start its own tasks on
     * {@param executor}, which will read packets from the queue and execute
     * {@code handler.handlePacket} on them.
     * @param executor An executor service to use to execute
     * packetHandler for items added to queue.
     * @param clock If {@param enableStatistics} is true (or resolves as true),
     *              a clock to use to construct the {@link Observer}.
     * @param interruptOnClose whether the running task (if any) should be interrupted when the queue is closed. This
     * is useful when the queue is closed from within a task.
     */
    public PacketQueue(
        int capacity,
        Boolean enableStatistics,
        @NotNull String id,
        @NotNull PacketHandler<T> packetHandler,
        ExecutorService executor,
        Clock clock,
        boolean interruptOnClose)
    {
        this.id = id;
        this.capacity = capacity;
        queue = new LinkedBlockingQueue<>(capacity);

        asyncQueueHandler = new AsyncQueueHandler<>(
            queue,
            new HandlerAdapter(packetHandler),
            id,
            executor,
            packetHandler.maxSequentiallyProcessedPackets(),
            interruptOnClose);

        if (enableStatistics == null)
        {
            enableStatistics = enableStatisticsDefault;
        }

        observer = enableStatistics ? createObserver(clock) : null;

        logger.debug("Initialized a PacketQueue instance with ID " + id);
    }

    /**
     * Initializes a new {@link PacketQueue} instance.
     * @param capacity the capacity of the queue.
     * @param enableStatistics whether detailed statistics should be gathered
     * using a {@link QueueStatisticsObserver} as a default queue observer.
     * This might affect performance. A value of {@code null} indicates that
     * the default {@link #enableStatisticsDefault} value will be used.
     * @param id the ID of the packet queue, to be used for logging.
     * @param packetHandler An handler to be used by the queue for
     * packets read from it.  The queue will start its own tasks on
     * {@param executor}, which will read packets from the queue and execute
     * {@code handler.handlePacket} on them.
     * @param executor An executor service to use to execute
     * packetHandler for items added to queue.
     */
    public PacketQueue(
        int capacity,
        Boolean enableStatistics,
        @NotNull String id,
        @NotNull PacketHandler<T> packetHandler,
        ExecutorService executor)
    {
        this(capacity, enableStatistics, id, packetHandler, executor, Clock.systemUTC());
    }

    /**
     * Adds a specific packet ({@code T}) instance to the queue.
     * @param pkt the packet to add.
     */
    public void add(T pkt)
    {
        if (closed)
            return;

        while (!queue.offer(pkt))
        {
            // Drop from the head of the queue.
            T p = queue.poll();
            if (p != null)
            {
                if (observer != null)
                {
                    observer.dropped(p);
                }
                errorHandler.packetDropped();

                // Call release on dropped packet to allow proper implementation
                // of object pooling by PacketQueue users
                releasePacket(p);
            }
        }

        if (observer != null)
        {
            observer.added(pkt);
        }

        asyncQueueHandler.handleQueueItemsUntilEmpty();
    }

    /**
     * Closes current <tt>PacketQueue</tt> instance. No items will be added
     * to queue when it's closed.  Asynchronous queue processing by
     * {@link #asyncQueueHandler} is stopped.
     */
    public void close()
    {
        if (!closed)
        {
            closed = true;

            asyncQueueHandler.cancel();

            T item;
            while ((item = queue.poll()) != null) {
                releasePacket(item);
            }
        }
    }

    /**
     * Releases packet when it is handled by provided packet handler.
     * This method is not called when <tt>PacketQueue</tt> was created without
     * handler and hence no automatic queue processing is done.
     * Default implementation is empty, but it might be used to implement
     * packet pooling to re-use them.
     * @param pkt packet to release
     */
    protected void releasePacket(T pkt)
    {
    }

    /** Get the current number of packets queued in this queue. */
    public int size()
    {
        return queue.size();
    }

    /** Get the maximum number of packets queued in this queue. */
    public int capacity()
    {
        return capacity;
    }

    /** Get the ID of this queue. */
    public String id()
    {
        return id;
    }

    /**
     * Gets a JSON representation of the parts of this object's state that
     * are deemed useful for debugging.
     */
    @SuppressWarnings("unchecked")
    public JSONObject getDebugState()
    {
        JSONObject debugState = new JSONObject();
        debugState.put("id", id);
        debugState.put("capacity", capacity);
        debugState.put("closed", closed);
        debugState.put(
                "statistics",
                observer == null
                        ? null : observer.getStats());

        return debugState;
    }

    /**
     * Sets the handler of errors (packets dropped or exceptions caught while
     * processing).
     * @param errorHandler the handler to set.
     */
    public void setErrorHandler(@NotNull ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    /**
     * A simple interface to handle packets.
     * @param <T> the type of the packets.
     */
    public interface PacketHandler<T>
    {
        /**
         * Does something with a packet.
         * @param pkt the packet to do something with.
         * @return {@code true} if the operation was successful, and
         * {@code false} otherwise.
         */
        boolean handlePacket(T pkt);

        /**
         * Specifies the number of packets allowed to be processed sequentially
         * without yielding control to executor's thread. Specifying positive
         * number will allow other possible queues sharing same
         * {@link ExecutorService} to process their packets.
         * @return positive value to specify max number of packets which allows
         * implementation of cooperative multi-tasking between different
         * {@link PacketQueue} sharing same {@link ExecutorService}.
         */
        default long maxSequentiallyProcessedPackets()
        {
            return -1;
        }
    }

    /**
     * An interface to observe a queue, to collect statistics or similar.
     */
    public interface Observer<T>
    {
        /** Called when a packet is added to a queue. */
        void added(T pkt);

        /** Called when a packet is removed from a queue. */
        void removed(T pkt);

        /** Called when a packet is dropped from a queue. */
        void dropped(T pkt);

        /** Get statistics gathered by this observer. */
        Map<?, ?> getStats();
    }

    /**
     * An adapter class implementing {@link AsyncQueueHandler.Handler}
     * to wrap {@link PacketHandler}.
     */
    private final class HandlerAdapter implements AsyncQueueHandler.Handler<T>
    {
        /**
         * An actual handler of packets.
         */
        private final PacketHandler<T> handler;

        /**
         * Constructs adapter of {@link PacketHandler} to
         * {@link AsyncQueueHandler.Handler} interface.
         * @param handler an handler instance to adapt
         */
        HandlerAdapter(PacketHandler<T> handler)
        {
            this.handler = handler;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleItem(T item)
        {
            if (observer != null)
            {
                observer.removed(item);
            }

            try
            {
                handler.handlePacket(item);
            }
            catch (Throwable t)
            {
                errorHandler.packetHandlingFailed(t);
            }
        }
    }
}
