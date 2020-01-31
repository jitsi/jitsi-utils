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

package org.jitsi.utils

import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec

class OutcomeTest : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

//    val outcome = Outcome()

    init {
        given("an outcome") {
            val outcome = Outcome()
            then("the outcome should not yet be known") {
                outcome.isKnown shouldBe false
                outcome.hasFailed shouldBe false
                outcome.hasSucceeded shouldBe false
            }
            and("subscribers are added") {
                var successCalled = false
                var failCalled = false
                outcome.onSuccess { successCalled = true }
                outcome.onFailure { failCalled = true }
                then("they should not be notified yet") {
                    successCalled shouldBe false
                    failCalled shouldBe false
                }
                and("the outcome is successful") {
                    outcome.succeeded()
                    then("only the success handler should be called") {
                        successCalled shouldBe true
                        failCalled shouldBe false
                    }
                    and("a success handler is added after") {
                        var newSuccessCalled = false
                        outcome.onSuccess { newSuccessCalled = true }
                        then("it should be called right away") {
                            newSuccessCalled shouldBe true
                        }
                    }
                }
                and("the outcome is unsuccessful") {
                    outcome.failed()
                    then("only the fail handler should be called") {
                        successCalled shouldBe false
                        failCalled shouldBe true
                    }
                    and("a failure handler is added after") {
                        var newFailcalled = false
                        outcome.onFailure { newFailcalled = true }
                        then("it should be called right away") {
                            newFailcalled shouldBe true
                        }
                    }
                }
            }
        }
    }
}
