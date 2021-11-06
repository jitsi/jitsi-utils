/*
 * Copyright @ 2015 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.utils;

import com.sun.jna.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jitsi.utils.logging.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.regex.*;

/**
 * Implements Java Native Interface (JNI)-related facilities such as loading a
 * JNI library from a jar.
 *
 * @author Lyubomir Marinov
 */
public final class JNIUtils
{
    /**
     * The regular expression pattern which matches the file extension
     * &quot;dylib&quot; that is commonly used on Mac OS X for dynamic
     * libraries/shared objects.
     */
    private static final Pattern DYLIB_PATTERN = Pattern.compile("\\.dylib$");

    private static final Logger logger = Logger.getLogger(JNIUtils.class);

    public static void loadLibrary(String libname, ClassLoader classLoader)
    {
        loadLibrary(libname, null, classLoader);
    }

    public static <T> void loadLibrary(String libname, Class<T> clazz)
    {
        loadLibrary(libname, clazz, clazz.getClassLoader());
    }

    private static <T> void loadLibrary(String libname, Class<T> clazz,
        ClassLoader classLoader)
    {
        try
        {
            try
            {
                // Always prefer libraries from java.library.path over those unpacked from the jar.
                // This allows the end user to manually unpack native libraries and store them
                // in java.library.path to later load via System.loadLibrary.
                // This allows end-users to preserve native libraries on disk,
                // which is necessary for debuggers like gdb to load symbols.
                System.loadLibrary(libname);
                logger.info("Loading library " + libname + " from java.library.path rather than bundled version");
                return;
            }
            catch (UnsatisfiedLinkError e)
            {
                if (clazz == null)
                {
                    throw e;
                }
            }
            loadNativeInClassloader(libname, clazz, false);
        }
        catch (UnsatisfiedLinkError ulerr)
        {
            // Attempt to extract the library from the resources and load it that
            // way.
            libname = System.mapLibraryName(libname);
            if (Platform.isMac())
                libname = DYLIB_PATTERN.matcher(libname).replaceFirst(".jnilib");

            File embedded;

            try
            {
                embedded
                    = Native.extractFromResourcePath(
                            "/" + Platform.RESOURCE_PREFIX + "/" + libname,
                            classLoader);
            }
            catch (IOException ioex)
            {
                throw ulerr;
            }
            try
            {
                if (clazz != null)
                {
                    loadNativeInClassloader(
                        embedded.getAbsolutePath(), clazz, true);
                }
                else
                {
                    System.load(embedded.getAbsolutePath());
                }
            }
            finally
            {
                // Native.isUnpacked(String) is (package) internal.
                if (embedded.getName().startsWith("jna"))
                {
                    // Native.deleteLibrary(String) is (package) internal.
                    if (!embedded.delete())
                        embedded.deleteOnExit();
                }
            }
        }
    }

    /**
     * Hack so that the native library is loaded into the ClassLoader
     * that called this method, and not into the ClassLoader where
     * this code resides. This is necessary for true OSGi environments.
     *
     * @param lib The library to load, name or path.
     * @param clazz The class where to load it.
     * @param isAbsolute Whether the library is name or path.
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private static <T> void loadNativeInClassloader(
        String lib, Class<T> clazz, boolean isAbsolute)
    {
        try
        {
            Method loadLibrary0 = Runtime
                .getRuntime()
                .getClass()
                .getDeclaredMethod(
                    isAbsolute ? "load0" : "loadLibrary0",
                    Class.class,
                    String.class);
            loadLibrary0.setAccessible(true);
            loadLibrary0.invoke(Runtime.getRuntime(), clazz, lib);
        }
        catch (Exception e)
        {
            System.loadLibrary(lib);
        }
    }

    /**
     * Prevents the initialization of new <tt>JNIUtils</tt> instances.
     */
    private JNIUtils()
    {
    }
}
