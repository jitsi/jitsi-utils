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
package org.jitsi.utils.queue

import java.util.concurrent.ExecutorService

/**
 * Concrete dummy implementation of PacketQueue. Intended for launching tests
 * against base implementation of PacketQueue.
 *
 * @author Yura Yaroshevich
 */
internal open class DummyQueue(
    capacity: Int,
    packetHandler: PacketHandler<Dummy>?,
    executor: ExecutorService?,
    idSuffix: String = "DummyQueue"
) : PacketQueue<DummyQueue.Dummy>(capacity, false, "DummyQueue$idSuffix", packetHandler, executor) {

    override fun getBuffer(pkt: Dummy): ByteArray? {
        return null
    }

    override fun getOffset(pkt: Dummy): Int {
        return 0
    }

    override fun getLength(pkt: Dummy): Int {
        return 0
    }

    override fun getContext(pkt: Dummy): Any? {
        return null
    }

    override fun createPacket(
        buf: ByteArray,
        off: Int,
        len: Int,
        context: Any
    ): Dummy {
        return Dummy()
    }

    internal class Dummy {
        var id = 0
    }
}
