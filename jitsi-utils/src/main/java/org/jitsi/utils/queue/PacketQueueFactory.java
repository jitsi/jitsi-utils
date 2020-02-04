package org.jitsi.utils.queue;

import sun.reflect.generics.reflectiveObjects.*;

import java.util.concurrent.*;

public class PacketQueueFactory
{
    /**
     * Sets the default value for the {@code enableStatistics} constructor
     * parameter.
     *
     * @param enable the value to set.
     */
    static void setEnableStatisticsDefault(boolean enable)
    {
        enableStatisticsDefault = enable;
    }

    /**
     * The default value for the {@code enableStatistics} constructor argument.
     */
    static boolean enableStatisticsDefault = false;

    /**
     * The default capacity of a {@link PacketQueueImpl}.
     */
    private final static int DEFAULT_CAPACITY = 256;

    /**
     * Returns a new {@link PacketQueue} instance.
     */
    public static <T> PacketQueue<T> getPacketQueue()
    {
        return getPacketQueue(null, "PacketQueue", null);
    }

    /**
     * Returns a new {@link PacketQueue} instance.
     */
    public static <T> PacketQueue<T> getPacketQueue(int capacity)
    {
        return getPacketQueue(capacity, false, null, "PacketQueue", null);
    }

    /**
    * Initializes a new {@link PacketQueue} instance.
    * @param enableStatistics whether detailed statistics should be gathered.
    * This might affect performance. A value of {@code null} indicates that
    * the default {@link #enableStatisticsDefault} value will be used.
    * @param id the ID of the packet queue, to be used for logging.
    * @param packetHandler An optional handler to be used by the queue for
    * packets read from it. If a non-null value is passed the queue will
    * start its own thread, which will read packets from the queue and execute
    * {@code handler.handlePacket} on them. If set to null, no thread will be
    * created, and the queue will provide access to the head element via
    * {@link PacketQueue#get()} and {@link PacketQueue#poll()}.
    */
   public static <T> PacketQueue<T> getPacketQueue(
       Boolean enableStatistics, String id, PacketQueue.PacketHandler<T> packetHandler)
   {
       return getPacketQueue(DEFAULT_CAPACITY, true, enableStatistics, id, packetHandler);
   }

    /**
     * Returns a new {@link PacketQueue} instance.
     * @param id the ID of the packet queue, to be used for logging.
     * @param packetHandler An optional handler to be used by the queue for
     * packets read from it. If a non-null value is passed the queue will
     * start its own thread, which will read packets from the queue and execute
     * {@code handler.handlePacket} on them. If set to null, no thread will be
     * created, and the queue will provide access to the head element via
     * {@link PacketQueue#get()} and {@link PacketQueue#poll()}.
     */
    public static <T> PacketQueue<T> getPacketQueue(int capacity, boolean copy,
        Boolean enableStatistics, String id,
        PacketQueue.PacketHandler<T> packetHandler)
    {
        return getPacketQueue(capacity, copy, enableStatistics, id, packetHandler, null);
    }

    /**
     * Initializes a new {@link PacketQueue} instance.
     * @param capacity the capacity of the queue.
     * @param copy whether the queue is to store the instances it is given via
     * the various {@code add} methods, or create a copy.
     * @param enableStatistics whether detailed statistics should be gathered.
     * This might affect performance. A value of {@code null} indicates that
     * the default {@link #enableStatisticsDefault} value will be used.
     * @param id the ID of the packet queue, to be used for logging.
     * @param packetHandler An optional handler to be used by the queue for
     * packets read from it. If a non-null value is passed the queue will
     * start its own thread, which will read packets from the queue and execute
     * {@code handler.handlePacket} on them. If set to null, no thread will be
     * created, and the queue will provide access to the head element via
     * {@link PacketQueue#get()} and {@link PacketQueue#poll()}.
     * @param executor An optional executor service to use to execute
     * packetHandler for items added to queue.
     */
    public static <T> PacketQueue<T> getPacketQueue(
        int capacity,
        boolean copy,
        Boolean enableStatistics,
        String id,
        PacketQueue.PacketHandler<T> packetHandler,
        ExecutorService executor)
    {
        if (copy)
        {
            throw new NotImplementedException();
        }

        if (enableStatistics == null)
        {
            enableStatistics = enableStatisticsDefault;
        }

        if (enableStatistics)
        {
            return new PacketQueueWithStatistics<>(capacity, id, packetHandler, executor);
        }
        else
        {
            return new PacketQueueImpl<>(capacity, id, packetHandler, executor);
        }
    }
}
