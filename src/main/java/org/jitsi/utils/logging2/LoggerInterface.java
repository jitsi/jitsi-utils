package org.jitsi.utils.logging2;

import org.jitsi.utils.logging.Logger;

import java.util.*;
import java.util.logging.*;

public interface LoggerInterface
{
    /**
     * Create a 'child' logger which derives from this one.  The child logger
     * will share the same log level setting as this one and its
     * {@link LogContext} (given here) will inherit all the context
     * from this logger.
     * @return
     */
    LoggerInterface createChildLogger(String name, Map<String, String> context);
    /**
     * Check if a message with a TRACE level would actually be logged by this
     * logger.
     * <p>
     * @return true if the TRACE level is currently being logged
     */
    boolean isTraceEnabled();

    /**
     * Log a TRACE message.
     * <p>
     * If the logger is currently enabled for the TRACE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    void trace(Object msg);

    /**
     * Check if a message with a DEBUG level would actually be logged by this
     * logger.
     * <p>
     * @return true if the DEBUG level is currently being logged
     */
    boolean isDebugEnabled();

    /**
     * Log a DEBUG message.
     * <p>
     * If the logger is currently enabled for the DEBUG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    void debug(Object msg);

    /**
     * Check if a message with an INFO level would actually be logged by this
     * logger.
     *
     * @return true if the INFO level is currently being logged
     */
    boolean isInfoEnabled();

    /**
     * Log a INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    void info(Object msg);

    /**
     * Check if a message with a WARN level would actually be logged by this
     * logger.
     * <p>
     * @return true if the WARN level is currently being logged
     */
    boolean isWarnEnabled();

    /**
     * Log a WARN message.
     * <p>
     * If the logger is currently enabled for the WARN message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    void warn(Object msg);

    /**
     * Log a ERROR message.
     * <p>
     * If the logger is currently enabled for the ERROR message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    void error(Object msg);

    /**
     * Set logging level for all handlers to ERROR
     */
    void setLevelError();

    /**
     * Set logging level for all handlers to WARNING
     */
    void setLevelWarn();

    /**
     * Set logging level for all handlers to INFO
     */
    void setLevelInfo();

    /**
     * Set logging level for all handlers to DEBUG
     */
    void setLevelDebug();

    /**
     * Set logging level for all handlers to TRACE
     */
    void setLevelTrace();

    /**
     * Set logging level for all handlers to ALL (allow all log messages)
     */
    void setLevelAll();

    /**
     * Set logging level for all handlers to OFF (allow no log messages)
     */
    void setLevelOff();

    /**
     * Set logging level for all handlers to <tt>level</tt>
     *
     * @param level the level to set for all logger handlers
     */
    void setLevel(Level level);

    /**
     * @return the {@link Level} configured for this {@link Logger}.
     */
    Level getLevel();
}
