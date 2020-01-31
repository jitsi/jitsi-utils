package org.jitsi.utils.queue;

import org.json.simple.*;

import java.util.concurrent.*;

public class PacketSimpleStatisticsImpl<T>
    extends PacketQueueImpl<T>
{
    private QueueStatistics queueStatistics = new QueueStatistics();

    public PacketSimpleStatisticsImpl(
        int capacity,
        String id,
        PacketHandler<T> packetHandler,
        ExecutorService executor)
    {
        super(capacity, id, packetHandler, executor);
    }

    @Override
    protected void packetAdded(T pkt)
    {
        queueStatistics.add(System.currentTimeMillis());
    }

    @Override
    protected void packetRemoved(T pkt)
    {
        queueStatistics.remove(-1, System.currentTimeMillis());
    }

    @Override
    protected void packetDropped(T pkt)
    {
        queueStatistics.drop(-1, System.currentTimeMillis());
    }

    @Override
    public JSONObject getDebugState()
    {
        JSONObject debugState = super.getDebugState();
        debugState.put("statistics",queueStatistics.getStats());

        return debugState;
    }
}
