/*
 * Copyright @ 2019 - present 8x8, Inc.
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

import java.util.concurrent.atomic.*;

/**
 * An {@link ErrorHandler} implementation which counts the number of
 * dropped packets and exceptions.
 *
 * @author Boris Grozev
 */
public class CountingErrorHandler implements ErrorHandler
{
    /**
     * The {@link Logger} used by the {@link PacketQueue} class and its
     * instances for logging output.
     */
    private static final Logger logger
            = Logger.getLogger(PacketQueue.class.getName());

    /**
     * The number of dropped packets.
     */
    private final AtomicLong numPacketsDropped = new AtomicLong();

    /**
     * The number of exceptions.
     */
    private final AtomicLong numExceptions = new AtomicLong();

    /**
     * {@inheritDoc}
     */
    @Override
    public void packetDropped()
    {
        numPacketsDropped.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void packetHandlingFailed(Throwable t)
    {
        logger.warn("Failed to handle packet: ", t);
        numExceptions.incrementAndGet();
    }

    /**
     * Get the number of dropped packets.
     * @return
     */
    public long getNumPacketsDropped()
    {
        return numPacketsDropped.get();
    }

    /**
     * Get the number of exceptions.
     * @return
     */
    public long getNumExceptions()
    {
        return numExceptions.get();
    }
}
