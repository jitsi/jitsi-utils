/*
 * Copyright @ 2022 - present 8x8, Inc.
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
package org.jitsi.utils.dsi

import io.kotest.assertions.fail
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.ints.shouldBeBetween
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.jitsi.utils.concurrent.FakeScheduledExecutorService
import org.jitsi.utils.time.FakeClock
import java.time.Instant

/**
 * This is meant to help test [DominantSpeakerIdentification]. We don't have any specification to test against, other
 * than the code has been running unmodified for years and we're happy with the results.
 *
 * The audio levels trace (dsi-trace.csv) was captured from a conference. The speaker changes trace (dsi-changes.csv)
 * was created afterwards with the existing code at the time (adapted to work in a test environment).
 *
 * The DSI algorithm is very sensitive to timing (exactly when the decision maker was started, and how often it runs).
 * The decision maker runs periodically every 300 ms. Running it every 299 ms or 301 ms instead results in slightly
 * different decisions and a different number of speaker changes. So any changes to the algorithm will probably break
 * this test -- that's OK. The test is meant to help a human decide if the new behavior is as desired, not enforce the
 * previous behavior.
 */
class DominantSpeakerIdentificationTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf

    private val levelsTraceCsv = javaClass.getResource("/dsi-trace.csv") ?: fail("Can not read dsi-trace.csv")
    private val speakerChangesTraceCsv =
        javaClass.getResource("/dsi-changes.csv") ?: fail("Can not read dsi-changes.csv")

    private val levels = levelsTraceCsv.readText().split("\n").dropLast(1).map { AudioLevel(it) }.toList()
    private val speakerChangesTrace = speakerChangesTraceCsv.readText().split("\n").dropLast(1).map {
        SpeakerChange(it)
    }.toList()
    private val clock = FakeClock()
    private val fakeExecutor = FakeScheduledExecutorService(clock)
    private val speakerChanges = mutableListOf<SpeakerChange>()

    init {
        context("Single endpoint test") {
            val endpointId = levels[0].endpointId

            listOf(true, false).forEach { detectSilence ->
                context("${if (detectSilence) "With" else "Without"} silence detection") {
                    val dsi = createDsi(detectSilence)
                    // Only feed levels from one endpoint.
                    levels.filter { it.endpointId == endpointId }.forEach {
                        fakeExecutor.run()
                        clock.setTime(Instant.ofEpochMilli(it.timeMs))
                        dsi.levelChanged(it.endpointId, it.level)
                    }

                    if (detectSilence) {
                        // Should change from the endpoint to silence and back multiple times
                        speakerChanges.size shouldBeGreaterThan 10
                        speakerChanges.map { it.endpointId }.toSet() shouldBe setOf(null, endpointId)
                    } else {
                        speakerChanges.size shouldBe 1
                        speakerChanges[0].endpointId shouldBe endpointId
                    }
                }
            }
        }
        context("Compare with trace") {
            listOf(true, false).forEach { detectSilence ->
                context("${if (detectSilence) "With" else "Without"} silence detection") {
                    val dsi = createDsi(detectSilence)

                    levels.forEach {
                        fakeExecutor.run()
                        clock.setTime(Instant.ofEpochMilli(it.timeMs))
                        dsi.levelChanged(it.endpointId, it.level)
                    }

                    // Not much we can do, just sanity checks.
                    if (detectSilence) {
                        should("Have more changes than the trace") {
                            speakerChanges.size shouldBeGreaterThan speakerChangesTrace.size
                        }
                        should("Have a reasonable number of total changes") {
                            speakerChanges.size shouldBeLessThan 300
                        }
                        should("Have a reasonable number of silence intervals") {
                            speakerChanges.count { it.endpointId == null }.shouldBeBetween(10, 100)
                        }
                    } else {
                        // This is to make sure the previous behavior is conserved without silence detection.
                        should("Match the changes in the trace") {
                            speakerChanges shouldBe speakerChangesTrace
                        }
                    }
                }
            }
        }
    }

    private fun createDsi(detectSilence: Boolean) = DominantSpeakerIdentification<String>(
        clock,
        fakeExecutor,
        if (detectSilence) 3000 else -1
    ).apply {
        addActiveSpeakerChangedListener { speakerChanges.add(SpeakerChange(clock.instant().toEpochMilli(), it)) }
    }
}

private data class AudioLevel(
    val timeMs: Long,
    val endpointId: String,
    val level: Int
) {
    constructor(s: String) : this(
        s.split(",")[0].toLong(),
        s.split(",")[1],
        s.split(",")[2].toInt(),
    )
}
private data class SpeakerChange(
    val timeMs: Long,
    val endpointId: String?
) {
    constructor(s: String) : this(
        s.split(",")[0].toLong(),
        s.split(",")[1],
    )
}
