package org.jitsi.utils.logging2;

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
        // We don't merge directly into the given map, as it may have come
        // from Kotlin and be a read-only map
        Map<String, String> resultingContext = new HashMap<>(this.context);
        resultingContext.putAll(childContextData);
        return new LogContext(resultingContext);
    }

    @Override
    public String toString()
    {
        return formattedContext;
    }

}
