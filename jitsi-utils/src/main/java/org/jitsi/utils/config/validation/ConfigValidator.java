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

package org.jitsi.utils.config.validation;

import org.jitsi.utils.config.*;
import org.jitsi.utils.logging2.*;
import org.reflections.*;
import org.reflections.scanners.*;
import org.reflections.util.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

/**
 * Runs various configuration validations based on all found
 * instances of {@link ConfigProperty} in the given package name.
 */
public class ConfigValidator
{
    protected Logger logger = new LoggerImpl(getClass().getName());
    protected final Reflections reflections;

    public ConfigValidator(String packageName)
    {
        reflections = new Reflections(new ConfigurationBuilder()
            .filterInputsBy(new FilterBuilder().includePackage(packageName))
            .setUrls(ClasspathHelper.forPackage(packageName))
            .setScanners(
                new SubTypesScanner(),
                new TypeAnnotationsScanner()
            )
        );
    }

    /**
     * Perform all config validations
     *
     * @param propNamesFromConfiguration a set of Strings representing
     *                                   all of the property names that were parsed
     *                                   from any configuration sources. We'll check
     *                                   that all of these have corresponding
     *                                   {@link ConfigProperty}s which read them and
     *                                   warn if one isn't read by anything
     */
    public void validate(Set<String> propNamesFromConfiguration)
    {
        checkForDefinedObsoleteProperties();
        checkForUnknownProperties(propNamesFromConfiguration);
    }

    protected Set<Class<? extends ConfigProperty>> getConfigProperties()
    {
        return reflections.getSubTypesOf(ConfigProperty.class);
    }

    /**
     * Warns about configuration properties which have been defined in a configuration source
     * but are marked as obsolete in their corresponding {@link ConfigProperty} class.
     */
    protected void checkForDefinedObsoleteProperties()
    {
        Set<Class<? extends ConfigProperty>> obsoleteConfigProperties = getConfigProperties()
            .stream()
            .filter(clazz -> clazz.isAnnotationPresent(ObsoleteConfig.class))
            .collect(Collectors.toSet());

        for (Class<? extends ConfigProperty> obsoleteConfigProperty : obsoleteConfigProperties)
        {
            if (Modifier.isAbstract(obsoleteConfigProperty.getModifiers()))
            {
                continue;
            }
            try
            {
                Constructor<? extends ConfigProperty> ctor = obsoleteConfigProperty.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object value = ctor.newInstance().get();
                ObsoleteConfig anno = obsoleteConfigProperty.getAnnotation(ObsoleteConfig.class);
                logger.warn("Prop " + obsoleteConfigProperty + " is obsolete but was present in config with " +
                    "value '" + value.toString() + "': " + anno.value());
            }
            catch (NoSuchMethodException e)
            {
                logger.error("Configuration property " + obsoleteConfigProperty +
                    " must have a no-arg constructor!");
            }
            catch (InvocationTargetException e)
            {
                // We don't get a raw ConfigPropertyNotFoundException
                // when calling it this way, instead it's wrapped by
                // an InvocationTargetException
                if (e.getCause() instanceof ConfigPropertyNotFoundException)
                {
                    logger.debug("Prop " + obsoleteConfigProperty + " is obsolete but wasn't found defined, ok!");
                }
                else
                {
                    logger.debug("Error creating instance of " + obsoleteConfigProperty + ": " + e.toString());
                }
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                logger.error("Error creating instance of " + obsoleteConfigProperty + ": " + e.toString());
            }
        }
    }

    /**
     * Scan the new config for properties within the 'videobridge' scope
     * which don't have a class which reads them
     */
    protected void checkForUnknownProperties(Set<String> propNames)
    {
        for (String key : propNames)
        {
            System.out.println(key);
            if (!doesAnyPropReadPropName(key))
            {
                logger.error("Config property " + key + " was defined in your config, but no " +
                    "property class reads it.");
            }
        }
    }

    /**
     * Returns true if any {@link ConfigProperty} instance found in the package
     * reads the given property name.  NOTE: the way we determine whether or
     * not a {@link ConfigProperty} reads a property name is to search the
     * {@code String} members contained by that {@link ConfigProperty}.
     *
     * @param propName
     * @return
     */
    protected boolean doesAnyPropReadPropName(String propName)
    {
        // Try and find any config property which reads this key
        for (Class<? extends ConfigProperty> configProperty : getConfigProperties())
        {
            for (Field field : configProperty.getDeclaredFields())
            {
                // We assume all 'String' fields contain property names
                // which, for this purpose, is probably fine because even if
                // we read a non-property name string, it's unlikely that
                // it would match something in a config file (and, even if it
                // did, this just prints a warning)
                if (field.getType() != String.class)
                {
                    continue;
                }
                field.setAccessible(true);
                try
                {
                    String propKey = (String) field.get(null);
                    if (propKey.equalsIgnoreCase(propName))
                    {
                        return true;
                    }
                } catch (IllegalAccessException e)
                {
                    logger.warn("Unable to read field " + field + " of class " + configProperty);
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
