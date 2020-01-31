package org.jitsi.utils.queue;

import org.jetbrains.annotations.*;
import org.json.simple.*;

import java.util.concurrent.*;

public interface PacketQueue<T>
{
    void add(T pkt);

    T get();

    T poll();

    void close();

    JSONObject getDebugState();

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
