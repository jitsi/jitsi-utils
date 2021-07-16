/*
 * Copyright @ 2015 - present, 8x8 Inc
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
package org.jitsi.utils.version;

/**
 * An implementation of {@link Version}.
 *
 * @author Emil Ivov
 * @author Pawel Domas
 * @author Boris Grozev
 */
public class VersionImpl implements Version
{
    /**
     * The application name field.
     */
    private final String applicationName;

    /**
     * The version major field.
     */
    private final int versionMajor;

    /**
     * The version minor field.
     */
    private final int versionMinor;

    /**
     * The nightly build id field.
     */
    private final String nightlyBuildID;

    /**
     * The pre-release ID field.
     */
    private final String preReleaseId;

    /**
     * Creates version object with custom major, minor and nightly build id.
     *
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     */
    public VersionImpl(String applicationName,
                          int majorVersion,
                          int minorVersion)
    {
        this(applicationName, majorVersion, minorVersion, null);
    }

    /**
     * Creates version object with custom major, minor and nightly build id.
     *
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     */
    public VersionImpl(String applicationName,
                          int majorVersion,
                          int minorVersion,
                          String nightlyBuildID)
    {
        this(applicationName, majorVersion, minorVersion, nightlyBuildID, null);
    }

    /**
     * Creates version object with custom major, minor and nightly build id.
     *
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     */
    public VersionImpl(String applicationName,
                          int majorVersion,
                          int minorVersion,
                          String nightlyBuildID,
                          String preReleaseId)
    {
        this.applicationName = applicationName;
        this.versionMajor = majorVersion;
        this.versionMinor = minorVersion;
        this.nightlyBuildID = nightlyBuildID;
        this.preReleaseId = preReleaseId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getVersionMajor()
    {
        return versionMajor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVersionMinor()
    {
        return versionMinor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNightly()
    {
        return nightlyBuildID != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNightlyBuildID()
    {
        if (!isNightly())
            return null;

        return nightlyBuildID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPreRelease()
    {
        return preReleaseId != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPreReleaseID()
    {
        return preReleaseId;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Version version)
    {
        if (version == null)
            return -1;

        if (getVersionMajor() != version.getVersionMajor())
            return getVersionMajor() - version.getVersionMajor();

        if (getVersionMinor() != version.getVersionMinor())
            return getVersionMinor() - version.getVersionMinor();

        try
        {
            return compareNightlyBuildIDByComponents(
                getNightlyBuildID(), version.getNightlyBuildID());
        }
        catch(Throwable th)
        {
            // if parsing fails will continue with lexicographically compare
        }

        return getNightlyBuildID().compareTo(version.getNightlyBuildID());
    }

    /**
     *  As normally nightly.build.id is in the form of <build-num> or
     *  <build-num>.<revision> we will first try to compare them by splitting
     *  the id in components and compare them one by one asnumbers
     * @param v1 the first version to compare
     * @param v2 the second version to compare
     * @return a negative integer, zero, or a positive integer as the first
     * parameter <tt>v1</tt> represents a version that is earlier, same,
     * or more recent than the one referenced by the <tt>v2</tt> parameter.
     */
    private static int compareNightlyBuildIDByComponents(String v1, String v2)
    {
        String[] s1 = v1.split("\\.");
        String[] s2 = v2.split("\\.");

        int len = Math.max(s1.length, s2.length);
        for (int i = 0; i < len; i++)
        {
            int n1 = 0;
            int n2 = 0;

            if (i < s1.length)
                n1 = Integer.parseInt(s1[i]);
            if (i < s2.length)
                n2 = Integer.parseInt(s2[i]);

            if (n1 == n2)
                continue;
            else
                return n1 - n2;
        }

        // will happen if boths version has identical numbers in
        // their components (even if one of them is longer, has more components)
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object version)
    {
        if (version == null)
        {
            return false;
        }

        //simply compare the version strings
        return toString().equals(version.toString());
    }

    @Override
    public String getApplicationName()
    {
        return applicationName;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Returns a String representation of this Version instance in the generic
     * form of major.minor[.nightly.build.id].
     *
     * @return a major.minor[.build] String containing the complete
     * Jitsi version.
     */
    @Override
    public String toString()
    {
        StringBuffer versionStringBuff = new StringBuffer();

        versionStringBuff.append(getVersionMajor());
        versionStringBuff.append(".");
        versionStringBuff.append(getVersionMinor());

        if (isPreRelease())
        {
            versionStringBuff.append("-");
            versionStringBuff.append(getPreReleaseID());
        }

        if (isNightly())
        {
            versionStringBuff.append(".");
            versionStringBuff.append(getNightlyBuildID());
        }

        return versionStringBuff.toString();
    }
}
