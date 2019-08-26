package org.jitsi.utils.logging2;

import org.junit.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import static junit.framework.TestCase.*;

public class LoggerImplTest
{
    private static Function<String, java.util.logging.Logger> oldLoggerFactoryFunction;
    private FakeLogger fakeLogger = new FakeLogger("fake");

    @BeforeClass
    public static void beforeClass()
    {
        oldLoggerFactoryFunction = LoggerImpl.loggerFactory;
    }

    @AfterClass
    public static void afterClass()
    {
        LoggerImpl.loggerFactory = oldLoggerFactoryFunction;
    }

    @Before
    public void beforeTest()
    {
        LoggerImpl.loggerFactory = (name) -> fakeLogger;
    }

    @After
    public void afterTest()
    {
        fakeLogger.reset();
    }

    @Test
    public void testBasicLogging()
    {
        LoggerImpl logger = new LoggerImpl("test");
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
        LoggerImpl logger = new LoggerImpl("test", Level.WARNING);

        logger.info("hello, world!");
        assertEquals(0, fakeLogger.logLines.size());
    }

    @Test
    public void testChildLoggerInheritsMaxLevel()
    {
        LoggerImpl logger = new LoggerImpl("test", Level.WARNING);

        Logger childLogger = logger.createChildLogger("child", Collections.emptyMap());

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

        LoggerImpl logger = new LoggerImpl("test", ctx);

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

        LoggerImpl logger = new LoggerImpl("test", ctx);

        Map<String, String> subCtxData = new HashMap<>();
        subCtxData.put("keyThree", "value3");
        Logger childLogger = logger.createChildLogger("child", subCtxData);

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
        LoggerImpl.loggerFactory = (name) -> parentLoggerDelegate;
        LoggerImpl logger = new LoggerImpl("parent");
        logger.setLevelError();

        FakeLogger childLoggerDelegate = new FakeLogger("child");
        LoggerImpl.loggerFactory = (name) -> childLoggerDelegate;
        Logger childLogger = logger.createChildLogger("child", Collections.emptyMap());
        childLogger.setLevelDebug();

        logger.info("hello, world!");
        assertEquals(0, parentLoggerDelegate.logLines.size());

        childLogger.debug("hello, world!");
        assertEquals(1, childLoggerDelegate.logLines.size());
    }

}

class FakeLogger extends java.util.logging.Logger
{
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