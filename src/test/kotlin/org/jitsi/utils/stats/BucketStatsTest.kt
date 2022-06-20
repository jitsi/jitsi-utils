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
package org.jitsi.utils.stats

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jitsi.utils.OrderedJsonObject
import java.lang.IllegalArgumentException

@SuppressFBWarnings(
    value = ["NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"],
    justification = "False positives"
)
class BucketStatsTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        context("adding stats") {
            val bucketStats = BucketStats(listOf(0, 1, 2, 3, 5, 200, 999, Long.MAX_VALUE), "_delay_ms", "_ms")

            should("use the correct thresholds") {
                bucketStats.snapshot.buckets.buckets.map { it.first } shouldBe setOf(
                    Pair(0L, 1L),
                    Pair(1L, 2L),
                    Pair(2L, 3L),
                    Pair(3L, 5L),
                    Pair(5L, 200L),
                    Pair(200L, 999L),
                    Pair(999L, Long.MAX_VALUE)
                )
            }

            should("calculate the average correctly") {
                repeat(100) { bucketStats.addValue(1) }
                repeat(100) { bucketStats.addValue(5) }
                bucketStats.snapshot.average shouldBe 3.0
                bucketStats.toJson()["average_delay_ms"] shouldBe 3.0
            }

            should("calculate the max correctly") {
                repeat(100) { bucketStats.addValue(1) }
                repeat(100) { bucketStats.addValue(5) }
                bucketStats.addValue(100)
                bucketStats.snapshot.maxValue shouldBe 100
                bucketStats.toJson()["max_delay_ms"] shouldBe 100
            }

            context("export the buckets correctly to json") {
                repeat(5) { bucketStats.addValue(0) }
                repeat(100) { bucketStats.addValue(1) }
                repeat(100) { bucketStats.addValue(3) }
                repeat(100) { bucketStats.addValue(4) }
                bucketStats.addValue(150)
                bucketStats.addValue(1500)

                context("In separate buckets (default)") {
                    val bucketsJson = bucketStats.toJson()["buckets"]
                    bucketsJson.shouldBeInstanceOf<OrderedJsonObject>()

                    bucketsJson["0_to_1_ms"] shouldBe 5
                    bucketsJson["1_to_2_ms"] shouldBe 100
                    bucketsJson["2_to_3_ms"] shouldBe 0
                    bucketsJson["3_to_5_ms"] shouldBe 200
                    bucketsJson["5_to_200_ms"] shouldBe 1
                    bucketsJson["200_to_999_ms"] shouldBe 0
                    bucketsJson["999_to_max_ms"] shouldBe 1
                }
                context("Cumulative from the left") {
                    val bucketsJson = bucketStats.toJson(format = BucketStats.Format.CumulativeLeft)["buckets"]
                    bucketsJson.shouldBeInstanceOf<OrderedJsonObject>()

                    bucketsJson["0_to_1_ms"] shouldBe 5
                    bucketsJson["0_to_2_ms"] shouldBe 105
                    bucketsJson["0_to_3_ms"] shouldBe 105
                    bucketsJson["0_to_5_ms"] shouldBe 305
                    bucketsJson["0_to_200_ms"] shouldBe 306
                    bucketsJson["0_to_999_ms"] shouldBe 306
                }
                context("Cumulative from the right") {
                    val bucketsJson = bucketStats.toJson(format = BucketStats.Format.CumulativeRight)["buckets"]
                    bucketsJson.shouldBeInstanceOf<OrderedJsonObject>()

                    bucketsJson["1_to_max_ms"] shouldBe 302
                    bucketsJson["2_to_max_ms"] shouldBe 202
                    bucketsJson["3_to_max_ms"] shouldBe 202
                    bucketsJson["5_to_max_ms"] shouldBe 2
                    bucketsJson["200_to_max_ms"] shouldBe 1
                    bucketsJson["999_to_max_ms"] shouldBe 1
                }
            }

            should("calculate p99 and p999 correctly") {
                bucketStats.snapshot.buckets.p99bound shouldBe -1
                bucketStats.snapshot.buckets.p999bound shouldBe -1

                repeat(200) { bucketStats.addValue(1) }
                bucketStats.snapshot.buckets.p99bound shouldBe 2
                bucketStats.snapshot.buckets.p999bound shouldBe -1

                repeat(800) { bucketStats.addValue(1) }
                bucketStats.snapshot.buckets.p999bound shouldBe 2

                bucketStats.addValue(4)
                bucketStats.addValue(4)
                bucketStats.snapshot.buckets.p99bound shouldBe 2
                bucketStats.snapshot.buckets.p999bound shouldBe 5

                repeat(2000) { bucketStats.addValue(1) }
                bucketStats.snapshot.buckets.p999bound shouldBe 2
            }
        }

        context("initializing with invalid thresholds should throw") {
            shouldThrow<IllegalArgumentException> {
                BucketStats(listOf(2, 10, 5), "_delay_ms", " ms")
            }
        }
    }
}
