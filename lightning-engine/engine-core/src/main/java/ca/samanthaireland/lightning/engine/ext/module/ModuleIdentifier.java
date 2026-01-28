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
 * Uniquely identifies a module with its name and version.
 *
 * <p>Module identifiers are used to specify exact module requirements
 * and for compound module composition.
 *
 * <p>Format: "modulename:version" (e.g., "physics:0.2", "gridmap:0.2.1")
 *
 * @param name the module name
 * @param version the module version
 */
public record ModuleIdentifier(String name, ModuleVersion version) {

    public ModuleIdentifier {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be null or blank");
        }
        if (name.contains(":")) {
            throw new IllegalArgumentException("Module name cannot contain ':': " + name);
        }
        if (version == null) {
            throw new IllegalArgumentException("Module version cannot be null");
        }
    }

    /**
     * Parse a module identifier from a spec string.
     *
     * <p>Format: "modulename:version" (e.g., "physics:0.2")
     *
     * @param spec the spec string to parse
     * @return the parsed ModuleIdentifier
     * @throws IllegalArgumentException if the spec string is invalid
     */
    public static ModuleIdentifier parse(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new IllegalArgumentException("Module spec cannot be null or blank");
        }

        int colonIndex = spec.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException(
                    "Invalid module spec: '" + spec + "'. Expected format: name:version");
        }
        if (colonIndex == 0) {
            throw new IllegalArgumentException(
                    "Invalid module spec: '" + spec + "'. Module name cannot be empty");
        }
        if (colonIndex == spec.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid module spec: '" + spec + "'. Version cannot be empty");
        }

        String name = spec.substring(0, colonIndex);
        String versionStr = spec.substring(colonIndex + 1);

        return new ModuleIdentifier(name, ModuleVersion.parse(versionStr));
    }

    /**
     * Create a module identifier from name and version.
     *
     * @param name the module name
     * @param version the module version
     * @return a new ModuleIdentifier
     */
    public static ModuleIdentifier of(String name, ModuleVersion version) {
        return new ModuleIdentifier(name, version);
    }

    /**
     * Create a module identifier from name and version components.
     *
     * @param name the module name
     * @param major the major version number
     * @param minor the minor version number
     * @return a new ModuleIdentifier
     */
    public static ModuleIdentifier of(String name, int major, int minor) {
        return new ModuleIdentifier(name, ModuleVersion.of(major, minor));
    }

    /**
     * Returns the spec string representation.
     *
     * @return format "name:version" (e.g., "physics:0.2")
     */
    public String toSpec() {
        return name + ":" + version;
    }

    @Override
    public String toString() {
        return toSpec();
    }
}
