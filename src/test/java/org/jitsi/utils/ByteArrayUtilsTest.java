/*
 * Copyright @ 2019 - present 8x8, Inc
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
package org.jitsi.utils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

import static org.jitsi.utils.ByteArrayUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class ByteArrayUtilsTest
{
    @Test
    public void testReadWriteShort()
    {
        ByteArrayBuffer bab = new BasicByteArrayBuffer(10);
        int offset = 3;

        writeShort(bab, offset, (short) 123);
        assertEquals(123, readShort(bab, offset),
            "Write/read a positive short");

        writeShort(bab, offset, (short) -123);
        assertEquals(-123, readShort(bab, offset),
            "Write/read a negative short");
    }

    @Test
    public void testReadWriteInt()
    {
        ByteArrayBuffer bab = new BasicByteArrayBuffer(10);
        int offset = 3;

        writeInt(bab, offset, 1234);
        assertEquals(1234, readInt(bab, offset),
            "Write/read a positive int");

        writeInt(bab, offset, -1234);
        assertEquals(-1234, readInt(bab, offset),
            "Write/read a negative int");
    }

    @Test
    public void testReadWriteUint16()
    {
        ByteArrayBuffer bab = new BasicByteArrayBuffer(10);
        int offset = 3;

        writeUint16(bab, offset, 1234);
        assertEquals(
            1234, readUint16(bab, offset), "Write/read a 16-bit int");
    }

    @Test
    public void testReadWriteUint24()
    {
        ByteArrayBuffer bab = new BasicByteArrayBuffer(10);
        int offset = 3;

        writeUint24(bab, offset, 1234);
        assertEquals(
            1234, readUint24(bab, offset), "Write/read a 24-bit int");
    }

    @Test
    public void testReadUint32()
    {
        ByteArrayBuffer bab = new BasicByteArrayBuffer(10);
        int offset = 3;

        long l = 0xff11_1111L;
        writeInt(bab, offset, (int) l);
        assertEquals(
            l, readUint32(bab, offset), "Read a 32-bit unsigned int (msb=1)");

        l = 0x0f11_1111L;
        writeInt(bab, offset, (int) l);
        assertEquals(
            l, readUint32(bab, offset), "Read a 32-bit unsigned int (msb=0)");
    }
}
