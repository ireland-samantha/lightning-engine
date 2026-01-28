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

import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;

import java.util.List;

/**
 * Response DTO for execution container details.
 */
public record ContainerResponse(
        long id,
        String name,
        String status,
        long currentTick,
        boolean autoAdvancing,
        long autoAdvanceIntervalMs,
        int matchCount,
        List<String> loadedModules
) {
    /**
     * Creates a response from an ExecutionContainer.
     */
    public static ContainerResponse from(ExecutionContainer container) {
        return new ContainerResponse(
                container.getId(),
                container.getName(),
                container.getStatus().name(),
                container.ticks().current(),
                container.ticks().isPlaying(),
                container.ticks().interval(),
                container.matches().count(),
                container.modules().available()
        );
    }
}
