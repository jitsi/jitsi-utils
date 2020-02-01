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
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import java.lang.IllegalStateException

class OutcomeTest : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        given("an outcome") {
            val outcome = MutableOutcome()
            then("the outcome should not yet be known") {
                outcome.isKnown() shouldBe false
                outcome.hasFailed() shouldBe false
                outcome.hasSucceeded() shouldBe false
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
        given("two outcomes") {
            val oc1 = MutableOutcome()
            val oc2 = MutableOutcome()
            and("combining them") {
                val oc3 = oc1 + oc2
                then("the state should still be unknown") {
                    oc3.isKnown() shouldBe false
                    oc3.hasSucceeded() shouldBe false
                    oc3.hasFailed() shouldBe false
                }
                and("setting the state of one to failed") {
                    oc1.failed()
                    then("the aggregate should have failed") {
                        oc3.isKnown() shouldBe true
                        oc3.hasFailed() shouldBe true
                        oc3.hasSucceeded() shouldBe false
                    }
                }
                and("setting the state of one to success") {
                    oc1.succeeded()
                    then("the outcome should still be unknown") {
                        oc3.isKnown() shouldBe false
                        oc3.hasFailed() shouldBe false
                        oc3.hasSucceeded() shouldBe false
                    }
                    and("then setting the other to success") {
                        oc2.succeeded()
                        then("should have the aggregate outcome be success") {
                            oc3.isKnown() shouldBe true
                            oc3.hasFailed() shouldBe false
                            oc3.hasSucceeded() shouldBe true
                        }
                    }
                    and("then setting the other to failed") {
                        oc2.failed()
                        then("should have the aggregate outcome be failed") {
                            oc3.isKnown() shouldBe true
                            oc3.hasFailed() shouldBe true
                            oc3.hasSucceeded() shouldBe false
                        }
                    }
                }
                then("trying to add another should throw") {
                    shouldThrow<IllegalStateException> {
                        oc3 + MutableOutcome()
                    }
                }
            }
        }
    }
}
