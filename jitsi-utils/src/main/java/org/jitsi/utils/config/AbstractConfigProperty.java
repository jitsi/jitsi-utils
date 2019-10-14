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

import org.jitsi.utils.config.strategy.prop_not_found.*;
import org.jitsi.utils.config.strategy.read_frequency.*;

import java.util.*;
import java.util.function.*;

/**
 * A base helper class for modeling a configuration property.  Contains the
 * code for iterating over multiple {@link Supplier}s for the first
 * one which successfully returns a result; if none contain the property,
 * it defers to the given {@link PropNotFoundStrategy}
 *
 * @param <T> the type of the configuration property's value
 */
public abstract class AbstractConfigProperty<T> implements ConfigProperty<T>
{
    protected final List<Supplier<T>> configValueSuppliers;
    protected final ReadFrequencyStrategy<T> readFrequencyStrategy;
    protected final PropNotFoundStrategy<T> propNotFoundStrategy;

    public AbstractConfigProperty(PropertyConfig<T> builder)
    {
        builder.validate();
        this.configValueSuppliers = builder.propValueSuppliers;
        // Note: it's important we set the not-found strategy first, as some
        // read strategies may read the value upon creation
        this.propNotFoundStrategy = builder.propNotFoundStrategy;
        this.readFrequencyStrategy = builder.readFrequencyStrategyCreator.apply(this::doGet);
    }

    /**
     * Iterate through each of the retrievers, returning a value the first time
     * one is successfully retrieved.  If none are found, return the default value.
     * @return the retrieved value for this configuration property
     */
    private T doGet()
    {
        for (Supplier<T> configValueSupplier : configValueSuppliers)
        {
            try
            {
                return configValueSupplier.get();
            }
            catch (ConfigPropertyNotFoundException ignored) { }
        }
        return propNotFoundStrategy.handleNotFound(this.getClass().getName());
    }

    @Override
    public T get()
    {
        return readFrequencyStrategy.getValue();
    }
}
