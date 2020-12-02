/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.utils.logging2;

import static org.junit.jupiter.api.Assertions.*;

import edu.umd.cs.findbugs.annotations.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public class LoggerImplTest
{
    private static Function<String, java.util.logging.Logger> oldLoggerFactoryFunction;
    private final FakeLogger fakeLogger = new FakeLogger("fake");

    @BeforeAll
    public static void beforeClass()
    {
        oldLoggerFactoryFunction = LoggerImpl.loggerFactory;
    }

    @AfterAll
    public static void afterClass()
    {
        LoggerImpl.loggerFactory = oldLoggerFactoryFunction;
    }

    @BeforeEach
    public void beforeTest()
    {
        LoggerImpl.loggerFactory = (name) -> fakeLogger;
    }

    @AfterEach
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
        assertEquals(Level.INFO, fakeLogger.last().getLevel());
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.debug("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.FINE, fakeLogger.last().getLevel());
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.warn("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.WARNING, fakeLogger.last().getLevel());
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.error("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.SEVERE, fakeLogger.last().getLevel());
        assertEquals("hello, world!", fakeLogger.lastMsg());

        fakeLogger.reset();
        logger.trace("hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
        assertEquals(Level.FINER, fakeLogger.last().getLevel());
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

    @Test
    public void testMsgSupplier()
    {
        LoggerImpl logger = new LoggerImpl("test");
        logger.setLevel(Level.INFO);

        logger.debug(() -> { throw new RuntimeException("This shouldn't run"); });

        logger.info(() -> "hello, world!");
        assertEquals(1, fakeLogger.logLines.size());
    }
}

class FakeLogger extends java.util.logging.Logger
{
    final List<LogRecord> logLines = new ArrayList<>();

    FakeLogger(String name)
    {
        super(name, null);
    }

    @Override
    public void log(LogRecord logRecord)
    {
        logLines.add(logRecord);
    }

    String[] getLastLineContextTokens()
    {
        return LogContextTest.getTokens(lastContext());
    }

    LogRecord last()
    {
        return logLines.get(logLines.size() - 1);
    }

    String lastMsg()
    {
        return last().getMessage();
    }

    private String lastContext()
    {
        return ((ContextLogRecord)last()).getContext();
    }

    void reset()
    {
        logLines.clear();
    }
}
