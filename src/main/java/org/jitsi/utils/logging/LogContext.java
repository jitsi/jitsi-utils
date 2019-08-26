package org.jitsi.utils.logging;

import java.util.*;
import java.util.stream.*;

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
        String data = context.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" "));
        sb.append(data);
        sb.append("]");
        return sb.toString();
    }

    public LogContext createSubContext(Map<String, String> childContextData)
    {
        context.forEach((key, value) -> childContextData.merge(key, value, (v1, v2) -> v1));
        return new LogContext(childContextData);
    }

    @Override
    public String toString()
    {
        return formattedContext;
    }

    public static LogContext EMPTY = new LogContext(Collections.emptyMap());
}
