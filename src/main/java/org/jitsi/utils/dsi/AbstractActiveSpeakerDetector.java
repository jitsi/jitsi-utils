/*
 * Copyright @ 2015 Atlassian Pty Ltd
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
package org.jitsi.utils.dsi;

import java.util.*;

/**
 * Provides a base {@link ActiveSpeakerDetector} which aids the implementations
 * of actual algorithms for the detection/identification of the active/dominant
 * speaker in a multipoint conference.
 *
 * @author Boris Grozev
 * @author Lyubomir Marinov
 */
public abstract class AbstractActiveSpeakerDetector<T>
    implements ActiveSpeakerDetector<T>
{
    /**
     * The list of listeners to be notified by this detector when the active
     * speaker changes.
     */
    private final List<ActiveSpeakerChangedListener<T>> listeners = new LinkedList<>();

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the specified <tt>listener</tt> is
     * <tt>null</tt>
     */
    @Override
    public void addActiveSpeakerChangedListener(
            ActiveSpeakerChangedListener<T> listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("listener");
        }

        synchronized (listeners)
        {
            if (!listeners.contains(listener))
            {
                listeners.add(listener);
            }
        }
    }

    /**
     * Notifies the <tt>ActiveSpeakerChangedListener</tt>s registered with this
     * instance that the active speaker in multipoint conference associated with
     * this instance has changed and is identified by a specific ID.
     *
     * @param id the identifier of the new dominant speaker.
     */
    protected void fireActiveSpeakerChanged(T id)
    {
        for (ActiveSpeakerChangedListener<T> listener : listeners)
        {
            listener.activeSpeakerChanged(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeActiveSpeakerChangedListener(ActiveSpeakerChangedListener<T> listener)
    {
        if (listener != null)
        {
            synchronized (listeners)
            {
                listeners.remove(listener);
            }
        }
    }
}
