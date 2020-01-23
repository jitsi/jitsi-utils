/*
 * Copyright @ 2018 - Present, 8x8 Inc
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

/** Format a sequence of ints as a ranged string, e.g. "1, 3-8, 9-10"
 * Elements are separated using [separator]; non-singleton ranges are connected with [rangeSeparator].
 * The given [prefix] and [postfix] are used if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [rangeLimit], in which case only the first [rangeLimit]
 * ranges (or singleton elements) will be appended, followed by the [truncated] string (which defaults to "...").
 * */
/* TODO: it'd be nice to support this for any integer type but I don't know
    if there's a good way to represent this.  Unfortunately interface [Number]
    doesn't have [operator fun plus], so we can't use it to test
    element == previous + 1.
 */
fun Iterator<Int>.joinToRangedString(
    separator: CharSequence = ", ",
    rangeSeparator: CharSequence = "-",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    rangeLimit: Int = -1,
    truncated: CharSequence = "..."
): String = with(StringBuilder()) {
    append(prefix)
    if (!hasNext()) {
        return append(postfix).toString()
    }
    if (rangeLimit == 0) {
        return append(truncated).append(postfix).toString()
    }

    var rangeCount = 0
    fun canAddElement(): Boolean {
        return when {
            rangeLimit < 0 -> true
            rangeCount < rangeLimit -> true
            else -> false
        }
    }

    var inRange = false
    var previous = next()
    append(previous)

    loop@ while (hasNext()) {
        val element = next()
        when (element) {
            previous + 1 -> inRange = true
            else -> {
                if (inRange) {
                    append(rangeSeparator).append(previous)
                    inRange = false
                }
                append(separator)
                rangeCount++
                if (canAddElement()) {
                    append(element)
                } else {
                    append(truncated)
                    break@loop
                }
            }
        }
        previous = element
    }

    if (inRange) {
        append(rangeSeparator).append(previous)
    }
    append(postfix)

    toString()
}

fun Iterable<Int>.joinToRangedString(
    separator: CharSequence = ", ",
    rangeSeparator: CharSequence = "-",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    rangeLimit: Int = -1,
    truncated: CharSequence = "..."
): String = this.iterator().joinToRangedString(separator = separator, rangeSeparator = rangeSeparator, prefix = prefix, postfix = postfix, rangeLimit = rangeLimit, truncated = truncated)

fun Sequence<Int>.joinToRangedString(
    separator: CharSequence = ", ",
    rangeSeparator: CharSequence = "-",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    rangeLimit: Int = -1,
    truncated: CharSequence = "..."
): String = this.iterator().joinToRangedString(separator = separator, rangeSeparator = rangeSeparator, prefix = prefix, postfix = postfix, rangeLimit = rangeLimit, truncated = truncated)

fun Array<Int>.joinToRangedString(
    separator: CharSequence = ", ",
    rangeSeparator: CharSequence = "-",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    rangeLimit: Int = -1,
    truncated: CharSequence = "..."
): String = this.iterator().joinToRangedString(separator = separator, rangeSeparator = rangeSeparator, prefix = prefix, postfix = postfix, rangeLimit = rangeLimit, truncated = truncated)
