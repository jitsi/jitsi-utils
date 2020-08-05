package org.jitsi.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

public class TimeUtilsTest
{
    @Test
    public void testTimestampFormats()
    {
        assertEquals("0", TimeUtils.formatTimeAsFullMillis(0, 0));

        assertEquals("0.000001", TimeUtils.formatTimeAsFullMillis(0, 1));
        assertEquals("0.001", TimeUtils.formatTimeAsFullMillis(0, 1_000));
        assertEquals("1", TimeUtils.formatTimeAsFullMillis(0, 1_000_000));
        assertEquals("1.001", TimeUtils.formatTimeAsFullMillis(0, 1_001_000));
        
        assertEquals("1000", TimeUtils.formatTimeAsFullMillis(1, 0));
        assertEquals("1000.000001", TimeUtils.formatTimeAsFullMillis(1, 1));
        assertEquals("1000.001", TimeUtils.formatTimeAsFullMillis(1, 1_000));
        assertEquals("1001", TimeUtils.formatTimeAsFullMillis(1, 1_000_000));
        
        assertEquals("1570545261000", TimeUtils.formatTimeAsFullMillis(1570545261,0));
        assertEquals("1570545261234", TimeUtils.formatTimeAsFullMillis(1570545261,234000000));
        assertEquals("1570545261234.770582", TimeUtils.formatTimeAsFullMillis(1570545261,234770582));
        
        assertEquals("-1000", TimeUtils.formatTimeAsFullMillis(-1, 0));
        assertEquals("-999.999999", TimeUtils.formatTimeAsFullMillis(-1, 1));
        assertEquals("-0.000001", TimeUtils.formatTimeAsFullMillis(-1, 999_999_999));
        assertEquals("-1999.999999", TimeUtils.formatTimeAsFullMillis(-2, 1));
        
        assertEquals("9223372036854775807000", TimeUtils.formatTimeAsFullMillis(Long.MAX_VALUE, 0));
        assertEquals("9223372036854775807999.999999", TimeUtils.formatTimeAsFullMillis(Long.MAX_VALUE, 999_999_999));
        
        assertEquals("-9223372036854775808000", TimeUtils.formatTimeAsFullMillis(Long.MIN_VALUE, 0));
        assertEquals("-9223372036854775807999.999999", TimeUtils.formatTimeAsFullMillis(Long.MIN_VALUE, 1));
    }
}
