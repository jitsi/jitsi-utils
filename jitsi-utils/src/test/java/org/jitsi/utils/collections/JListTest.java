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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;
import org.junit.jupiter.api.*;

public class JListTest
{
    @Test
    public void addElements()
    {
        List<Integer> nums = JList.of(1, 2, 3, 4);
        assertEquals(4, nums.size());
        assertEquals((int) nums.get(0), 1);
        assertEquals((int) nums.get(1), 2);
        assertEquals((int) nums.get(2), 3);
        assertEquals((int) nums.get(3), 4);
    }

    @Test
    public void noAddAllowed()
    {
        List<Integer> nums = JList.of(1, 2, 3, 4);
        assertThrows(UnsupportedOperationException.class, () -> nums.add(5));
    }

    @Test
    public void noRemoveAllowed()
    {
        List<Integer> nums = JList.of(1, 2, 3, 4);
        assertThrows(UnsupportedOperationException.class, () -> nums.remove(0));
    }

    @Test
    public void noNullsAllowed()
    {
        assertThrows(NullPointerException.class, () -> JList.of(1, 2, 3, null));
    }
}
