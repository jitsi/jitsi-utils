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

import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

public class LogContextTest
{
    public static boolean containsData(String[] dataTokens, String expected)
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

    public static String[] getTokens(String formattedCtxString)
    {
        int contextBlockStartIndex = formattedCtxString.indexOf(LogContext.CONTEXT_START_TOKEN);
        int contextBlockStopIndex = formattedCtxString.indexOf(LogContext.CONTEXT_END_TOKEN, contextBlockStartIndex);
        return formattedCtxString.substring(contextBlockStartIndex + 1, contextBlockStopIndex).split(" ");
    }
    @Test
    public void logContextFormatIsCorrect()
    {
        Map<String, String> ctxData = new HashMap();
        ctxData.put("confId", "111");
        ctxData.put("epId", "123");
        LogContext ctx = new LogContext(ctxData);

        String formatted = ctx.toString();

        assertTrue(formatted.startsWith("["));
        assertTrue(formatted.endsWith("]"));
        String[] data = getTokens(formatted);
        assertTrue(containsData(data, "epId=123"));
        assertTrue(containsData(data, "confId=111"));
    }

    @Test
    public void creatingSubContextWorksCorrectly()
    {
        Map<String, String> ctxData = new HashMap();
        ctxData.put("confId", "111");
        LogContext ctx = new LogContext(ctxData);

        Map<String, String> subCtxData = new HashMap();
        subCtxData.put("epId", "123");

        LogContext subCtx = ctx.createSubContext(subCtxData);
        String[] data = getTokens(subCtx.toString());
        assertTrue(containsData(data, "epId=123"));
        assertTrue(containsData(data, "confId=111"));
    }

    @Test
    public void creatingSubContextWithConflictsWorksCorrectly()
    {
        Map<String, String> ctxData = new HashMap();
        ctxData.put("confId", "111");
        ctxData.put("epId", "456");
        LogContext ctx = new LogContext(ctxData);

        Map<String, String> subCtxData = new HashMap();
        // This should override the 'epId' value in the parent context
        subCtxData.put("epId", "123");

        LogContext subCtx = ctx.createSubContext(subCtxData);
        String[] data = getTokens(subCtx.toString());
        assertTrue(containsData(data, "epId=123"));
        assertTrue(containsData(data, "confId=111"));
    }
}