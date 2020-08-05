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
package org.jitsi.service.configuration;

import java.beans.*;
import java.util.*;

/**
 * This interface uses SC's own ProperteyVetoException.
 */
public interface ConfigVetoableChangeListener extends EventListener
{
  /**
   * Fired before a Bean's property changes.
   *
   * @param e the change (containing the old and new values)
   * @throws ConfigPropertyVetoException if the change is vetoed by the listener
   */
  void vetoableChange(PropertyChangeEvent e) throws ConfigPropertyVetoException;
}
