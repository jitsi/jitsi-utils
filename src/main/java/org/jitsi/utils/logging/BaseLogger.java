package org.jitsi.utils.logging;

import java.util.logging.*;

/**
 * Implements {@link LoggerInterface} by delegating to
 * a {@link java.util.logging.Logger}.
 */
public class BaseLogger implements LoggerInterface
{
    private final java.util.logging.Logger loggerDelegate;

    public BaseLogger(java.util.logging.Logger logger)
    {
        this.loggerDelegate = logger;
    }

    private boolean isLoggable(Level level)
    {
        return loggerDelegate.isLoggable(level);
    }

    private void log(Level level, Object msg, Throwable thrown)
    {
        loggerDelegate.log(level, msg != null ? msg.toString() : "null", thrown);
    }

    private void log(Level level, Object msg)
    {
        loggerDelegate.log(level, msg != null ? msg.toString() : "null");
    }

    @Override
    public void setLevel(Level level)
    {
        Handler[] handlers = loggerDelegate.getHandlers();
        for (Handler handler : handlers)
            handler.setLevel(level);

        loggerDelegate.setLevel(level);
    }

    @Override
    public Level getLevel()
    {
        // OpenJDK's Logger implementation initializes its effective level value
        // with Level.INFO.intValue(), but DOESN'T initialize the Level object.
        // So, if it hasn't been explicitly set, assume INFO.
        Level level = loggerDelegate.getLevel();
        return level != null ? level : Level.INFO;
    }

    @Override
    public void setLevelAll()
    {
        setLevel(Level.ALL);
    }

    @Override
    public void setLevelDebug()
    {
        setLevel(Level.FINE);
    }

    @Override
    public void setLevelError()
    {
        setLevel(Level.SEVERE);
    }

    @Override
    public void setLevelInfo()
    {
        setLevel(Level.INFO);
    }

    @Override
    public void setLevelOff()
    {
        setLevel(Level.OFF);
    }

    @Override
    public void setLevelTrace()
    {
        setLevel(Level.FINER);
    }

    @Override
    public void setLevelWarn()
    {
        setLevel(Level.WARNING);
    }

    @Override
    public boolean isTraceEnabled()
    {
        return isLoggable(Level.FINER);
    }

    @Override
    public void trace(Object msg)
    {
        log(Level.FINER, msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return isLoggable(Level.FINE);
    }

    @Override
    public void debug(Object msg)
    {
        log(Level.FINE, msg);
    }

    @Override
    public boolean isInfoEnabled()
    {
        return isLoggable(Level.INFO);
    }

    @Override
    public void info(Object msg)
    {
        log(Level.INFO, msg);
    }

    @Override
    public boolean isWarnEnabled()
    {
        return isLoggable(Level.WARNING);
    }

    @Override
    public void warn(Object msg)
    {
        log(Level.WARNING, msg);
    }

    @Override
    public void error(Object msg)
    {
        log(Level.SEVERE, msg);
    }
}
