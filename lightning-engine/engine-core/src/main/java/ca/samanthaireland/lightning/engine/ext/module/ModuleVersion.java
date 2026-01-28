/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ca.samanthaireland.lightning.engine.ext.module;

/**
 * Represents a semantic version for a module.
 *
 * <p>Versions follow semantic versioning: major.minor[.patch]
 *
 * <p>Compatibility rules:
 * <ul>
 *   <li>Major version must match exactly</li>
 *   <li>Minor version must be >= required</li>
 *   <li>Patch version is ignored for compatibility</li>
 * </ul>
 *
 * @param major the major version number (breaking changes)
 * @param minor the minor version number (backwards-compatible features)
 * @param patch the patch version number (backwards-compatible bug fixes)
 */
public record ModuleVersion(int major, int minor, int patch) implements Comparable<ModuleVersion> {

    public ModuleVersion {
        if (major < 0) {
            throw new IllegalArgumentException("Major version must be non-negative: " + major);
        }
        if (minor < 0) {
            throw new IllegalArgumentException("Minor version must be non-negative: " + minor);
        }
        if (patch < 0) {
            throw new IllegalArgumentException("Patch version must be non-negative: " + patch);
        }
    }

    /**
     * Parse a version string.
     *
     * <p>Accepts formats: "major.minor" or "major.minor.patch"
     *
     * @param version the version string to parse
     * @return the parsed ModuleVersion
     * @throws IllegalArgumentException if the version string is invalid
     */
    public static ModuleVersion parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version string cannot be null or blank");
        }

        String[] parts = version.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException(
                    "Invalid version format: '" + version + "'. Expected major.minor or major.minor.patch");
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
            return new ModuleVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid version format: '" + version + "'. Version components must be integers", e);
        }
    }

    /**
     * Create a version with major and minor only (patch defaults to 0).
     *
     * @param major the major version number
     * @param minor the minor version number
     * @return a new ModuleVersion
     */
    public static ModuleVersion of(int major, int minor) {
        return new ModuleVersion(major, minor, 0);
    }

    /**
     * Create a version with major, minor, and patch.
     *
     * @param major the major version number
     * @param minor the minor version number
     * @param patch the patch version number
     * @return a new ModuleVersion
     */
    public static ModuleVersion of(int major, int minor, int patch) {
        return new ModuleVersion(major, minor, patch);
    }

    /**
     * Check if this version is compatible with a required version.
     *
     * <p>A version is compatible if:
     * <ul>
     *   <li>Major versions match exactly</li>
     *   <li>This minor version is >= the required minor version</li>
     * </ul>
     *
     * @param required the required version
     * @return true if this version satisfies the requirement
     */
    public boolean isCompatibleWith(ModuleVersion required) {
        if (required == null) {
            return true;
        }
        return this.major == required.major && this.minor >= required.minor;
    }

    @Override
    public int compareTo(ModuleVersion other) {
        if (other == null) {
            return 1;
        }
        int majorCmp = Integer.compare(this.major, other.major);
        if (majorCmp != 0) {
            return majorCmp;
        }
        int minorCmp = Integer.compare(this.minor, other.minor);
        if (minorCmp != 0) {
            return minorCmp;
        }
        return Integer.compare(this.patch, other.patch);
    }

    /**
     * Returns the version as a string in format "major.minor.patch" or "major.minor" if patch is 0.
     */
    @Override
    public String toString() {
        if (patch == 0) {
            return major + "." + minor;
        }
        return major + "." + minor + "." + patch;
    }
}
