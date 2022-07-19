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

import java.lang.ref.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.jetbrains.annotations.*;
import org.jitsi.utils.concurrent.*;
import org.jitsi.utils.logging2.*;
import org.json.simple.*;

/**
 * Implements {@link ActiveSpeakerDetector} with inspiration from the paper
 * &quot;Dominant Speaker Identification for Multipoint Videoconferencing&quot;
 * by Ilana Volfin and Israel Cohen.
 *
 * @param <T> The type used for speaker identifiers.
 *
 * @author Lyubomir Marinov
 */
@SuppressWarnings("unused")
public class DominantSpeakerIdentification<T>
    extends AbstractActiveSpeakerDetector<T>
{
    /**
     * The threshold of the relevant speech activities in the immediate
     * time-interval in &quot;global decision&quot;/&quot;Dominant speaker
     * selection&quot; phase of the algorithm.
     */
    private static final double C1 = 3;

    /**
     * The threshold of the relevant speech activities in the medium
     * time-interval in &quot;global decision&quot;/&quot;Dominant speaker
     * selection&quot; phase of the algorithm.
     */
    private static final double C2 = 2;

    /**
     * The threshold of the relevant speech activities in the long
     * time-interval in &quot;global decision&quot;/&quot;Dominant speaker
     * selection&quot; phase of the algorithm.
     */
    private static final double C3 = 0;

    /**
     * The indicator which determines whether the
     * <tt>DominantSpeakerIdentification</tt> class and its instances are to
     * execute in debug mode.
     */
    private static final boolean DEBUG;

    /**
     * The interval in milliseconds of the activation of the identification of
     * the dominant speaker in a multipoint conference.
     */
    private static final long DECISION_INTERVAL = 300;

    /**
     * The interval of time in milliseconds of idle execution of
     * <tt>DecisionMaker</tt> after which the latter should cease to exist. The
     * interval does not have to be very long because the background threads
     * running the <tt>DecisionMaker</tt>s are pooled anyway.
     */
    private static final long DECISION_MAKER_IDLE_TIMEOUT = 15 * 1000;

    /**
     * The interval of time without a call to {@link Speaker#levelChanged(int)}
     * after which <tt>DominantSpeakerIdentification</tt> assumes that there
     * will be no report of a <tt>Speaker</tt>'s level within a certain
     * time-frame. The default value of <tt>40</tt> is chosen in order to allow
     * non-aggressive fading of the last received or measured level and to be
     * greater than the most common RTP packet durations in milliseconds i.e.
     * <tt>20</tt> and <tt>30</tt>. 
     */
    private static final long LEVEL_IDLE_TIMEOUT = 40;

    /**
     * The <tt>Logger</tt> used by the <tt>DominantSpeakerIdentification</tt>
     * class and its instances to print debug information.
     */
    private static final Logger logger = new LoggerImpl(DominantSpeakerIdentification.class.getName());

    /**
     * The (total) number of long time-intervals used for speech activity score
     * evaluation at a specific time-frame.
     */
    private static final int LONG_COUNT = 1;

    /**
     * The threshold in terms of active medium-length blocks which is used
     * during the speech activity evaluation step for the long time-interval.
     */
    private static final int LONG_THRESHOLD = 4;

    /**
     * The maximum value of audio level supported by
     * <tt>DominantSpeakerIdentification</tt>.
     */
    private static final int MAX_LEVEL = 127;

    /**
     * The minimum value of audio level supported by
     * <tt>DominantSpeakerIdentification</tt>.
     */
    private static final int MIN_LEVEL = 0;

    /**
     * The number of (audio) levels received or measured for a <tt>Speaker</tt>
     * to be monitored in order to determine that the minimum level for the
     * <tt>Speaker</tt> has increased.
     */
    private static final int MIN_LEVEL_WINDOW_LENGTH
        = 15 /* seconds */ * 1000 /* milliseconds */
            / 20 /* milliseconds per level */;

    /**
     * The minimum value of speech activity score supported by
     * <tt>DominantSpeakerIdentification</tt>. The value must be positive
     * because (1) we are going to use it as the argument of a logarithmic
     * function and the latter is undefined for negative arguments and (2) we
     * will be dividing by the speech activity score.
     */
    private static final double MIN_SPEECH_ACTIVITY_SCORE = 0.0000000001D;

    /**
     * The threshold in terms of active sub-bands in a frame which is used
     * during the speech activity evaluation step for the medium length
     * time-interval.
     */
    private static final int MEDIUM_THRESHOLD = 7;

    /**
     * The (total) number of sub-bands in the frequency range evaluated for
     * immediate speech activity. The implementation of the class
     * <tt>DominantSpeakerIdentification</tt> does not really operate on the
     * representation of the signal in the frequency domain, it works with audio
     * levels derived from RFC 6465 &quot;A Real-time Transport Protocol (RTP)
     * Header Extension for Mixer-to-Client Audio Level Indication&quot;. 
     */
    private static final int N1 = 13;

    /**
     * The length/size of a sub-band in the frequency range evaluated for
     * immediate speech activity. In the context of the implementation of the
     * class <tt>DominantSpeakerIdentification</tt>, it specifies the
     * length/size of a sub-unit of the audio level range defined by RFC 6465.
     */
    private static final int N1_SUBUNIT_LENGTH = (MAX_LEVEL - MIN_LEVEL + N1 - 1) / N1;

    /**
     * The number of frames (i.e. {@link Speaker#immediates} evaluated for
     * medium speech activity.
     */
    private static final int N2 = 5;

    /**
     * The number of medium-length blocks constituting a long time-interval.
     */
    private static final int N3 = 10;

    /**
     * The interval of time without a call to {@link Speaker#levelChanged(int)}
     * after which <tt>DominantSpeakerIdentification</tt> assumes that a
     * non-dominant <tt>Speaker</tt> is to be automatically removed from
     * {@link #speakers}.
     */
    private static final long SPEAKER_IDLE_TIMEOUT = 60 * 60 * 1000;

    private static final ScheduledExecutorService DEFAULT_EXECUTOR
            = Executors.newScheduledThreadPool(1, new CustomizableThreadFactory("dsi", true));

    static
    {
        DEBUG = logger.isDebugEnabled();
    }

    /**
     * Computes the binomial coefficient indexed by <tt>n</tt> and <tt>r</tt>
     * i.e. the number of ways of picking <tt>r</tt> unordered outcomes from
     * <tt>n</tt> possibilities.
     *
     * @param n the number of possibilities to pick from
     * @param r the number unordered outcomes to pick from <tt>n</tt>
     * @return the binomial coefficient indexed by <tt>n</tt> and <tt>r</tt>
     * i.e. the number of ways of picking <tt>r</tt> unordered outcomes from
     * <tt>n</tt> possibilities
     */
    private static long binomialCoefficient(int n, int r)
    {
        int m = n - r; // r = Math.max(r, n - r);

        if (r < m)
        {
            r = m;
        }

        long t = 1;

        for (int i = n, j = 1; i > r; i--, j++)
        {
            t = t * i / j;
        }

        return t;
    }

    private static boolean computeBigs(
            byte[] littles,
            byte[] bigs,
            int threshold)
    {
        int bigLength = bigs.length;
        int littleLengthPerBig = littles.length / bigLength;
        boolean changed = false;

        for (int b = 0, l = 0; b < bigLength; b++)
        {
            byte sum = 0;

            for (int lEnd = l + littleLengthPerBig; l < lEnd; l++)
            {
                if (littles[l] > threshold)
                {
                    sum++;
                }
            }
            if (bigs[b] != sum)
            {
                bigs[b] = sum;
                changed = true;
            }
        }
        return changed;
    }

    private static double computeSpeechActivityScore(
            int vL,
            int nR,
            double lambda)
    {
        double p = 0.5;
        double speechActivityScore
            = Math.log(binomialCoefficient(nR, vL)) + vL * Math.log(p)
                + (nR - vL) * Math.log(1 - p) - Math.log(lambda) + lambda * vL;

        if (speechActivityScore < MIN_SPEECH_ACTIVITY_SCORE)
        {
            speechActivityScore = MIN_SPEECH_ACTIVITY_SCORE;
        }
        return speechActivityScore;
    }

    /**
     * The background task which repeatedly makes the (global) decision about speaker switches.
     */
    private DecisionMaker decisionMaker;

    /**
     * The identifier of the dominant speaker.
     */
    private T dominantId;

    /**
     * The last/latest time at which this <tt>DominantSpeakerIdentification</tt>
     * made a (global) decision about speaker switches. The (global) decision
     * about switcher switches should be made every {@link #DECISION_INTERVAL}
     * milliseconds.
     */
    private long lastDecisionTime;

    /**
     * The time in milliseconds of the most recent (audio) level report or
     * measurement (regardless of the <tt>Speaker</tt>).
     */
    private long lastLevelChangedTime;

    /**
     * The last/latest time at which this <tt>DominantSpeakerIdentification</tt>
     * notified the <tt>Speaker</tt>s who have not received or measured audio
     * levels for a certain time (i.e. {@link #LEVEL_IDLE_TIMEOUT}) that they
     * will very likely not have a level within a certain time-frame of the
     * algorithm.
     */
    private long lastLevelIdleTime;

    /**
     * The relative speech activities for the immediate, medium and long
     * time-intervals, respectively, which were last calculated for a
     * <tt>Speaker</tt>. Simply reduces the number of allocations and the
     * penalizing effects of the garbage collector.
     */
    private final double[] relativeSpeechActivities = new double[3];

    /**
     * The <tt>Speaker</tt>s in the multipoint conference associated with this
     * <tt>ActiveSpeakerDetector</tt>.
     */
    private final Map<T, Speaker<T>> speakers = new HashMap<>();

    /**
     * The <tt>Speaker</tt>s in the multipoint conference with the highest
     * current energy levels.
     */
    private final ArrayList<Speaker<T>> loudest = new ArrayList<>();

    private final Clock clock;

    /**
     * The executor used to schedule {@link #decisionMaker}.
     */
    private final ScheduledExecutorService executor;

    /**
     * The number of current loudest speakers to keep track of.
     */
    private int numLoudestToTrack = 0;

    /**
     * Time in milliseconds after which speaker is removed from loudest list if
     * no new audio packets have been received from that speaker.
     */
    private int energyExpireTimeMs = 150;

    /**
     * Alpha factor for exponential smoothing of energy values, multiplied by 100.
     */
    private int energyAlphaPct = 50;

    /**
     * Initializes a new <tt>DominantSpeakerIdentification</tt> instance.
     */
    public DominantSpeakerIdentification()
    {
        this(Clock.systemUTC(), DEFAULT_EXECUTOR);
    }

    public DominantSpeakerIdentification(Clock clock, ScheduledExecutorService executor)
    {
        this.clock = clock;
        this.executor = executor;
    }

    /**
     * Set energy ranking options
     */
    public synchronized void setLoudestConfig(int numLoudestToTrack_, int energyExpireTimeMs_, int energyAlphaPct_)
    {
        numLoudestToTrack = numLoudestToTrack_;
        energyExpireTimeMs = energyExpireTimeMs_;
        energyAlphaPct = energyAlphaPct_;
        logger.trace(() -> "numLoudestToTrack = " + numLoudestToTrack);
        logger.trace(() -> "energyExpireTimeMs = " + energyExpireTimeMs);
        logger.trace(() -> "energyAlphaPct = " + energyAlphaPct);

        while (loudest.size() > numLoudestToTrack)
            loudest.remove(numLoudestToTrack);
    }

    /**
     * Notifies this <tt>DominantSpeakerIdentification</tt> instance that a
     * specific <tt>DecisionMaker</tt> has permanently stopped executing (in its
     * background/daemon <tt>Thread</tt>). If the specified
     * <tt>decisionMaker</tt> is the one utilized by this
     * <tt>DominantSpeakerIdentification</tt> instance, the latter will update
     * its state to reflect that the former has exited.
     *
     * @param decisionMaker the <tt>DecisionMaker</tt> which has exited
     */
    synchronized void decisionMakerExited(DecisionMaker decisionMaker)
    {
        if (this.decisionMaker == decisionMaker)
        {
            this.decisionMaker = null;
        }
    }

    /**
     * Retrieves a JSON representation of this instance for the purposes of the
     * REST API of Videobridge.
     * <p>
     * By the way, the method name reflects the fact that the method handles an
     * HTTP GET request.
     * </p>
     *
     * @return a <tt>JSONObject</tt> which represents this instance of the
     * purposes of the REST API of Videobridge
     */
    @SuppressWarnings("unchecked")
    public JSONObject doGetJSON()
    {
        JSONObject jsonObject;

        if (DEBUG)
        {
            synchronized (this)
            {
                jsonObject = new JSONObject();

                // dominantSpeaker
                T dominantSpeaker = getDominantSpeaker();

                jsonObject.put("dominantSpeaker", Objects.toString(dominantSpeaker));

                // speakers
                Collection<Speaker<T>> speakersCollection = this.speakers.values();
                JSONArray speakersArray = new JSONArray();

                for (Speaker<T> speaker : speakersCollection)
                {
                    JSONObject speakerJSONObject = new JSONObject();

                    // id
                    speakerJSONObject.put("id", speaker.id.toString());
                    // levels
                    speakerJSONObject.put("levels", speaker.getLevels());
                    speakersArray.add(speakerJSONObject);
                }
                jsonObject.put("speakers", speakersArray);
            }
        }
        else
        {
            // Retrieving a JSON representation of a
            // DominantSpeakerIdentification has been implemented for the
            // purposes of debugging only.
            jsonObject = null;
        }
        return jsonObject;
    }

    /**
     * Gets the identifier of the dominant speaker.
     */
    public T getDominantSpeaker()
    {
        return dominantId;
    }

    /**
     * Gets the <tt>Speaker</tt> in this multipoint conference identified by a
     * specific {@code id}. If no such <tt>Speaker</tt> exists, a new <tt>Speaker</tt>
     * is initialized and returned.
     *
     * @param id the identifier of the <tt>Speaker</tt> to return.
     * @return the <tt>Speaker</tt> in this multipoint conference identified by {@code id}.
     */
    @NotNull
    private synchronized Speaker<T> getOrCreateSpeaker(T id)
    {
        Speaker<T> speaker = speakers.get(id);

        if (speaker == null)
        {
            speaker = new Speaker<>(id);
            speakers.put(id, speaker);

            // Since we've created a new Speaker in the multipoint conference,
            // we'll very likely need to make a decision whether there have been
            // speaker switch events soon.
            maybeStartDecisionMaker();
        }
        return speaker;
    }

    /**
     * Update loudest speaker list.
     * @param speaker the speaker with a new energy level
     * @param level the energy level
     * @param now the current time
     * @return The current ranking statistics.
     */
    private synchronized SpeakerRanking updateLoudestList(Speaker<T> speaker, int level, long now)
    {
        boolean isDominant = dominantId != null && dominantId.equals(speaker.id);

        if (level < 0)
        {
            /* Ignore this level, it is too old. Just gather the stats. */
            int rank = 0;
            while (rank < loudest.size() && loudest.get(rank) != speaker)
                ++rank;

            return new SpeakerRanking(isDominant, rank, speaker.energyScore);
        }

        /* exponential smoothing. round to nearest. */
        speaker.energyScore = (energyAlphaPct * level + (100 - energyAlphaPct) * speaker.energyScore + 50) / 100;

        if (numLoudestToTrack == 0)
            return new SpeakerRanking(isDominant, 0, speaker.energyScore);

        long oldestValid = now - energyExpireTimeMs;

        logger.trace(() -> "Want to add " + speaker.id.toString()
            + " with score " + speaker.energyScore + ". Last level = " + level + ".");

        int i = 0;
        while (i < loudest.size())
        {
            Speaker<T> cur = loudest.get(i);
            if (cur.getLastLevelChangedTime() < oldestValid)
            {
                logger.trace(() -> "Removing " + cur.id.toString() + ". old.");
                loudest.remove(i);
                continue;
            }
            if (cur == speaker)
            {
                logger.trace(() -> "Removing " + cur.id.toString() + ". same.");
                loudest.remove(i);
                continue;
            }
            ++i;
        }

        int rank = 0;
        while (rank < loudest.size())
        {
            Speaker<T> cur = loudest.get(rank);
            if (cur.energyScore < speaker.energyScore)
                break;
            ++rank;
        }

        if (rank < numLoudestToTrack)
        {
            final int pos = rank;
            logger.trace(() -> "Adding " + speaker.id.toString() + " at position " + pos + ".");
            loudest.add(rank, speaker);

            if (loudest.size() > numLoudestToTrack)
                loudest.remove(numLoudestToTrack);
        }

        if (logger.isTraceEnabled())
        {
            i = 0;
            while (i < loudest.size())
            {
                Speaker<T> cur = loudest.get(i);
                logger.trace("New list: " + i + ": " + cur.id.toString() + ": " + cur.energyScore + ".");
                ++i;
            }
        }

        return new SpeakerRanking(isDominant, rank, speaker.energyScore);
    }

    /**
     * Query whether a particular endpoint is currently one of the loudest speakers.
     */
    public synchronized boolean isAmongLoudest(T id)
    {
        return loudest.stream().anyMatch(speaker -> speaker.id.equals(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SpeakerRanking levelChanged(T id, int level)
    {
        Speaker<T> speaker;
        long now = clock.millis();

        synchronized (this)
        {
            speaker = getOrCreateSpeaker(id);

            // Note that this ActiveSpeakerDetector is still in use. When it is
            // not in use long enough, its DecisionMaker i.e. background thread
            // will prepare itself and, consequently, this
            // DominantSpeakerIdentification for garbage collection.
            if (lastLevelChangedTime < now)
            {
                lastLevelChangedTime = now;

                // A report or measurement of an audio level indicates that this
                // DominantSpeakerIdentification is in use and, consequently,
                // that it'll very likely need to make a decision whether there
                // have been speaker switch events soon.
                maybeStartDecisionMaker();
            }
        }

        int cookedLevel = speaker.levelChanged(level, now);
        return updateLoudestList(speaker, cookedLevel, now);
    }

    /**
     * Makes the decision whether there has been a speaker switch event. If
     * there has been such an event, notifies the registered listeners that a
     * new speaker is dominating the multipoint conference.
     */
    private void makeDecision()
    {
        // If we have to fire events to any registered listeners eventually, we
        // will want to do it outside the synchronized block.
        T oldDominantSpeakerValue = null, newDominantSpeakerValue = null;

        synchronized (this)
        {

        int speakerCount = speakers.size();
        T newDominantId;

        if (speakerCount == 0)
        {
            // If there are no Speakers in a multipoint conference, then there
            // are no speaker switch events to detect.
            newDominantId = null;
        }
        else if (speakerCount == 1)
        {
            // If there is a single Speaker in a multipoint conference, then
            // his/her speech surely dominates.
            newDominantId = speakers.keySet().iterator().next();
        }
        else
        {
            Speaker<T> dominantSpeaker
                = (dominantId == null)
                    ? null
                    : speakers.get(dominantId);

            // If there is no dominant speaker, nominate one at random and then
            // let the other speakers compete with the nominated one.
            if (dominantSpeaker == null)
            {
                Map.Entry<T, Speaker<T>> s = speakers.entrySet().iterator().next();

                dominantSpeaker = s.getValue();
                newDominantId = s.getKey();
            }
            else
            {
                newDominantId = null;
            }

            dominantSpeaker.evaluateSpeechActivityScores();

            double[] relativeSpeechActivities = this.relativeSpeechActivities;
            // If multiple speakers cause speaker switches, they compete among
            // themselves by their relative speech activities in the middle
            // time-interval.
            double newDominantC2 = C2;

            for (Map.Entry<T, Speaker<T>> s : speakers.entrySet())
            {
                Speaker<T> speaker = s.getValue();

                // The dominant speaker does not compete with itself. In other
                // words, there is no use detecting a speaker switch from the
                // dominant speaker to the dominant speaker. Technically, the
                // relative speech activities are all zeroes for the dominant
                // speaker.
                if (speaker == dominantSpeaker)
                {
                    continue;
                }

                speaker.evaluateSpeechActivityScores();

                // Compute the relative speech activities for the immediate,
                // medium and long time-intervals.
                for (int interval = 0;
                        interval < relativeSpeechActivities.length;
                        ++interval)
                {
                    relativeSpeechActivities[interval]
                        = Math.log(
                                speaker.getSpeechActivityScore(interval)
                                    / dominantSpeaker.getSpeechActivityScore(
                                            interval));
                }

                double c1 = relativeSpeechActivities[0];
                double c2 = relativeSpeechActivities[1];
                double c3 = relativeSpeechActivities[2];

                if ((c1 > C1) && (c2 > C2) && (c3 > C3) && (c2 > newDominantC2))
                {
                    // If multiple speakers cause speaker switches, they compete
                    // among themselves by their relative speech activities in
                    // the middle time-interval.
                    newDominantC2 = c2;
                    newDominantId = s.getKey();
                }
            }
        }
        if ((newDominantId != null) && !newDominantId.equals(dominantId))
        {
            oldDominantSpeakerValue = dominantId;
            dominantId = newDominantId;
            newDominantSpeakerValue = dominantId;
        }

        } // synchronized (this)

        // Now that we are outside the synchronized block, fire events, if any,
        // to any registered listeners.
        if ((newDominantSpeakerValue != null) &&
            !newDominantSpeakerValue.equals(oldDominantSpeakerValue))
        {
            fireActiveSpeakerChanged(newDominantSpeakerValue);
        }
    }

    /**
     * Starts a background thread which is to repeatedly make the (global)
     * decision about speaker switches if such a background thread has not been
     * started yet and if the current state of this
     * <tt>DominantSpeakerIdentification</tt> justifies the start of such a
     * background thread (e.g. there is at least one <tt>Speaker</tt> in this
     * multipoint conference). 
     */
    private synchronized void maybeStartDecisionMaker()
    {
        if ((this.decisionMaker == null) && !speakers.isEmpty())
        {
            DecisionMaker decisionMaker = new DecisionMaker(this);
            boolean scheduled = false;

            this.decisionMaker = decisionMaker;
            try
            {
                executor.execute(decisionMaker);
                scheduled = true;
            }
            finally
            {
                if (!scheduled && (this.decisionMaker == decisionMaker))
                {
                    this.decisionMaker = null;
                }
            }
        }
    }

    /**
     * Runs in the background/daemon <tt>Thread</tt> of {@link #decisionMaker}
     * and makes the decision whether there has been a speaker switch event.
     *
     * @return a negative integer if the <tt>DecisionMaker</tt> is to exit or
     * a non-negative integer to specify the time in milliseconds until the next
     * execution of the <tt>DecisionMaker</tt>
     */
    private long runInDecisionMaker()
    {
        long now = clock.millis();
        long levelIdleTimeout = LEVEL_IDLE_TIMEOUT - (now - lastLevelIdleTime);
        long sleep = 0;

        if (levelIdleTimeout <= 0)
        {
            if (lastLevelIdleTime != 0)
            {
                timeoutIdleLevels(now);
            }
            lastLevelIdleTime = now;
        }
        else
        {
            sleep = levelIdleTimeout;
        }

        long decisionTimeout = DECISION_INTERVAL - (now - lastDecisionTime);

        if (decisionTimeout <= 0)
        {
            // The identification of the dominant active speaker may be a
            // time-consuming ordeal so the time of the last decision is the
            // time of the beginning of a decision iteration.
            lastDecisionTime = now;
            makeDecision();
            // The identification of the dominant active speaker may be a
            // time-consuming ordeal so the timeout to the next decision
            // iteration should be computed after the end of the decision
            // iteration.
            decisionTimeout = DECISION_INTERVAL - (clock.millis() - now);

        }
        if ((decisionTimeout > 0) && (sleep > decisionTimeout))
        {
            sleep = decisionTimeout;
        }

        return sleep;
    }

    /**
     * Runs in the background/daemon <tt>Thread</tt> of a specific
     * <tt>DecisionMaker</tt> and makes the decision whether there has been a
     * speaker switch event.
     *
     * @param decisionMaker the <tt>DecisionMaker</tt> invoking the method
     * @return a negative integer if the <tt>decisionMaker</tt> is to exit or
     * a non-negative integer to specify the time in milliseconds until the next
     * execution of the <tt>decisionMaker</tt>
     */
    long runInDecisionMaker(DecisionMaker decisionMaker)
    {
        synchronized (this)
        {
            // Most obviously, DecisionMakers no longer employed by this
            // DominantSpeakerIdentification should cease to exist as soon as
            // possible.
            if (this.decisionMaker != decisionMaker)
            {
                return -1;
            }

            // If the decisionMaker has been unnecessarily executing long
            // enough, kill it in order to have a more deterministic behavior
            // with respect to disposal.
            if (0 < lastDecisionTime)
            {
                long idle = lastDecisionTime - lastLevelChangedTime;

                if (idle >= DECISION_MAKER_IDLE_TIMEOUT)
                {
                    return -1;
                }
            }
        }

        return runInDecisionMaker();
    }

    /**
     * Notifies the <tt>Speaker</tt>s in this multipoint conference who have not
     * received or measured audio levels for a certain time (i.e.
     * {@link #LEVEL_IDLE_TIMEOUT}) that they will very likely not have a level
     * within a certain time-frame of the <tt>DominantSpeakerIdentification</tt>
     * algorithm. Additionally, removes the non-dominant <tt>Speaker</tt>s who
     * have not received or measured audio levels for far too long (i.e.
     * {@link #SPEAKER_IDLE_TIMEOUT}).
     *
     * @param now the time at which the timing out is being detected
     */
    private synchronized void timeoutIdleLevels(long now)
    {
        Iterator<Map.Entry<T, Speaker<T>>> i = speakers.entrySet().iterator();

        while (i.hasNext())
        {
            Speaker<T> speaker = i.next().getValue();
            long idle = now - speaker.getLastLevelChangedTime();

            // Remove a non-dominant Speaker if he/she has been idle for far too
            // long.
            if ((SPEAKER_IDLE_TIMEOUT < idle) && ((dominantId == null) || (speaker.id != dominantId)))
            {
                i.remove();
            }
            else if (LEVEL_IDLE_TIMEOUT < idle)
            {
                speaker.levelTimedOut();
            }
        }
    }

    /**
     * Represents the background thread which repeatedly makes the (global)
     * decision about speaker switches. Weakly references an associated
     * <tt>DominantSpeakerIdentification</tt> instance in order to eventually
     * detect that the multipoint conference has actually expired and that the
     * background <tt>Thread</tt> should perish.
     *
     * @author Lyubomir Marinov
     */
    private static class DecisionMaker
        implements Runnable
    {
        /**
         * The <tt>DominantSpeakerIdentification</tt> instance which is
         * repeatedly run into this background thread in order to make the
         * (global) decision about speaker switches. It is a
         * <tt>WeakReference</tt> in order to eventually detect that the
         * mulipoint conference has actually expired and that this background
         * <tt>Thread</tt> should perish.
         */
        private final WeakReference<DominantSpeakerIdentification<?>> algorithm;

        /**
         * Initializes a new <tt>DecisionMaker</tt> instance which is to
         * repeatedly run a specific <tt>DominantSpeakerIdentification</tt>
         * into a background thread in order to make the (global) decision about
         * speaker switches.
         *
         * @param algorithm the <tt>DominantSpeakerIdentification</tt> to be
         * repeatedly run by the new instance in order to make the (global)
         * decision about speaker switches
         */
        public DecisionMaker(DominantSpeakerIdentification<?> algorithm)
        {
            this.algorithm = new WeakReference<>(algorithm);
        }

        /**
         * Repeatedly runs {@link #algorithm} i.e. makes the (global) decision
         * about speaker switches until the multipoint conference expires.
         */
        @Override
        public void run()
        {
            boolean exit = false;

            DominantSpeakerIdentification<?> algorithm = this.algorithm.get();

            if (algorithm == null)
            {
                exit = true;
            }
            else
            {
                long sleep;
                try
                {
                    sleep = algorithm.runInDecisionMaker(this);
                }
                catch (Exception e)
                {
                    // If an exception occurs we do not re-schedule.
                    sleep = -1;
                }

                // A negative sleep value is contracted to mean that this DecisionMaker should not re-schedule itself.
                if (sleep < 0)
                {
                    exit = true;
                }
                else
                {
                    algorithm.executor.schedule(this, sleep, TimeUnit.MILLISECONDS);
                }
            }

            if (exit)
            {
                // Notify the algorithm that this DecisionMaker no longer run. Subsequently, the algorithm may decide
                // to create and schedule another one if and when it's needed.
                algorithm = this.algorithm.get();

                if (algorithm != null)
                {
                    algorithm.decisionMakerExited(this);
                }
            }
        }
    }

    /**
     * Represents a speaker in a multipoint conference identified by an ID.
     *
     * @author Lyubomir Marinov
     */
    private class Speaker<U>
    {
        private final byte[] immediates = new byte[LONG_COUNT * N3 * N2];

        /**
         * The speech activity score of this <tt>Speaker</tt> for the immediate
         * time-interval.
         */
        private double immediateSpeechActivityScore = MIN_SPEECH_ACTIVITY_SCORE;

        /**
         * The time in milliseconds of the most recent invocation of
         * {@link #levelChanged(int)} i.e. the last time at which an actual
         * (audio) level was reported or measured for this <tt>Speaker</tt>. If
         * no level is reported or measured for this <tt>Speaker</tt> long
         * enough i.e. {@link #LEVEL_IDLE_TIMEOUT}, the associated
         * <tt>DominantSpeakerIdentification</tt> will presume that this
         * <tt>Speaker</tt> was muted for the duration of a certain frame.
         */
        private long lastLevelChangedTime = clock.millis();

        /**
         * The (history of) audio levels received or measured for this
         * <tt>Speaker</tt>.
         */
        private final byte[] levels;

        private final byte[] longs = new byte[LONG_COUNT];

        /**
         * The speech activity score of this <tt>Speaker</tt> for the long
         * time-interval.
         */
        private double longSpeechActivityScore = MIN_SPEECH_ACTIVITY_SCORE;

        private final byte[] mediums = new byte[LONG_COUNT * N3];

        /**
         * The speech activity score of this <tt>Speaker</tt> for the medium
         * time-interval.
         */
        private double mediumSpeechActivityScore = MIN_SPEECH_ACTIVITY_SCORE;

        /**
         * The minimum (audio) level received or measured for this
         * <tt>Speaker</tt>. Since <tt>MIN_LEVEL</tt> is specified for samples
         * generated by a muted audio source, a value equal to
         * <tt>MIN_LEVEL</tt> indicates that the minimum level for this
         * <tt>Speaker</tt> has not been determined yet.
         */
        private byte minLevel = MIN_LEVEL;

        /**
         * The (current) estimate of the minimum (audio) level received or
         * measured for this <tt>Speaker</tt>. Used to increase the value of
         * {@link #minLevel}
         */
        private byte nextMinLevel = MIN_LEVEL;

        /**
         * The number of subsequent (audio) levels received or measured for this
         * <tt>Speaker</tt> which have been monitored thus far in order to
         * estimate an up-to-date minimum (audio) level received or measured for
         * this <tt>Speaker</tt>.
         */
        private int nextMinLevelWindowLength;

        /** Exponential smoothing of filtered energy values.
         *  Synchronized by parent class.
         */
        int energyScore;

        /**
         * The identifier of this <tt>Speaker</tt> which is unique within this {@link DominantSpeakerIdentification}.
         */
        public final U id;

        /**
         * Initializes a new <tt>Speaker</tt> instance with a specific identifier
         *
         * @param id the object identifying this speaker.
         * instance
         */
        public Speaker(U id)
        {
            this.id = id;

            levels = new byte[immediates.length];
        }

        private boolean computeImmediates()
        {
            // The minimum audio level received or measured for this Speaker is
            // the level of "silence" for this Speaker. Since the various
            // Speakers may differ in their levels of "silence", put all
            // Speakers on equal footing by replacing the individual levels of
            // "silence" with the uniform level of absolute silence.
            byte[] immediates = this.immediates;
            byte[] levels = this.levels;
            byte minLevel = (byte) (this.minLevel + N1_SUBUNIT_LENGTH);
            boolean changed = false;

            for (int i = 0; i < immediates.length; ++i)
            {
                byte level = levels[i];

                if (level < minLevel)
                    level = MIN_LEVEL;

                byte immediate = (byte) (level / N1_SUBUNIT_LENGTH);

                if (immediates[i] != immediate)
                {
                    immediates[i] = immediate;
                    changed = true;
                }
            }
            return changed;
        }

        private boolean computeLongs()
        {
            return computeBigs(mediums, longs, LONG_THRESHOLD);
        }

        private boolean computeMediums()
        {
            return computeBigs(immediates, mediums, MEDIUM_THRESHOLD);
        }

        /**
         * Computes/evaluates the speech activity score of this <tt>Speaker</tt>
         * for the immediate time-interval.
         */
        private void evaluateImmediateSpeechActivityScore()
        {
            immediateSpeechActivityScore = computeSpeechActivityScore(immediates[0], N1, 0.78);
        }

        /**
         * Computes/evaluates the speech activity score of this <tt>Speaker</tt>
         * for the long time-interval.
         */
        private void evaluateLongSpeechActivityScore()
        {
            longSpeechActivityScore = computeSpeechActivityScore(longs[0], N3, 47);
        }

        /**
         * Computes/evaluates the speech activity score of this <tt>Speaker</tt>
         * for the medium time-interval.
         */
        private void evaluateMediumSpeechActivityScore()
        {
            mediumSpeechActivityScore = computeSpeechActivityScore(mediums[0], N2, 24);
        }

        /**
         * Evaluates the speech activity scores of this <tt>Speaker</tt> for the
         * immediate, medium, and long time-intervals. Invoked when it is time
         * to decide whether there has been a speaker switch event.
         */
        synchronized void evaluateSpeechActivityScores()
        {
            if (computeImmediates())
            {
                evaluateImmediateSpeechActivityScore();
                if (computeMediums())
                {
                    evaluateMediumSpeechActivityScore();
                    if (computeLongs())
                    {
                        evaluateLongSpeechActivityScore();
                    }
                }
            }
        }

        /**
         * Gets the time in milliseconds at which an actual (audio) level was
         * reported or measured for this <tt>Speaker</tt> last.
         *
         * @return the time in milliseconds at which an actual (audio) level
         * was reported or measured for this <tt>Speaker</tt> last
         */
        public synchronized long getLastLevelChangedTime()
        {
            return lastLevelChangedTime;
        }

        /**
         * Gets the (history of) audio levels received or measured for this
         * <tt>Speaker</tt>.
         *
         * @return a <tt>String</tt> that lists the (history of) audio
         * levels received or measured for this <tt>Speaker</tt>
         */
        String getLevels()
        {
            // The levels of Speaker are internally maintained starting with the
            // last audio level received or measured for this Speaker and ending
            // with the first audio level received or measured for this Speaker.
            // Reverse the list and print them in the order they were received.
            byte[] src = this.levels;
            StringBuilder sb = new StringBuilder();
            sb.append('[');

            for (int s = src.length - 1; s >= 0; --s)
            {
                sb.append(src[s]);
                sb.append(',');
            }

            sb.setCharAt(sb.length() - 1, ']'); /* replace last comma */
            return sb.toString();
        }

        /**
         * Gets the speech activity score of this <tt>Speaker</tt> for a
         * specific time-interval.
         *
         * @param interval <tt>0</tt> for the immediate time-interval,
         * <tt>1</tt> for the medium time-interval, or <tt>2</tt> for the long
         * time-interval
         * @return the speech activity score of this <tt>Speaker</tt> for the
         * time-interval specified by <tt>index</tt>
         */
        double getSpeechActivityScore(int interval)
        {
            switch (interval)
            {
            case 0:
                return immediateSpeechActivityScore;
            case 1:
                return mediumSpeechActivityScore;
            case 2:
                return longSpeechActivityScore;
            default:
                throw new IllegalArgumentException("interval " + interval);
            }
        }

        /**
         * Notifies this <tt>Speaker</tt> that a new audio level has been
         * received or measured.
         *
         * @param level the audio level which has been received or measured for
         * this <tt>Speaker</tt>
         */
        @SuppressWarnings("unused")
        public void levelChanged(int level)
        {
            levelChanged(level, clock.millis());
        }

        /**
         * Notifies this <tt>Speaker</tt> that a new audio level has been
         * received or measured at a specific time.
         *
         * @param level the audio level which has been received or measured for
         * this <tt>Speaker</tt>
         * @param time the (local <tt>System</tt>) time in milliseconds at which
         * the specified <tt>level</tt> has been received or measured
         * @return the audio level that was applied, after any filtering or
         * level adjustment has taken place. If negative, the audio level
         * was ignored.
         */
        public synchronized int levelChanged(int level, long time)
        {
            // It sounds relatively reasonable that late audio levels should
            // better be discarded.
            if (lastLevelChangedTime <= time)
            {
                lastLevelChangedTime = time;

                // Ensure that the specified level is within the supported
                // range.
                byte b;

                if (level < MIN_LEVEL)
                    b = MIN_LEVEL;
                else if (level > MAX_LEVEL)
                    b = MAX_LEVEL;
                else
                    b = (byte) level;

                // Push the specified level into the history of audio levels
                // received or measured for this Speaker.
                System.arraycopy(levels, 0, levels, 1, levels.length - 1);
                levels[0] = b;

                // Determine the minimum level received or measured for this
                // Speaker.
                updateMinLevel(b);

                return b >= (minLevel + N1_SUBUNIT_LENGTH) ? b : 0;
            }
            else
            {
                return -1;
            }
        }

        /**
         * Notifies this <tt>Speaker</tt> that no new audio level has been
         * received or measured for a certain time which very likely means that
         * this <tt>Speaker</tt> will not have a level within a certain
         * time-frame of a <tt>DominantSpeakerIdentification</tt> algorithm.
         */
        public synchronized void levelTimedOut()
        {
            levelChanged(MIN_LEVEL, lastLevelChangedTime);
        }

        /**
         * Updates the minimum (audio) level received or measured for this
         * <tt>Speaker</tt> in light of the receipt of a specific level.
         *
         * @param level the audio level received or measured for this
         * <tt>Speaker</tt>
         */
        private void updateMinLevel(byte level)
        {
            if (level != MIN_LEVEL)
            {
                if ((minLevel == MIN_LEVEL) || (minLevel > level))
                {
                    minLevel = level;
                    nextMinLevel = MIN_LEVEL;
                    nextMinLevelWindowLength = 0;
                }
                else
                {
                    // The specified (audio) level is greater than the minimum
                    // level received or measure for this Speaker. However, the
                    // minimum level may be out-of-date by now. Estimate an
                    // up-to-date minimum level and, eventually, make it the
                    // minimum level received or measured for this Speaker.
                    if (nextMinLevel == MIN_LEVEL)
                    {
                        nextMinLevel = level;
                        nextMinLevelWindowLength = 1;
                    }
                    else
                    {
                        if (nextMinLevel > level)
                        {
                            nextMinLevel = level;
                        }
                        nextMinLevelWindowLength++;
                        if (nextMinLevelWindowLength >= MIN_LEVEL_WINDOW_LENGTH)
                        {
                            // The arithmetic mean will increase the minimum
                            // level faster than the geometric mean. Since the
                            // goal is to track a minimum, it sounds reasonable
                            // to go with a slow increase.
                            double newMinLevel
                                = Math.sqrt(minLevel * (double) nextMinLevel);

                            // Ensure that the new minimum level is within the
                            // supported range.
                            if (newMinLevel < MIN_LEVEL)
                                newMinLevel = MIN_LEVEL;
                            else if (newMinLevel > MAX_LEVEL)
                                newMinLevel = MAX_LEVEL;

                            minLevel = (byte) newMinLevel;

                            nextMinLevel = MIN_LEVEL;
                            nextMinLevelWindowLength = 0;
                        }
                    }
                }
            }
        }
    }
}
