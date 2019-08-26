package org.jitsi.utils.logging2;

import org.junit.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.logging.*;

import static junit.framework.TestCase.*;

public class BaseLoggerTest
{
    private static Function<String, Logger> oldLoggerFactoryFunction;
    private FakeLogger fakeLogger = new FakeLogger("fake");

    @BeforeClass
    public static void beforeClass()
    {
        oldLoggerFactoryFunction = BaseLogger.loggerFactory;
    }

    @AfterClass
    public static void afterClass()
    {
        BaseLogger.loggerFactory = oldLoggerFactoryFunction;
    }

    @Before
    public void beforeTest()
    {
        BaseLogger.loggerFactory = (name) -> fakeLogger;
    }

    @After
    public void afterTest()
    {
        fakeLogger.reset();
    }

    @Test
    public void testBasicLogging()
    {
        BaseLogger logger = new BaseLogger("test");
        logger.setLevelAll();

        logger.info("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.INFO, fakeLogger.last().level);
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.debug("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.FINE, fakeLogger.last().level);
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.warn("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.WARNING, fakeLogger.last().level);
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.error("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.SEVERE, fakeLogger.last().level);
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.trace("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.FINER, fakeLogger.last().level);
        assertEquals("hello, world!", fakeLogger.lastMsg());
    }

    @Test
    public void testMaxLevel()
    {
        BaseLogger logger = new BaseLogger("test", Level.WARNING);

        logger.info("hello, world!");
        assertEquals(0, fakeLogger.logLines.size());
    }

    @Test
    public void testChildLoggerInheritsMaxLevel()
    {
        BaseLogger logger = new BaseLogger("test", Level.WARNING);

        LoggerInterface childLogger = logger.createChildLogger("child", Collections.emptyMap());

        childLogger.info("hello, world!");
        assertEquals(0, fakeLogger.logLines.size());
    }

    @Test
    public void testLoggingWithContext()
    {
        Map<String, String> ctxData = new HashMap<>();
        ctxData.put("keyOne", "value1");
        ctxData.put("keyTwo", "value2");
        LogContext ctx = new LogContext(ctxData);

        BaseLogger logger = new BaseLogger("test", ctx);

        logger.info("hello, world!");

        String[] contextTokens = fakeLogger.getLastLineContextTokens();
        assertTrue(LogContextTest.containsData(contextTokens, "keyOne=value1"));
        assertTrue(LogContextTest.containsData(contextTokens, "keyTwo=value2"));
    }

    @Test
    public void testChildLoggerInheritsContext()
    {
        Map<String, String> ctxData = new HashMap<>();
        ctxData.put("keyOne", "value1");
        ctxData.put("keyTwo", "value2");
        LogContext ctx = new LogContext(ctxData);

        BaseLogger logger = new BaseLogger("test", ctx);

        Map<String, String> subCtxData = new HashMap<>();
        subCtxData.put("keyThree", "value3");
        LoggerInterface childLogger = logger.createChildLogger("child", subCtxData);

        childLogger.info("hello, world!");

        String[] contextTokens = fakeLogger.getLastLineContextTokens();
        assertTrue(LogContextTest.containsData(contextTokens, "keyOne=value1"));
        assertTrue(LogContextTest.containsData(contextTokens, "keyTwo=value2"));
        assertTrue(LogContextTest.containsData(contextTokens, "keyThree=value3"));
    }

    /**
     * Although the max level is shared between parent and child, the current log
     * level should not be
     */
    @Test
    public void testParentChildLevelsIndependent()
    {
        FakeLogger parentLoggerDelegate = new FakeLogger("parent");
        BaseLogger.loggerFactory = (name) -> parentLoggerDelegate;
        BaseLogger logger = new BaseLogger("parent");
        logger.setLevelError();

        FakeLogger childLoggerDelegate = new FakeLogger("child");
        BaseLogger.loggerFactory = (name) -> childLoggerDelegate;
        LoggerInterface childLogger = logger.createChildLogger("child", Collections.emptyMap());
        childLogger.setLevelDebug();

        logger.info("hello, world!");
        assertEquals(0, parentLoggerDelegate.logLines.size());

        childLogger.debug("hello, world!");
        assertEquals(1, childLoggerDelegate.logLines.size());
    }

}

class FakeLogger extends Logger {
    final List<LogLine> logLines = new ArrayList<>();

    public FakeLogger(String name)
    {
        super(name, null);
    }

    @Override
    public void log(LogRecord logRecord)
    {
        logLines.add(new LogLine(logRecord.getLevel(), logRecord.getMessage()));
    }

    String[] getLastLineContextTokens()
    {
        return LogContextTest.getTokens(lastMsg());
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