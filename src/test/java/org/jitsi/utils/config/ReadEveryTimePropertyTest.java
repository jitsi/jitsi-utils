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

package org.jitsi.utils.config;

import org.junit.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;

import static org.junit.Assert.*;

public class ReadEveryTimePropertyTest
{
    @Test
    public void readsEveryTime()
    {
        AtomicInteger numTimesGetterCalled = new AtomicInteger(0);
        AnswerProp answerProp = new AnswerProp(() -> {
            numTimesGetterCalled.incrementAndGet();
            return 42;
        });

        answerProp.get();
        answerProp.get();
        answerProp.get();
        answerProp.get();

        assertEquals(4, numTimesGetterCalled.get());
    }

    protected static class AnswerProp extends AbstractConfigProperty<Integer>
    {
        AnswerProp(Supplier<Integer> getter)
        {
            super(new PropertyConfig<Integer>()
                .suppliedBy(getter)
                .readEveryTime()
                .throwIfNotFound()
            );
        }
    }
}
