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

package org.jitsi.utils.config.strategy.read_frequency;

import java.util.function.*;

/**
 * Queries the given supplier for the configuration
 * property's value once (on creation) and returns that
 * cached value each time {@link ReadEveryTimeStrategy#getValue()}
 * is called
 *
 * @param <T> the type of the configuration property's value
 */
public class ReadOnceStrategy<T> implements ReadFrequencyStrategy<T>
{
    protected final T value;

    public ReadOnceStrategy(Supplier<T> supplier)
    {
        value = supplier.get();
    }

    @Override
    public T getValue()
    {
        return value;
    }
}
