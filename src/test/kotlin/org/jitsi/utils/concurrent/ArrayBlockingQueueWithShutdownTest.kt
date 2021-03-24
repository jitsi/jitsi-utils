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

package org.jitsi.utils.concurrent

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class ArrayBlockingQueueWithShutdownTest : ShouldSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val queue = ArrayBlockingQueueWithShutdown<Int>(5)

    context("ArrayBlockingQueueWithShutdown") {
        context("adding elements") {
            queue.offer(1) shouldBe true
            queue.offer(2) shouldBe true
            queue.offer(3) shouldBe true
            queue.offer(4) shouldBe true
            should("work") {
                queue.size shouldBe 4
            }
            context("and then reading") {
                should("read the values") {
                    queue.poll() shouldBe 1
                    queue.poll() shouldBe 2
                    queue.poll() shouldBe 3
                    queue.poll() shouldBe 4
                    queue.poll() shouldBe null
                }
            }
            context("and then shutting down") {
                queue.shutdown()
                should("still leave all items in the queue") {
                    queue.size shouldBe 4
                }
            }
        }
        context("after being shut down") {
            queue.shutdown()
            should("not add further elements") {
                queue.offer(1) shouldBe false
            }
            should("show as shut down") {
                queue.isShutdown shouldBe true
            }
        }
    }
})
