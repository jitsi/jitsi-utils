package org.jitsi.utils.logging;

import java.util.*;
import java.util.stream.*;

public class LogContext
{
    public static LogContext EMPTY = new LogContext(Collections.emptyMap());
    public static String CONTEXT_START_TOKEN = "[";
    public static String CONTEXT_END_TOKEN = "]";

    protected final Map<String, String> context;
    protected final String formattedContext;

    public LogContext(Map<String, String> context)
    {
        this.context = context;
        this.formattedContext = formatContext(context);
    }

    protected String formatContext(Map<String, String> context)
    {
        if (context.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(CONTEXT_START_TOKEN);
        String data = context.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" "));
        sb.append(data);
        sb.append(CONTEXT_END_TOKEN);
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

}
