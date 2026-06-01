/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.utils

import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class OrderedJsonObjectTest : ShouldSpec() {

    init {
        context("an ordered json object") {
            val ojo = OrderedJsonObject()
            ojo.put("one", 1)
            ojo.put("two", 2)
            ojo.put("three", 3)
            ojo.put("four", 4)
            ojo.put("five", 5)
            ojo.put("six", 6)

            should("print items in the order they were added") {
                ojo.toString() shouldBe """{"one":1,"two":2,"three":3,"four":4,"five":5,"six":6}"""
            }

            should("iterate in the order they were added") {
                ojo.fieldNames().asSequence().toList() shouldContainExactly
                    listOf("one", "two", "three", "four", "five", "six")
                ojo.elements().asSequence().map { it.intValue() }.toList() shouldContainExactly
                    listOf(1, 2, 3, 4, 5, 6)
            }

            should("print recursive objects properly") {
                val subOjo = OrderedJsonObject()
                subOjo.put("Washington", 1)
                subOjo.put("Adams", 2)
                subOjo.put("Jefferson", 3)
                subOjo.put("Madison", 4)

                ojo.set<ObjectNode>("presidents", subOjo)

                val expected = """{"one":1,"two":2,"three":3,"four":4,"five":5,"six":6,""" +
                    """"presidents":{"Washington":1,"Adams":2,"Jefferson":3,"Madison":4}}"""
                ojo.toString() shouldBe expected
            }
        }
    }
}
