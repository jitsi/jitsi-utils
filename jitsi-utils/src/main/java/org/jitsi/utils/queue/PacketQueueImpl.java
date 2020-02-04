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

import java.util.concurrent.*;

/**
 * An abstract queue of packets.
 *
 * @author Boris Grozev
 * @author Yura Yaroshevich
 */
public class PacketQueueImpl<T>
    implements PacketQueue<T>
{
    /**
     * The {@link Logger} used by the {@link PacketQueueImpl} class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PacketQueue.class.getName());

    /**
     * The underlying {@link BlockingQueue} which holds packets.
     * Used as synchronization object between {@link #close()}, {@link #get()}
     * and {@link #doAdd(Object)}.
     */
    private final BlockingQueue<T> queue;

    /**
     * The optionally used {@link AsyncQueueHandler} to perpetually read packets
     * from {@link #queue} on separate thread and handle them with provided
     * packet handler.
     */
    private final AsyncQueueHandler<T> asyncQueueHandler;

    /**
     * A string used to identify this {@link PacketQueueImpl} for logging purposes.
     */
    private final String id;

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
     * Initializes a new {@link PacketQueueImpl} instance.
     * @param capacity the capacity of the queue.
     * @param id the ID of the packet queue, to be used for logging.
     * @param packetHandler An optional handler to be used by the queue for
     * packets read from it. If a non-null value is passed the queue will
     * start its own thread, which will read packets from the queue and execute
     * {@code handler.handlePacket} on them. If set to null, no thread will be
     * created, and the queue will provide access to the head element via
     * {@link #get()} and {@link #poll()}.
     * @param executor An optional executor service to use to execute
     * packetHandler for items added to queue.
     */
    PacketQueueImpl(
        int capacity,
        String id,
        PacketHandler<T> packetHandler,
        ExecutorService executor)
    {
        this.id = id;
        this.capacity = capacity;
        queue = new ArrayBlockingQueue<>(capacity);

        if (packetHandler != null)
        {
            asyncQueueHandler = new AsyncQueueHandler<>(
                queue,
                new HandlerAdapter(packetHandler),
                id,
                executor,
                packetHandler.maxSequentiallyProcessedPackets());
        }
        else
        {
            asyncQueueHandler = null;
        }

        logger.debug("Initialized a PacketQueue instance with ID " + id);
    }

    /**
     * Hook for derived class to know a packet was added.
     */
    protected void packetAdded(T pkt) {};

    /**
     * Hook for derived class to know a packet was removed.
     */
    protected void packetRemoved(T pkt) {};

    /**
     * Hook for derived class to know a packet was dropped.
     */
    protected void packetDropped(T pkt) {};

    /**
     * Adds a specific packet ({@code T}) instance to the queue.
     * @param pkt the packet to add.
     */
    @Override
    public void add(T pkt)
    {
        doAdd(pkt);
    }

    /**
     * Adds a specific packet ({@code T}) instance to the queue.
     * @param pkt the packet to add.
     */
    private void doAdd(T pkt)
    {
        if (closed)
            return;

        while (!queue.offer(pkt))
        {
            // Drop from the head of the queue.
            T p = queue.poll();
            if (p != null)
            {
                packetDropped(pkt);
                errorHandler.packetDropped();

                // Call release on dropped packet to allow proper implementation
                // of object pooling by PacketQueue users
                releasePacket(p);
            }
        }

        packetAdded(pkt);

        synchronized (queue)
        {
            // notify single thread because only 1 item was added into queue
            queue.notify();
        }

        if (asyncQueueHandler != null)
        {
            asyncQueueHandler.handleQueueItemsUntilEmpty();
        }
    }

    /**
     * Removes and returns the packet ({@code T}) at the head of this queue.
     * Blocks until there is a packet in the queue. Returns {@code null} if
     * the queue is closed or gets closed while waiting for a packet to be added.
     * @return the packet at the head of this queue.
     */
    @Override
    public T get()
    {
        if (asyncQueueHandler != null)
        {
            // If the queue was configured with a handler, it is running its
            // own reading thread, and reading from it via this interface would
            // not provide consistent results.
            throw new IllegalStateException(
                "Trying to read from a queue with a configured handler.");
        }

        while (true)
        {
            if (closed)
                return null;
            synchronized (queue)
            {
                T pkt = queue.poll();
                if (pkt != null)
                {
                    packetRemoved(pkt);
                    return pkt;
                }

                try
                {
                    queue.wait();
                }
                catch (InterruptedException ignored)
                {}
            }
        }
    }

    /**
     * Removes and returns the packet ({@code T}) at the head of this queue, if
     * the queue is non-empty. If the queue is closed or empty, returns null
     * without blocking.
     * @return the packet at the head of this queue, or null if the queue is
     * empty.
     */
    @Override
    public T poll()
    {
        if (closed)
            return null;

        if (asyncQueueHandler != null)
        {
            // If the queue was configured with a handler, it is running its
            // own reading thread, and reading from it via this interface would
            // not provide consistent results.
            throw new IllegalStateException(
                "Trying to read from a queue with a configured handler.");
        }

        synchronized (queue)
        {
            T pkt = queue.poll();

            if (pkt != null)
            {
                packetRemoved(pkt);
            }

            return pkt;
        }
    }

    /**
     * Closes current <tt>PacketQueue</tt> instance. No items will be added
     * to queue when it's closed. Threads which were blocked in {@link #get()}
     * will receive <tt>null</tt>. Asynchronous queue processing by
     * {@link #asyncQueueHandler} is stopped.
     */
    @Override
    public void close()
    {
        if (!closed)
        {
            closed = true;

            if (asyncQueueHandler != null)
            {
                asyncQueueHandler.cancel();
            }

            synchronized (queue)
            {
                // notify all threads because PacketQueue is closed and all
                // threads waiting on queue must stop reading it.
                queue.notifyAll();
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

    /**
     * Gets a JSON representation of the parts of this object's state that
     * are deemed useful for debugging.
     */
    @Override
    public JSONObject getDebugState()
    {
        JSONObject debugState = new JSONObject();
        debugState.put("id", id);
        debugState.put("capacity", capacity);
        debugState.put("closed", closed);

        return debugState;
    }

    /**
     * Sets the handler of errors (packets dropped or exceptions caught while
     * processing).
     * @param errorHandler the handler to set.
     */
    @Override
    public void setErrorHandler(@NotNull ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    /**
     * An adapter class implementing {@link AsyncQueueHandler.Handler<T>}
     * to wrap {@link PacketHandler<T>}.
     */
    private final class HandlerAdapter implements AsyncQueueHandler.Handler<T>
    {
        /**
         * An actual handler of packets.
         */
        private final PacketHandler<T> handler;

        /**
         * Constructs adapter of {@link PacketHandler<T>} to
         * {@link AsyncQueueHandler.Handler<T>} interface.
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
            try
            {
                packetRemoved(item);
                handler.handlePacket(item);
            }
            catch (Throwable t)
            {
                errorHandler.packetHandlingFailed(t);
            }
            finally
            {
                releasePacket(item);
            }
        }
    }
}
