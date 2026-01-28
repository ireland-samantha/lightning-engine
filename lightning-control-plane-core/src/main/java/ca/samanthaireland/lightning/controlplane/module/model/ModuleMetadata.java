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

package ca.samanthaireland.lightning.controlplane.module.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata for a module stored in the module registry.
 *
 * @param name        unique name of the module
 * @param version     semantic version string
 * @param description human-readable description
 * @param fileName    original filename of the JAR
 * @param fileSize    size in bytes
 * @param checksum    SHA-256 checksum of the JAR file
 * @param uploadedAt  when the module was uploaded
 * @param uploadedBy  identifier of who uploaded the module
 */
public record ModuleMetadata(
        String name,
        String version,
        String description,
        String fileName,
        long fileSize,
        String checksum,
        Instant uploadedAt,
        String uploadedBy
) {

    public ModuleMetadata {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        Objects.requireNonNull(fileName, "fileName cannot be null");
        Objects.requireNonNull(checksum, "checksum cannot be null");
        Objects.requireNonNull(uploadedAt, "uploadedAt cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (version.isBlank()) {
            throw new IllegalArgumentException("version cannot be blank");
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize cannot be negative");
        }
    }

    /**
     * Creates metadata for a newly uploaded module.
     *
     * @param name        module name
     * @param version     module version
     * @param description module description
     * @param fileName    original filename
     * @param fileSize    file size in bytes
     * @param checksum    SHA-256 checksum
     * @param uploadedBy  uploader identifier
     * @return new metadata instance
     */
    public static ModuleMetadata create(
            String name,
            String version,
            String description,
            String fileName,
            long fileSize,
            String checksum,
            String uploadedBy
    ) {
        return new ModuleMetadata(
                name,
                version,
                description != null ? description : "",
                fileName,
                fileSize,
                checksum,
                Instant.now(),
                uploadedBy != null ? uploadedBy : "system"
        );
    }

    /**
     * Gets the unique identifier for this module (name:version).
     *
     * @return the module identifier
     */
    public String id() {
        return name + ":" + version;
    }
}
