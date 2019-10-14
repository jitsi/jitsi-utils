package org.jitsi.utils;

import org.junit.*;

import static org.junit.Assert.*;

public class TimeUtilsTest
{
    @Test
    public void testTimestampFormats()
    {
        assertEquals(TimeUtils.formatTimeAsFullMillis(0, 0), "0");

        assertEquals(TimeUtils.formatTimeAsFullMillis(0, 1), "0.000001");
        assertEquals(TimeUtils.formatTimeAsFullMillis(0, 1_000), "0.001");
        assertEquals(TimeUtils.formatTimeAsFullMillis(0, 1_000_000), "1");
        assertEquals(TimeUtils.formatTimeAsFullMillis(0, 1_001_000), "1.001");

        assertEquals(TimeUtils.formatTimeAsFullMillis(1, 0), "1000");
        assertEquals(TimeUtils.formatTimeAsFullMillis(1, 1), "1000.000001");
        assertEquals(TimeUtils.formatTimeAsFullMillis(1, 1_000), "1000.001");
        assertEquals(TimeUtils.formatTimeAsFullMillis(1, 1_000_000), "1001");

        assertEquals(TimeUtils.formatTimeAsFullMillis(1570545261,0), "1570545261000");
        assertEquals(TimeUtils.formatTimeAsFullMillis(1570545261,234000000), "1570545261234");
        assertEquals(TimeUtils.formatTimeAsFullMillis(1570545261,234770582), "1570545261234.770582");

        assertEquals(TimeUtils.formatTimeAsFullMillis(-1, 0), "-1000");
        assertEquals(TimeUtils.formatTimeAsFullMillis(-1, 1), "-999.999999");
        assertEquals(TimeUtils.formatTimeAsFullMillis(-1, 999_999_999), "-0.000001");
        assertEquals(TimeUtils.formatTimeAsFullMillis(-2, 1), "-1999.999999");

        assertEquals(TimeUtils.formatTimeAsFullMillis(Long.MAX_VALUE, 0), "9223372036854775807000");
        assertEquals(TimeUtils.formatTimeAsFullMillis(Long.MAX_VALUE, 999_999_999), "9223372036854775807999.999999");

        assertEquals(TimeUtils.formatTimeAsFullMillis(Long.MIN_VALUE, 0), "-9223372036854775808000");
        assertEquals(TimeUtils.formatTimeAsFullMillis(Long.MIN_VALUE, 1), "-9223372036854775807999.999999");
    }
}
