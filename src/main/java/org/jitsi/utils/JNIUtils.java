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
import org.jitsi.utils.logging.*;

import java.io.*;
import java.util.regex.*;

/**
 * Implements Java Native Interface (JNI)-related facilities such as loading a
 * JNI library from a jar.
 *
 * @author Lyubomir Marinov
 */
public final class JNIUtils
{
    /** Set this property to get JNIUtils to always delete the libraries when the JVM exits,
     * rather than immediately.  Useful for debugging.
     */
    private static final String DELETE_ON_EXIT_PROPERTY =
            "org.jitsi.utils.JniUtils.AlwaysDeleteOnExit";

    private static final Logger logger = Logger.getLogger(JNIUtils.class);

    /**
     * Loads a native library, preferring {@code java.library.path} and
     * falling back to a copy bundled as a resource of {@code classLoader}.
     *
     * @param libname the name of the library to load.
     * @param classLoader the ClassLoader used to locate the bundled library.
     */
    public static void loadLibrary(String libname, ClassLoader classLoader)
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
        }
        catch (UnsatisfiedLinkError ulerr)
        {
            // Attempt to extract the library from the resources and load it that
            // way.
            libname = System.mapLibraryName(libname);

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
                System.load(embedded.getAbsolutePath());
            }
            finally
            {
                // Native.isUnpacked(String) is (package) internal.
                if (embedded.getName().startsWith("jna"))
                {
                    // Native.deleteLibrary(String) is (package) internal.
                    if (System.getProperty(DELETE_ON_EXIT_PROPERTY) != null || !embedded.delete())
                        embedded.deleteOnExit();
                }
            }
        }
    }

    /**
     * Loads a native library, preferring {@code java.library.path} and
     * falling back to a copy bundled as a resource of {@code clazz}'s
     * ClassLoader.
     *
     * <p>The {@code clazz} parameter is retained because this API used to
     * support loading the library on behalf of the caller's OSGi bundle
     * ClassLoader. Libraries are now bound to jitsi-utils' ClassLoader; in a
     * true OSGi environment, where the caller's bundle has its own
     * ClassLoader, the caller's native methods will not be bound to the
     * library loaded here.
     *
     * @param libname the name of the library to load.
     * @param clazz the class whose ClassLoader is used to locate the bundled
     * library.
     */
    public static <T> void loadLibrary(String libname, Class<T> clazz)
    {
        loadLibrary(libname, clazz.getClassLoader());
    }

    /**
     * Prevents the initialization of new <tt>JNIUtils</tt> instances.
     */
    private JNIUtils()
    {
    }
}
