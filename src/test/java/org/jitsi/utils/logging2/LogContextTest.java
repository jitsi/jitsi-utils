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

import org.jitsi.utils.collections.*;
import org.junit.*;

import static org.jitsi.utils.collections.JMap.*;
import static org.junit.Assert.*;

public class LogContextTest
{
    static boolean containsData(String[] dataTokens, String expected)
    {
        for (String dataToken : dataTokens)
        {
            if (dataToken.equalsIgnoreCase(expected))
            {
                return true;
            }
        }
        return false;
    }

    static String[] getTokens(String formattedCtxString)
    {
        int contextBlockStartIndex = formattedCtxString.indexOf(LogContext.CONTEXT_START_TOKEN);
        int contextBlockStopIndex = formattedCtxString.indexOf(LogContext.CONTEXT_END_TOKEN, contextBlockStartIndex);
        return formattedCtxString.substring(contextBlockStartIndex + 1, contextBlockStopIndex).split(" ");
    }

    @Test
    public void logContextFormat()
    {
        LogContext ctx = new LogContext(
                JMap.ofEntries(
                    entry("confId", "111"),
                    entry("epId", "123")
                )
        );

        String formatted = ctx.toString();

        assertTrue(formatted.startsWith("["));
        assertTrue(formatted.endsWith("]"));
        String[] data = getTokens(formatted);
        assertTrue(containsData(data, "epId=123"));
        assertTrue(containsData(data, "confId=111"));
    }

    @Test
    public void creatingSubContext()
    {
        LogContext ctx = new LogContext(JMap.of("confId", "111"));
        LogContext subCtx = ctx.createSubContext(JMap.of("epId", "123"));

        String[] data = getTokens(subCtx.toString());
        assertTrue(containsData(data, "epId=123"));
        assertTrue(containsData(data, "confId=111"));
    }

    @Test
    public void creatingSubContextWithConflicts()
    {
        LogContext ctx = new LogContext(
                JMap.ofEntries(
                        entry("confId", "111"),
                        entry("epId", "123")
                )
        );

        LogContext subCtx = ctx.createSubContext(JMap.of("epId", "456"));
        String[] data = getTokens(subCtx.toString());
        assertTrue(containsData(data, "epId=456"));
        assertTrue(containsData(data, "confId=111"));
    }

    @Test
    public void addingContextAfterCreation()
    {
        LogContext ctx = new LogContext(
                JMap.ofEntries(
                        entry("confId", "111"),
                        entry("epId", "123")
                )
        );

        ctx.addContext("newKey", "newValue");
        String[] data = getTokens(ctx.toString());
        assertTrue(containsData(data, "confId=111"));
        assertTrue(containsData(data, "epId=123"));
        assertTrue(containsData(data, "newKey=newValue"));
    }

    @Test
    public void addContextAfterCreationReflectedInChildren()
    {
        LogContext ctx = new LogContext(JMap.of("confId", "111"));
        LogContext subCtx = ctx.createSubContext(JMap.of("epId", "123"));
        LogContext subSubCtx = subCtx.createSubContext(JMap.of("ssrc", "98765"));

        ctx.addContext("newKey", "newValue");

        String[] subCtxData = getTokens(subCtx.toString());
        assertTrue(containsData(subCtxData, "confId=111"));
        assertTrue(containsData(subCtxData, "newKey=newValue"));
        assertTrue(containsData(subCtxData, "epId=123"));

        String[] subSubCtxData = getTokens(subSubCtx.toString());
        assertTrue(containsData(subSubCtxData, "confId=111"));
        assertTrue(containsData(subSubCtxData, "newKey=newValue"));
        assertTrue(containsData(subSubCtxData, "epId=123"));
        assertTrue(containsData(subSubCtxData, "ssrc=98765"));
    }

}