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

import ca.samanthaireland.lightning.engine.core.snapshot.ModuleData;

import java.util.List;

/**
 * REST API response representing module data within a snapshot.
 *
 * @param name       the module name (e.g., "EntityModule", "PhysicsModule")
 * @param version    the module version string (e.g., "1.0.0", "0.2.1")
 * @param components the component data for this module
 */
public record ModuleDataResponse(
        String name,
        String version,
        List<ComponentDataResponse> components
) {
    /**
     * Creates a response from domain ModuleData.
     *
     * @param module the domain module data
     * @return the response DTO
     */
    public static ModuleDataResponse from(ModuleData module) {
        List<ComponentDataResponse> components = module.components().stream()
                .map(ComponentDataResponse::from)
                .toList();
        return new ModuleDataResponse(module.name(), module.versionString(), components);
    }
}
