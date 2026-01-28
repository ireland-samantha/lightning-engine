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


package ca.samanthaireland.lightning.engine.core.match;

import java.util.List;

/**
 * Represents a game match within an execution container.
 *
 * @param id              Unique match identifier
 * @param containerId     ID of the container this match belongs to (0 for default container)
 * @param enabledModules  List of module names enabled for this match
 * @param enabledAIs      List of AI names enabled for this match
 */
public record Match(
        long id,
        long containerId,
        List<String> enabledModules,
        List<String> enabledAIs
) {
    /**
     * Creates a match for the default container (container ID 0).
     * Provided for backward compatibility.
     */
    public Match(long id, List<String> enabledModules, List<String> enabledAIs) {
        this(id, 0L, enabledModules, enabledAIs);
    }

    /**
     * Creates a copy of this match with a different container ID.
     */
    public Match withContainerId(long newContainerId) {
        return new Match(id, newContainerId, enabledModules, enabledAIs);
    }

    /**
     * Creates a copy of this match with a different ID.
     */
    public Match withId(long newId) {
        return new Match(newId, containerId, enabledModules, enabledAIs);
    }
}
