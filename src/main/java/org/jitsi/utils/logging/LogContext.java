package org.jitsi.utils.logging;

import java.util.*;

public class LogContext
{
    protected final Map<String, String> context;
    protected final String formattedContext;

    public LogContext(Map<String, String> context)
    {
        this.context = context;
        this.formattedContext = formatContext(context);
    }

    protected String formatContext(Map<String, String> context)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        context.forEach((key, value) -> {
            sb.append(key).append("=").append(value);
        });
        sb.append("]");
        return sb.toString();
    }

    public LogContext createSubContext(Map<String, String> subContext)
    {
        //TODO(brian): order of args for merge correct?
        context.forEach((key, value) -> subContext.merge(key, value, (v1, v2) -> v2));
        return new LogContext(subContext);
    }

    @Override
    public String toString()
    {
        return formattedContext;
    }

    public static LogContext EMPTY = new LogContext(Collections.emptyMap());
}
