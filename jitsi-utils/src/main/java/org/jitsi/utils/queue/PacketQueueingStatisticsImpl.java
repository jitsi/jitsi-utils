package org.jitsi.utils.queue;

import org.jetbrains.annotations.*;
import org.json.simple.*;

import java.util.concurrent.*;

public class PacketQueueingStatisticsImpl<T>
    implements PacketQueue<T>
{
    private PacketQueue<QueueElement<T>> innerQueue;
    private QueueStatistics queueStatistics = new QueueStatistics();

    public PacketQueueingStatisticsImpl(
        int capacity,
        String id,
        PacketHandler<T> packetHandler,
        ExecutorService executor)
    {
        innerQueue = new PacketQueueImpl<QueueElement<T>>(
            capacity, id, packetHandler != null ? new PacketHandlerWrapper<>(packetHandler) : null,
            executor) {

            @Override
            protected void packetAdded(QueueElement<T> pkt)
            {
                queueStatistics.add(pkt.pushTime);
            }

            @Override
            protected void packetRemoved(QueueElement<T> pkt)
            {
                queueStatistics.remove(pkt.pushTime, System.currentTimeMillis());
            }

            @Override
            protected void packetDropped(QueueElement<T> pkt)
            {
                queueStatistics.drop(pkt.pushTime, System.currentTimeMillis());
            }
        };
    }

    @Override
    public void add(T pkt)
    {
        innerQueue.add(new QueueElement<>(pkt));
    }

    @Override
    public T get()
    {
        QueueElement<T> elt = innerQueue.get();
        return elt != null ? elt.packet : null;
    }

    @Override
    public T poll()
    {
        QueueElement<T> elt = innerQueue.poll();
        return elt != null ? elt.packet : null;
    }

    @Override
    public void close()
    {
        innerQueue.close();
    }

    @Override
    public JSONObject getDebugState()
    {
        JSONObject debugState = innerQueue.getDebugState();
        debugState.put("statistics",queueStatistics.getStats());

        return debugState;
    }

    @Override
    public void setErrorHandler(@NotNull ErrorHandler errorHandler)
    {
        innerQueue.setErrorHandler(errorHandler);
    }

    private final static class QueueElement<T>
    {
        final T packet;
        final long pushTime;

        QueueElement(T p)
        {
            packet = p;
            pushTime = System.currentTimeMillis();
        }
    }

    private final static class PacketHandlerWrapper<T>
        implements PacketHandler<QueueElement<T>>
    {
        final PacketHandler<T> wrappedHandler;

        PacketHandlerWrapper(PacketHandler<T> w)
        {
            wrappedHandler = w;
        }

        @Override
        public boolean handlePacket(QueueElement<T> pkt)
        {
            return wrappedHandler.handlePacket(pkt.packet);
        }

        @Override
        public long maxSequentiallyProcessedPackets()
        {
            return wrappedHandler.maxSequentiallyProcessedPackets();
        }
    }
}
