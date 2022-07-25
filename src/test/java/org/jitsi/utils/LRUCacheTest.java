/*
 * Copyright @ 2019 - present 8x8, Inc
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
package org.jitsi.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

public class LRUCacheTest
{
    @Test
    public void testInsertionOrder()
    {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(2, "two again");
        cache.put(4, "four");

        assertEquals(2, cache.size());
        assertTrue(cache.containsKey(3));
        assertTrue(cache.containsKey(4));
        assertEquals(3, cache.eldest().getKey());
    }

    @Test
    public void testAccessOrder()
    {
        LRUCache<Integer, String> cache = new LRUCache<>(2, true);

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(2, "two again");
        cache.put(4, "four");

        assertEquals(2, cache.size());
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(4));
        assertEquals(2, cache.eldest().getKey());
    }

    @Test
    public void testSetAccessOrder()
    {
        Set<String> cache = LRUCache.lruSet(2, true);

        cache.add("a");
        cache.add("b");
        cache.add("c");
        cache.add("b");
        cache.add("d");

        assertEquals(2, cache.size());
        assertTrue(cache.contains("b"));
        assertTrue(cache.contains("d"));
    }

    @Test
    public void testSetInsertionOrder()
    {
        Set<String> cache = LRUCache.lruSet(2, false);

        cache.add("a");
        cache.add("b");
        cache.add("c");
        cache.add("b");
        cache.add("d");

        assertEquals(2, cache.size());
        assertTrue(cache.contains("c"));
        assertTrue(cache.contains("d"));
    }
}
