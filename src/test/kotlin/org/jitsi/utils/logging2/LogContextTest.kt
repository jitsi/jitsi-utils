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
package org.jitsi.utils.logging2

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

@SuppressFBWarnings(value = ["DM_GC"], justification = "We force GC on purpose to test weak references.")
class LogContextTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf

    init {
        context("logContextFormat") {
            val ctx = LogContext(mapOf("confId" to "111", "epId" to "123"))
            val formatted = ctx.formattedContext
            formatted.shouldStartWith("[")
            formatted.shouldEndWith("]")
            getTokens(formatted) shouldContain "epId=123"
            getTokens(formatted) shouldContain "confId=111"
        }

        context("creatingSubContext") {
            val ctx = LogContext(mapOf("confId" to "111"))
            val subCtx = ctx.createSubContext(mapOf("epId" to "123"))
            getTokens(subCtx.formattedContext) shouldContain "epId=123"
            getTokens(subCtx.formattedContext) shouldContain "confId=111"
        }

        context("creatingSubContextWithConflicts") {
            val ctx = LogContext(mapOf("confId" to "111", "epId" to "123"))
            val subCtx = ctx.createSubContext(mapOf("epId" to "456"))
            getTokens(subCtx.formattedContext) shouldContain "epId=456"
            getTokens(subCtx.formattedContext) shouldContain "confId=111"
        }

        context("addingContextAfterCreation") {
            val ctx = LogContext(mapOf("confId" to "111", "epId" to "123"))
            ctx.addContext("newKey", "newValue")
            getTokens(ctx.formattedContext) shouldContain "confId=111"
            getTokens(ctx.formattedContext) shouldContain "epId=123"
            getTokens(ctx.formattedContext) shouldContain "newKey=newValue"
        }

        context("addContextAfterCreationReflectedInChildren") {
            val ctx = LogContext(mapOf("confId" to "111"))
            val subCtx = ctx.createSubContext(mapOf("epId" to "123"))
            val subSubCtx = subCtx.createSubContext(mapOf("ssrc" to "98765"))
            ctx.addContext("newKey", "newValue")

            val subCtxData = getTokens(subCtx.formattedContext)
            subCtxData shouldContain "confId=111"
            subCtxData shouldContain "newKey=newValue"
            subCtxData shouldContain "epId=123"

            val subSubCtxData = getTokens(subSubCtx.toString())
            subSubCtxData shouldContain "confId=111"
            subSubCtxData shouldContain "newKey=newValue"
            subSubCtxData shouldContain "epId=123"
            subSubCtxData shouldContain "ssrc=98765"
        }

        context("testMultipleChildContexts") {
            val ctx = LogContext(mapOf("confId" to "111"))
            val subCtx1 = ctx.createSubContext(mapOf("epId" to "123"))
            val subCtx2 = ctx.createSubContext(mapOf("epId" to "456"))
            ctx.addContext("newKey", "newValue")

            val subCtx1Data = getTokens(subCtx1.formattedContext)
            subCtx1Data shouldContain "confId=111"
            subCtx1Data shouldContain "newKey=newValue"
            subCtx1Data shouldContain "epId=123"

            val subCtx2Data = getTokens(subCtx2.formattedContext)
            subCtx2Data shouldContain "confId=111"
            subCtx2Data shouldContain "newKey=newValue"
            subCtx2Data shouldContain "epId=456"
        }

        context("testChildContextDisappearing") {
            val ctx = LogContext(mapOf("confId" to "111"))
            // We use an array here rather than three separate variables to stop various code-analysis
            // tools from complaining about unused variables.
            val subCtxs = Array<LogContext?>(3) { i ->
                ctx.createSubContext(mapOf("epId" to "$i$i$i"))
            }
            ctx.addContext("newKey", "newValue")

            // We set subCtx[1] to null here and attempt to invoke GC in order for it to be null
            // for when we add more context to the parent logger.  Although we don't have a
            // guarantee that GC will always run, it did so reliably when I wrote these tests
            // to at least validate that LogContext behaves as expected.
            subCtxs[1] = null
            System.gc()

            ctx.addContext("anotherNewKey", "anotherNewValue")
            val subCtx1Data = getTokens(subCtxs[0]!!.formattedContext)
            subCtx1Data shouldContain "confId=111"
            subCtx1Data shouldContain "newKey=newValue"
            subCtx1Data shouldContain "epId=000"
            subCtx1Data shouldContain "anotherNewKey=anotherNewValue"

            val subCtx3Data = getTokens(subCtxs[2]!!.toString())
            subCtx3Data shouldContain "confId=111"
            subCtx3Data shouldContain "newKey=newValue"
            subCtx3Data shouldContain "epId=222"
            subCtx3Data shouldContain "anotherNewKey=anotherNewValue"
        }
    }

    companion object {
        @JvmStatic
        fun getTokens(formattedCtxString: String): Array<String> {
            val contextBlockStartIndex = formattedCtxString.indexOf(LogContext.CONTEXT_START_TOKEN)
            val contextBlockStopIndex =
                formattedCtxString.indexOf(LogContext.CONTEXT_END_TOKEN, contextBlockStartIndex)
            return formattedCtxString.substring(contextBlockStartIndex + 1, contextBlockStopIndex)
                .split(" ".toRegex())
                .toTypedArray()
        }
    }
}
