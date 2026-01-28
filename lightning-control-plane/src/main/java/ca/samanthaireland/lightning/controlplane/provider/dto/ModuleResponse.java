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

package ca.samanthaireland.lightning.controlplane.provider.dto;

import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;

import java.time.Instant;

/**
 * Response containing module information.
 */
public record ModuleResponse(
        String name,
        String version,
        String description,
        String fileName,
        long fileSize,
        String checksum,
        Instant uploadedAt,
        String uploadedBy,
        String downloadUrl
) {

    /**
     * Creates a response from module metadata.
     *
     * @param metadata the module metadata
     * @return the response DTO
     */
    public static ModuleResponse from(ModuleMetadata metadata) {
        return new ModuleResponse(
                metadata.name(),
                metadata.version(),
                metadata.description(),
                metadata.fileName(),
                metadata.fileSize(),
                metadata.checksum(),
                metadata.uploadedAt(),
                metadata.uploadedBy(),
                "/api/modules/" + metadata.name() + "/" + metadata.version() + "/download"
        );
    }
}
