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

import ca.samanthaireland.lightning.engine.core.snapshot.ComponentData;

import java.util.List;

/**
 * REST API response representing component data within a snapshot.
 *
 * @param name   the component name (e.g., "POSITION_X", "VELOCITY_Y")
 * @param values the component values, one per entity in columnar format
 */
public record ComponentDataResponse(
        String name,
        List<Float> values
) {
    /**
     * Creates a response from domain ComponentData.
     *
     * @param component the domain component data
     * @return the response DTO
     */
    public static ComponentDataResponse from(ComponentData component) {
        return new ComponentDataResponse(component.name(), component.values());
    }
}
