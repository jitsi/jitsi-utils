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

package org.jitsi.utils.config;

import org.jitsi.utils.config.strategy.prop_not_found.*;
import org.jitsi.utils.config.strategy.read_frequency.*;

import java.util.*;
import java.util.function.*;

/**
 * Not to be confused with a 'configuration property', {@link PropertyConfig}
 * is a helper class for defining the various aspects of a specific configuration property
 * (e.g. how often it is read, what should be done when it isn't found); basically:
 * a configuration property's properties...
 *
 * @param <PropValueType> the type of the configuration property's value
 */
public class PropertyConfig<PropValueType>
{
    /**
     * The ReadFrequencyStrategy constructor requires a supplier which will give the property's
     * value, but we don't have that here (it comes from {@link AbstractConfigProperty}.
     * We'll pass this method to the {@link org.jitsi.utils.config.AbstractConfigProperty}
     * constructor so that it can pass the provider and instantiate the ReadFrequencyStrategy
     */
    protected Function<Supplier<PropValueType>, ReadFrequencyStrategy<PropValueType>> readFrequencyStrategyCreator = null;
    protected PropNotFoundStrategy<PropValueType> propNotFoundStrategy;
    protected List<Supplier<PropValueType>> propValueSuppliers = new ArrayList<>();

    public PropertyConfig<PropValueType> suppliedBy(Supplier<PropValueType> supplier)
    {
        propValueSuppliers.add(supplier);
        return this;
    }

    public PropertyConfig<PropValueType> readOnce()
    {
        readFrequencyStrategyCreator = ReadOnceStrategy::new;

        return this;
    }

    public PropertyConfig<PropValueType> readEveryTime()
    {
        readFrequencyStrategyCreator = ReadEveryTimeStrategy::new;

        return this;
    }

    public PropertyConfig<PropValueType> throwIfNotFound()
    {
        propNotFoundStrategy = new ThrowIfNotFoundStrategy<>();

        return this;
    }

    public PropertyConfig<PropValueType> returnNullIfNotFound()
    {
        propNotFoundStrategy = new ReturnNullIfNotFoundStrategy<>();

        return this;
    }

    public void validate()
    {
        if (readFrequencyStrategyCreator == null)
        {
            throw new InvalidPropertyConfigurationException("Invalid property config: 'read' strategy not set");
        }
        if (propNotFoundStrategy == null)
        {
            throw new InvalidPropertyConfigurationException("Invalid property config: 'prop not found' " +
                "strategy not set");
        }
    }

    public static class InvalidPropertyConfigurationException extends RuntimeException
    {
        public InvalidPropertyConfigurationException(String msg)
        {
            super(msg);
        }
    }
}
