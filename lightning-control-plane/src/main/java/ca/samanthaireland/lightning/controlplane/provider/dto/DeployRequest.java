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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request to deploy a new game match to the cluster.
 * This is the primary CLI-facing deployment API.
 *
 * @param modules         list of module names required for the game
 * @param preferredNodeId optional preferred node (will be used if available and has capacity)
 * @param autoStart       whether to auto-start the container (default true)
 */
public record DeployRequest(
        @NotNull @NotEmpty List<String> modules,
        String preferredNodeId,
        Boolean autoStart
) {
    /**
     * Creates a request with just modules and default settings.
     *
     * @param modules the required modules
     * @return a new request with auto-start enabled
     */
    public static DeployRequest withModules(List<String> modules) {
        return new DeployRequest(modules, null, true);
    }

    /**
     * Returns whether auto-start is enabled.
     *
     * @return true if auto-start is enabled (defaults to true if null)
     */
    public boolean isAutoStart() {
        return autoStart == null || autoStart;
    }
}
