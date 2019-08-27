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
