package org.jitsi.utils.logging;

import javafx.util.*;
import org.junit.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertEquals;

public class BaseLoggerTest
{
    private static Function<String, Logger> oldLoggerFactoryFunction;
    private static FakeLogger fakeLogger = new FakeLogger("fake");

    @BeforeClass
    public static void beforeClass()
    {
        oldLoggerFactoryFunction = BaseLogger.loggerFactory;
        BaseLogger.loggerFactory = (name) -> fakeLogger;
    }

    @AfterClass
    public static void afterClass()
    {
        BaseLogger.loggerFactory = oldLoggerFactoryFunction;
        fakeLogger.reset();
    }

    @Test
    public void testBasicLogging()
    {
        BaseLogger logger = new BaseLogger("test");

        logger.info("hello, world!");

        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.INFO, fakeLogger.last().level);
        assertEquals("hello, world!", fakeLogger.lastMsg());
    }
}

class FakeLogger extends Logger {
    final List<LogLine> logLines = new ArrayList<>();

    public FakeLogger(String name)
    {
        super(name, null);
    }

    @Override
    public void log(Level level, String msg)
    {
        logLines.add(new LogLine(level, msg));
    }

    LogLine last()
    {
        return logLines.get(logLines.size() - 1);
    }

    String lastMsg()
    {
        return logLines.get(logLines.size() - 1).msg;
    }

    public void reset()
    {
        logLines.clear();
    }

    class LogLine
    {
        final Level level;
        final String msg;

        LogLine(Level level, String msg)
        {
            this.level = level;
            this.msg = msg;
        }
    }
}