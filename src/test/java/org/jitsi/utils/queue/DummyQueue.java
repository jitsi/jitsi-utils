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

import java.util.concurrent.*;

/**
 * Concrete dummy implementation of PacketQueue. Intended for launching tests
 * against base implementation of PacketQueue.
 *
 * @author Yura Yaroshevich
 */
class DummyQueue
    extends PacketQueue<DummyQueue.Dummy>
{
    DummyQueue(
        int capacity,
        PacketHandler<Dummy> packetHandler,
        ExecutorService executor)
    {
        super(capacity, null, "DummyQueue", packetHandler,
            executor);
    }

    static class Dummy {
        int id;
    }
}
