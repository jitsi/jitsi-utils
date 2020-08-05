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
 * Contains version information of the Jitsi instance that we're currently
 * running.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public interface Version
    extends Comparable<Version>
{
    /**
     * Returns the version major of the current Jitsi version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the Jitsi.
     *
     * @return the version major integer.
     */
    int getVersionMajor();

    /**
     * Returns the version minor of the current Jitsi version. In an
     * example 2.3.1 version string 3 is the version minor. The version minor
     * number changes after adding enhancements and possibly new features to a
     * given Jitsi version.
     *
     * @return the version minor integer.
     */
    int getVersionMinor();

    /**
     * Indicates if this Jitsi version corresponds to a nightly build
     * of a repository snapshot or to an official Jitsi release.
     *
     * @return true if this is a build of a nightly repository snapshot and
     * false if this is an official Jitsi release.
     */
    boolean isNightly();

    /**
     * If this is a nightly build, returns the build identifies (e.g.
     * nightly-2007.12.07-06.45.17). If this is not a nightly build Jitsi
     * version, the method returns null.
     *
     * @return a String containing a nightly build identifier or null if
     */
    String getNightlyBuildID();

    /**
     * Indicates whether this version represents a prerelease (i.e. a
     * non-complete release like an alpha, beta or release candidate version).
     * @return true if this version represents a prerelease and false otherwise.
     */
    boolean isPreRelease();

    /**
     * Returns the version prerelease ID of the current Jitsi version
     * and null if this version is not a prerelease. Version pre-release id-s
     * exist only for pre-releaseversions and are <tt>null<tt/> otherwise. Note
     * that pre-relesae versions are not used by Jitsi's current versioning
     * convention
     *
     * @return a String containing the version prerelease ID.
     */
    String getPreReleaseID();

    /**
     * Compares another <tt>Version</tt> object to this one and returns a
     * negative, zero or a positive integer if this version instance represents
     * respectively an earlier, same, or later version as the one indicated
     * by the <tt>version</tt> parameter.
     *
     * @param version the <tt>Version</tt> instance that we'd like to compare
     * to this one.
     *
     * @return a negative integer, zero, or a positive integer as this object
     * represents a version that is earlier, same, or more recent than the one
     * referenced by the <tt>version</tt> parameter.
     */
    int compareTo(Version version);

    /**
     * Compares the <tt>version</tt> parameter to this version and returns true
     * if and only if both reference the same Jitsi version and
     * false otherwise.
     *
     * @param version the version instance that we'd like to compare with this
     * one.
     * @return true if and only the version param references the same
     * Jitsi version as this Version instance and false otherwise.
     */
    boolean equals(Object version);

    /**
     * Returns the name of the application that we're currently running. Default
     * MUST be Jitsi.
     *
     * @return the name of the application that we're currently running. Default
     * MUST be Jitsi.
     */
    String getApplicationName();
}
