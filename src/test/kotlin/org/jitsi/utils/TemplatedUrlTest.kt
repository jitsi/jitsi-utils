/*
 * Copyright @ 2025 - present 8x8, Inc.
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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.net.URI
import java.net.URISyntaxException

class TemplatedUrlTest : ShouldSpec({
    context("TemplatedUrl") {
        should("resolve a simple template") {
            val template = "https://example.com/{{path}}"
            val templatedUrl = TemplatedUrl(template)
            templatedUrl.set("path", "test")

            templatedUrl.resolve() shouldBe URI("https://example.com/test")
        }

        should("resolve a template with multiple keys") {
            val template = "wss://{{host}}/{{path}}?param={{param}}"
            val templatedUrl = TemplatedUrl(template)
            templatedUrl.set("host", "example.com")
            templatedUrl.set("path", "api/resource")
            templatedUrl.set("param", "value")

            templatedUrl.resolve() shouldBe URI("wss://example.com/api/resource?param=value")
        }

        should("resolve a template with multiple keys when they are set()") {
            val template = "wss://{{host}}/{{path}}?param={{param}}"
            val templatedUrl = TemplatedUrl(template)
            templatedUrl.set("host", "example.com")
            templatedUrl.set("path", "api/resource")
            templatedUrl.set("param", "value")

            templatedUrl.resolve(
                mapOf("host" to "example2.com", "path" to "api/resource2", "param" to "value2")
            ) shouldBe URI("wss://example2.com/api/resource2?param=value2")
        }
        should("resolve a template with keys set in the constructor, with set() and in resolve()") {
            val template = "wss://{{host}}/{{path}}?param={{param}}"
            val templatedUrl = TemplatedUrl(template, mapOf("host" to "example.com"))
            templatedUrl.set("path", "api/resource")
            templatedUrl.set("param", "value")

            templatedUrl.resolve(
                mapOf("param" to "value2")
            ) shouldBe URI("wss://example.com/api/resource?param=value2")
        }

        should("resolve with a new key-value pair without saving it") {
            val template = "https://example.com/{{path}}?param={{param}}"
            val templatedUrl = TemplatedUrl(template)
            templatedUrl.set("path", "api")
            templatedUrl.set("param", "permanent")

            // Resolve with temporary param value
            templatedUrl.resolve("param", "temp") shouldBe URI("https://example.com/api?param=temp")

            templatedUrl.resolve() shouldBe URI("https://example.com/api?param=permanent")
        }

        should("throw an exception when not all requiredKeys are set") {
            val template = "https://example.com/{{path}}?param={{param}}"
            val templatedUrl = TemplatedUrl(template, requiredKeys = setOf("param", "required"))
            templatedUrl.set("path", "api")

            shouldThrow<IllegalArgumentException> {
                templatedUrl.resolve()
            }
            shouldThrow<IllegalArgumentException> {
                // "required" is still not set
                templatedUrl.resolve("param", "value")
            }
        }

        should("fail to resolve when some requiredKeys are not set") {
            val template = "https://{{host}}/{{path}}"
            val templatedUrl = TemplatedUrl(template, requiredKeys = setOf("host", "path"))

            templatedUrl.set("host", "example.com")

            shouldThrow<IllegalArgumentException> {
                templatedUrl.resolve()
            }
        }

        should("throw an exception when the URI is invalid") {
            val templatedUrl = TemplatedUrl("https://example.com/{{}}")

            shouldThrow<URISyntaxException> {
                templatedUrl.resolve()
            }
        }
        should("throw an exception when the values lead to an invalid URI ") {
            val templatedUrl = TemplatedUrl("https://example.com/{{path}}")

            shouldThrow<URISyntaxException> {
                templatedUrl.resolve("path", "}")
            }
        }

        should("handle keys that appear multiple times") {
            val template = "https://{{host}}/{{path}}/{{path}}"
            val templatedUrl = TemplatedUrl(template)
            templatedUrl.set("host", "example.com")
            templatedUrl.set("path", "resource")

            templatedUrl.resolve() shouldBe URI("https://example.com/resource/resource")
        }
        should("not modify the map passed as a constructor parameter") {
            val m = mutableMapOf("key1" to "value1", "key2" to "value2")
            val templatedUrl = TemplatedUrl("https://example.com/{{key1}}/{{key2}}", m)
            templatedUrl.set("key1", "newValue1")
            templatedUrl.set("key2", "newValue2")
            m shouldBe mapOf("key1" to "value1", "key2" to "value2")
        }
    }
})
