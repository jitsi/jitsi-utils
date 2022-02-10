/*
 * Copyright @ 2022 - present 8x8, Inc.
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

/**
 * Returns information about an endpoint's energy levels relative
 * to other endpoints.
 */
public class SpeakerRanking
{
    /**
     * Whether the endpoint is currently the dominant speaker.
     */
    public final boolean isDominant;

    /**
     * The endpoint's current rank by energy level.
     * If the endpoint is not in the list of the current loudest speakers,
     * this will be set to the size of the list. In essence, all untracked endpoints
     * are considered tied for the next highest rank after the tracked ones.
     */
    public final int energyRanking;

    /**
     * The endpoint's energy score, a smoothed average of processed
     * energy levels.
     */
    public final int energyScore;

    /**
     * Initializes a new <tt>SpeakerRanking</tt> instance.
     */
    public SpeakerRanking(boolean isDominant_, int energyRanking_, int energyScore_)
    {
        isDominant = isDominant_;
        energyRanking = energyRanking_;
        energyScore = energyScore_;
    }
}
