package org.jitsi.utils;

import edu.umd.cs.findbugs.annotations.*;

@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "It is the nature of this class to expose 'buffer.'")
public class BasicByteArrayBuffer implements ByteArrayBuffer
{
    /**
     * Default to a non-zero offset to cover more test cases.
     */
    private static int DEFAULT_OFFSET = 5;

    private byte[] buffer;
    private int offset;
    private int length;

    BasicByteArrayBuffer(int length)
    {
        buffer = new byte[length + DEFAULT_OFFSET];
        this.length = length;
        offset = DEFAULT_OFFSET;
    }

    @Override
    public byte[] getBuffer()
    {
        return buffer;
    }

    @Override
    public int getOffset()
    {
        return offset;
    }

    @Override
    public int getLength()
    {
        return length;
    }

    @Override
    public void setLength(int len)
    {
        length = len;
    }

    @Override
    public void setOffset(int off)
    {
        offset = off;
    }

    @Override
    public boolean isInvalid()
    {
        return false;
    }

    @Override
    public void readRegionToBuff(int off, int len, byte[] outBuf)
    {
    }

    @Override
    public void grow(int howMuch)
    {
    }

    @Override
    public void append(byte[] data, int len)
    {
    }

    @Override
    public void shrink(int len)
    {
    }
}
