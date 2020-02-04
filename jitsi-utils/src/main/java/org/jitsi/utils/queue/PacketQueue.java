package org.jitsi.utils.queue;

import org.jetbrains.annotations.*;
import org.json.simple.*;

import java.util.concurrent.*;

public interface PacketQueue<T>
{
    /**
     * Adds a specific packet ({@code T}) instance to the queue.
     * @param pkt the packet to add.
     */
    void add(T pkt);

    /**
     * Removes and returns the packet ({@code T}) at the head of this queue.
     * Blocks until there is a packet in the queue. Returns {@code null} if
     * the queue is closed or gets closed while waiting for a packet to be added.
     * @return the packet at the head of this queue.
     */
    T get();

    /**
     * Removes and returns the packet ({@code T}) at the head of this queue, if
     * the queue is non-empty. If the queue is closed or empty, returns null
     * without blocking.
     * @return the packet at the head of this queue, or null if the queue is
     * empty.
     */
    T poll();

    /**
     * Closes current <tt>PacketQueue</tt> instance. No items will be added
     * to queue when it's closed. Threads which were blocked in {@link #get()}
     * will receive <tt>null</tt>. Asynchronous queue processing by
     * {@link #asyncQueueHandler} is stopped.
     */
    void close();

    /**
     * Gets a JSON representation of the parts of this object's state that
     * are deemed useful for debugging.
     */
    JSONObject getDebugState();

    /**
     * Sets the handler of errors (packets dropped or exceptions caught while
     * processing).
     * @param errorHandler the handler to set.
     */
    void setErrorHandler(@NotNull ErrorHandler errorHandler);

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
}
