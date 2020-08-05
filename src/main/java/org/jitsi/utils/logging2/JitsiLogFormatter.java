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

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public class JitsiLogFormatter extends Formatter
{
    /**
     * Program name logging property name
     */
    private static final String PROGRAM_NAME_PROPERTY = ".programname";

    /**
     * Disable timestamp logging property name.
     */
    private static final String DISABLE_TIMESTAMP_PROPERTY
            = ".disableTimestamp";

    /**
     * Line separator used by current platform
     */
    private static String lineSeparator = System.getProperty("line.separator");

    /**
     * Two digit <tt>DecimalFormat</tt> instance, used to format datetime
     */
    private static DecimalFormat twoDigFmt = new DecimalFormat("00");

    /**
     * Three digit <tt>DecimalFormat</tt> instance, used to format datetime
     */
    private static DecimalFormat threeDigFmt = new DecimalFormat("000");

    /**
     * The application name used to generate this log
     */
    private static String programName;

    /**
     * Whether logger will add date to the logs, enabled by default.
     */
    private static boolean timestampDisabled = false;

    /**
     * The default constructor for <tt>JitsiLogFormatter</tt> which loads
     * program name property from logging.properties file, if it exists
     */
    public JitsiLogFormatter()
    {
        loadConfigProperties();
    }

    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public synchronized String format(LogRecord record)
    {
        StringBuffer sb = new StringBuffer();

        if (programName != null)
        {
            // Program name
            sb.append(programName);

            sb.append(' ');
        }

        if(!timestampDisabled)
        {
            //current time
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minutes = cal.get(Calendar.MINUTE);
            int seconds = cal.get(Calendar.SECOND);
            int millis = cal.get(Calendar.MILLISECOND);

            sb.append(year).append('-');
            sb.append(twoDigFmt.format(month)).append('-');
            sb.append(twoDigFmt.format(day)).append(' ');
            sb.append(twoDigFmt.format(hour)).append(':');
            sb.append(twoDigFmt.format(minutes)).append(':');
            sb.append(twoDigFmt.format(seconds)).append('.');
            sb.append(threeDigFmt.format(millis)).append(' ');
        }

        //log level
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");

        // Thread ID
        sb.append("[").append(record.getThreadID()).append("] ");

        if (record instanceof ContextLogRecord)
        {
            String context = ((ContextLogRecord)record).getContext();
            if (!context.isEmpty())
            {
                sb.append(context).append(" ");
            }
        }

        //caller method
        int lineNumber = inferCaller(record);

        sb.append(record.getSourceClassName());

        if (record.getSourceMethodName() != null)
        {
            sb.append(".");
            sb.append(record.getSourceMethodName());

            //include the line number if we have it.
            if(lineNumber != -1)
            {
                sb.append("#").append(lineNumber);
            }
        }
        sb.append(": ");
        sb.append(record.getMessage());
        sb.append(lineSeparator);
        if (record.getThrown() != null)
        {
            try
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            }
            catch (RuntimeException ex)
            {
            }
        }
        return sb.toString();
    }

    /**
     * Try to extract the name of the class and method that called the current
     * log statement.
     *
     * @param record the logrecord where class and method name should be stored.
     *
     * @return the line number that the call was made from in the caller.
     */
    private int inferCaller(LogRecord record)
    {
        // Get the stack trace.
        StackTraceElement[] stack = (new Throwable()).getStackTrace();

        //the line number that the caller made the call from
        int lineNumber = -1;

        // First, search back to a method in the SIP Communicator Logger class.
        int ix = 0;
        while (ix < stack.length)
        {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (cname.equals("org.jitsi.utils.logging2.LoggerImpl") ||
                cname.equals("org.jitsi.utils.logging.LoggerImpl"))
            {
                break;
            }
            ix++;
        }
        // Now search for the first frame
        // before the SIP Communicator Logger class.
        while (ix < stack.length)
        {
            StackTraceElement frame = stack[ix];
            lineNumber=stack[ix].getLineNumber();
            String cname = frame.getClassName();
            String shortName = cname.substring(cname.lastIndexOf(".") + 1);
            if (!cname.contains("org.jitsi.utils.logging"))
            {
                // We've found the relevant frame.
                record.setSourceClassName(shortName);
                record.setSourceMethodName(frame.getMethodName());
                break;
            }
            ix++;
        }

        return lineNumber;
    }

    /**
     * Loads all config properties.
     */
    private void loadConfigProperties()
    {
        loadProgramNameProperty();
        loadTimestampDisabledProperty();
    }

    /**
     * Checks and loads timestamp disabled property if any.
     */
    private static void loadTimestampDisabledProperty()
    {
        LogManager manager = LogManager.getLogManager();
        String cname = JitsiLogFormatter.class.getName();
        timestampDisabled = Boolean.parseBoolean(
                manager.getProperty(cname + DISABLE_TIMESTAMP_PROPERTY));
    }

    /**
     * Load the programname property to be used in logs to identify Jitsi-based
     * application which produced the logs
     */
    private static void loadProgramNameProperty()
    {
        LogManager manager = LogManager.getLogManager();
        String cname = JitsiLogFormatter.class.getName();
        programName = manager.getProperty(cname + PROGRAM_NAME_PROPERTY);
    }
}
