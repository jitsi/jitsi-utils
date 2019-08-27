package org.jitsi.utils.logging2;

import java.util.logging.*;

/**
 * {@link ContextLogRecord} extends {@link LogRecord} and adds
 * a 'context' String.  The reason it's done this way and the context
 * is not added to the log message itself is so that, in the log formatter,
 * we can place this context elsewhere (notably before the class and
 * method names) in the final log message.
 */
public class ContextLogRecord extends LogRecord
{
    protected final String context;
    public ContextLogRecord(Level level, String msg, String context)
    {
        super(level, msg);
        this.context = context;
    }

    public String getContext()
    {
        return context;
    }
}
