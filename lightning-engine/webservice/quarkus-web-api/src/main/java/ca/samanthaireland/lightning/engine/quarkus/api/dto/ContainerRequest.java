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


package ca.samanthaireland.lightning.engine.quarkus.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request DTO for creating or updating an execution container.
 *
 * <p>Supports selecting modules, AIs, and resources to install from the global pool.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContainerRequest(
        @JsonProperty("name") String name,
        @JsonProperty("maxEntities") Integer maxEntities,
        @JsonProperty("maxComponents") Integer maxComponents,
        @JsonProperty("maxCommandsPerTick") Integer maxCommandsPerTick,
        @JsonProperty("maxMemoryMb") Long maxMemoryMb,
        @JsonProperty("moduleJars") List<String> moduleJars,
        @JsonProperty("moduleScanDirectory") String moduleScanDirectory,
        @JsonProperty("moduleNames") List<String> moduleNames,
        @JsonProperty("aiNames") List<String> aiNames,
        @JsonProperty("resourceIds") List<Long> resourceIds
) {
    /**
     * Creates a request with just a name, using defaults for other values.
     */
    public ContainerRequest(String name) {
        this(name, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Returns module names with null-safety.
     */
    public List<String> moduleNames() {
        return moduleNames != null ? moduleNames : List.of();
    }

    /**
     * Returns AI names with null-safety.
     */
    public List<String> aiNames() {
        return aiNames != null ? aiNames : List.of();
    }

    /**
     * Returns resource IDs with null-safety.
     */
    public List<Long> resourceIds() {
        return resourceIds != null ? resourceIds : List.of();
    }
}
