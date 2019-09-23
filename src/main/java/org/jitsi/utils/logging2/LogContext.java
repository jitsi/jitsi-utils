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

import com.google.common.collect.*;
import org.jetbrains.annotations.*;
import org.jitsi.utils.collections.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * Maintains a map of key-value pairs (both Strings) which holds
 * arbitrary context to use as a prefix for log messages.  Sub-contexts
 * can be created and will inherit any context values from their parent
 * context.
 */
// Supress warnings about access since this is a library and usages will
// occur outside this repo
@SuppressWarnings("WeakerAccess")
public class LogContext
{
    public static LogContext EMPTY = new LogContext(Collections.emptyMap());
    public static String CONTEXT_START_TOKEN = "[";
    public static String CONTEXT_END_TOKEN = "]";

    /**
     * All context belonging to 'ancestors' of  this
     * LogContext
     */
    protected ImmutableMap<String, String> parentContext;
    /**
     * The context held by this specific LogContext.
     */
    protected ImmutableMap<String, String> context;

    /**
     * The formatted String representing the total context
     * (the combination of the parent context and this
     * context)
     */
    protected String formattedContext;

    /**
     * Child LogContext's of this LogContext (which will be notified
     * anytime this context changes)
     */
    private final List<LogContext> childContexts = new ArrayList<>();

    public LogContext(Map<String, String> context)
    {
        this(context, ImmutableMap.of());
    }

    protected LogContext(Map<String, String> context, ImmutableMap<String, String> parentContext)
    {
        this.context = ImmutableMap.copyOf(context);
        this.parentContext = parentContext;
        updateFormattedContext();
    }

    protected synchronized void updateFormattedContext()
    {
        this.formattedContext = formatContext(combineMaps(parentContext, context));
        updateChildren();
    }

    public synchronized LogContext createSubContext(Map<String, String> childContextData)
    {
        // The parent context to this child is the combination of this LogContext's context
        // and its parent's context
        ImmutableMap<String, String> combinedParentContext = combineMaps(parentContext, context);
        LogContext child = new LogContext(childContextData, combinedParentContext);
        childContexts.add(child);
        return child;
    }

    public void addContext(String key, String value)
    {
        addContext(JMap.of(key, value));
    }

    public synchronized void addContext(Map<String, String> addedContext)
    {
        this.context = combineMaps(context, addedContext);
        updateFormattedContext();
    }

    /**
     * Notify children of changes in this context
     */
    protected synchronized void updateChildren()
    {
        ImmutableMap<String, String> combined = combineMaps(parentContext, context);
        childContexts.forEach((child) -> child.parentContextUpdated(combined));
    }

    /**
     * Handle a change in the parent's context
     * @param parentContext the parent's new  context
     */
    protected synchronized void parentContextUpdated(ImmutableMap<String, String> parentContext)
    {
        this.parentContext = parentContext;
        updateFormattedContext();
    }

    @Override
    public String toString()
    {
        return formattedContext;
    }

    /**
     * Combine all the given maps into a new map.  Note that the order in which the maps
     * are passed matters: keys in later maps will override duplicates in earlier maps.
     * @param maps the maps to combine, in order of lowest to highest priority for keys
     * @return an *unmodifiable* combined map containing all the data of the given maps
     */
    @SafeVarargs
    @NotNull
    protected static ImmutableMap<String, String> combineMaps(@NotNull Map<String, String>... maps)
    {
        Map<String, String> combinedMap = new HashMap<>();
        for (Map<String, String> map : maps)
        {
            combinedMap.putAll(map);
        }
        return ImmutableMap.copyOf(combinedMap);
    }

    @SafeVarargs
    protected static String formatContext(Map<String, String>... contexts)
    {
        StringBuilder contextString = new StringBuilder();
        for (Map<String, String> context : contexts)
        {
            String data = context.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(" "));
            contextString.append(data);
        }
        if (contextString.length() > 0)
        {
            return CONTEXT_START_TOKEN +
                    contextString +
                    CONTEXT_END_TOKEN;
        }
        else
        {
            return "";
        }
    }
}
