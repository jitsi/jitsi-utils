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

package org.jitsi.utils.collections;

import org.junit.*;

import static org.jitsi.utils.collections.JMap.entry;
import static org.junit.Assert.*;

public class JMapTest
{

    @Test
    public void testMapOf()
    {
        java.util.Map<String, String> data = JMap.ofEntries(
            entry("one", "1"),
            entry("two", "2"),
            entry("three", "3")
        );

        assertEquals(3, data.size());
        assertEquals("1", data.get("one"));
        assertEquals("2", data.get("two"));
        assertEquals("3", data.get("three"));
    }

    @Test
    public void test1ArgMapOf()
    {
        java.util.Map data = JMap.of("one", "1");
        assertEquals(1, data.size());
        assertEquals("1", data.get("one"));
    }
    @Test
    public void test2ArgsMapOf()
    {
        java.util.Map data = JMap.of("one", "1", "two", "2");
        assertEquals(2, data.size());
        assertEquals("1", data.get("one"));
        assertEquals("2", data.get("two"));
    }

    @Test
    public void test3ArgsMapOf()
    {
        java.util.Map data = JMap.of("one", "1", "two", "2", "three", "3");
        assertEquals(3, data.size());
        assertEquals("1", data.get("one"));
        assertEquals("2", data.get("two"));
        assertEquals("3", data.get("three"));
    }
}