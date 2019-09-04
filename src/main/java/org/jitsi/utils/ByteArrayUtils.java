/*
 * Copyright @ 2019 - Present 8x8, Inc
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

/**
 * Contains basic methods for reading/writing values to/from a {@code byte[]}s
 * and {@link ByteArrayBuffer}s.
 */
public class ByteArrayUtils
{
    /**
     * Read a unsigned 16-bit value from a byte array buffer at a specified
     * offset as an int.
     *
     * @param bab the buffer from which to read.
     * @param off start offset of the unsigned short
     * @return the int value of the unsigned short at offset
     */
    public static int readUint16AsInt(ByteArrayBuffer bab, int off)
    {
        return readUint16AsInt(bab.getBuffer(), off + bab.getOffset());
    }

    /**
     * Read a unsigned 16-bit value from a byte array at a specified offset as
     * an int.
     *
     * @param buf the buffer from which to read.
     * @param off start offset of the unsigned short
     * @return the int value of the unsigned short at offset
     */
    public static int readUint16AsInt(byte[] buf, int off)
    {
        int b1 = (0xFF & (buf[off + 0]));
        int b2 = (0xFF & (buf[off + 1]));
        int val = b1 << 8 | b2;
        return val;
    }

    /**
     * Read a 32-bit integer from a byte array buffer at a specified offset.
     *
     * @param bab the byte array from which to read.
     * @param off start offset in the buffer of the integer to be read.
     */
    public static int readInt(ByteArrayBuffer bab, int off)
    {
        return readInt(bab.getBuffer(), off + bab.getOffset());
    }

    /**
     * Read a 32-bit integer from a byte array at a specified offset.
     *
     * @param buf the byte array from which to read.
     * @param off start offset in the buffer of the integer to be read.
     */
    public static int readInt(byte[] buf, int off)
    {
        return
                ((buf[off++] & 0xFF) << 24)
                        | ((buf[off++] & 0xFF) << 16)
                        | ((buf[off++] & 0xFF) << 8)
                        | (buf[off] & 0xFF);
    }

    /**
     * Writes an {@code int} into a byte array buffer at a specified offset.
     *
     * @param bab the byte array to write to.
     * @param value the value to write.
     */
    public static void writeInt(ByteArrayBuffer bab, int off, int value)
    {
        writeInt(bab.getBuffer(), off + bab.getOffset(), value);
    }

    /**
     * Writes an {@code int} into a byte array at a specified offset.
     *
     * @param buf the byte array to write to.
     * @param value the value to write.
     */
    public static void writeInt(byte[] buf, int off, int value)
    {
        buf[off++] = (byte) ((value >> 24) & 0xFF);
        buf[off++] = (byte) ((value >> 16) & 0xFF);
        buf[off++] = (byte) ((value >> 8) & 0xFF);
        buf[off] = (byte) (value & 0xFF);
    }

    /**
     * Read a 16-bit signed integer from a byte array buffer at a specified
     * offset into a {@code short}.
     *
     * @param bab the byte array from which to read.
     * @param off start offset in the buffer of the integer to be read.
     */
    public static short readShort(ByteArrayBuffer bab, int off)
    {
        return readShort(bab.getBuffer(), off + bab.getOffset());
    }

    /**
     * Read a 16-bit signed integer from a byte array at a specified offset
     * into a {@code short}.
     *
     * @param buf the byte array from which to read.
     * @param off start offset in the buffer of the integer to be read.
     */
    public static short readShort(byte[] buf, int off)
    {
        return (short) (((buf[off++] & 0xFF) << 8) | (buf[off] & 0xFF));
    }

    /**
     * Writes a {@code short} into a byte array buffer at a specified offset.
     *
     * @param bab the byte array to write to.
     * @param value the value to write.
     */
    public static void writeShort(ByteArrayBuffer bab, int off, short value)
    {
        writeShort(bab.getBuffer(), off + bab.getOffset(), value);
    }

    /**
     * Writes a {@code short} into a byte array at a specified offset.
     *
     * @param buf the byte array to write to.
     * @param value the value to write.
     */
    public static void writeShort(byte[] buf, int off, short value)
    {
        buf[off++] = (byte) ((value >> 8) & 0xFF);
        buf[off] = (byte) (value & 0xFF);
    }
}
