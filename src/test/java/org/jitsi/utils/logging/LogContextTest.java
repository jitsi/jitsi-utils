package org.jitsi.utils.logging;

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