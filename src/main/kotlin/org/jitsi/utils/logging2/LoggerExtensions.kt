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

/**
 * This file defines additional functions for a lib which is used elsewhere.
 */
@file:Suppress("unused")

package org.jitsi.utils.logging2

import java.util.logging.Level
import kotlin.reflect.full.companionObject

/**
 * Create a logger with an optional [minLogLevel] and [logContext] using the
 * fully-qualified name of the *actual* class (i.e. the instance's class, not
 * the class that happens to be calling this method).
 *
 */
fun <T : Any> T.createLogger(minLogLevel: Level = Level.ALL, logContext: LogContext = LogContext()): Logger =
    LoggerImpl(getClassForLogging(this.javaClass).name, minLogLevel, logContext)

/**
 * Create a child logger from [parentLogger] with any optional [childContext]
 * using the fully-qualified name of the *actual* class (i.e. the instance's
 * class, not the class that happens to be calling this method).
 */
fun <T : Any> T.createChildLogger(
    parentLogger: Logger,
    childContext: Map<String, String> = emptyMap()
): Logger = parentLogger.createChildLogger(getClassForLogging(this.javaClass).name, childContext)

/**
 * Given a [Class], get the proper class to be used for the name of a logger
 * by stripping any companion object class identifier, if present.
 */
fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
    return (javaClass.enclosingClass?.takeIf {
        it.kotlin.companionObject?.java == javaClass
    } ?: javaClass)
}

/**
 * Note that, although the logger now supports taking a message supplier
 * argument, these methods are still more efficient as they're inline (and
 * therefore don't require constructing a lambda object)
 */
inline fun Logger.cinfo(msg: () -> String) {
    if (isInfoEnabled) {
        this.info(msg())
    }
}

inline fun Logger.cdebug(msg: () -> String) {
    if (isDebugEnabled) {
        this.debug(msg())
    }
}

inline fun Logger.cwarn(msg: () -> String) {
    if (isWarnEnabled) {
        this.warn(msg())
    }
}

inline fun Logger.cerror(msg: () -> String) {
    this.error(msg())
}
