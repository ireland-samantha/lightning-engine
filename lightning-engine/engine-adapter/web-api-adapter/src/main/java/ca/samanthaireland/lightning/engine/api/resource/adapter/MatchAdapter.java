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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Adapter interface for match operations within a container.
 *
 * <p>Handles match CRUD operations scoped to a specific container.
 */
public interface MatchAdapter {

    /**
     * Create a match in this container.
     *
     * @param modules list of module names to enable
     * @return the created match
     */
    ContainerAdapter.MatchResponse createMatch(List<String> modules) throws IOException;

    /**
     * Create a match with modules and AIs in this container.
     *
     * @param modules list of module names to enable
     * @param ais list of AI names to enable
     * @return the created match
     */
    ContainerAdapter.MatchResponse createMatch(List<String> modules, List<String> ais) throws IOException;

    /**
     * Get all matches in this container.
     *
     * @return list of matches
     */
    List<ContainerAdapter.MatchResponse> getMatches() throws IOException;

    /**
     * Get a specific match in this container.
     *
     * @param matchId the match ID
     * @return the match if found
     */
    Optional<ContainerAdapter.MatchResponse> getMatch(long matchId) throws IOException;

    /**
     * Delete a match in this container.
     *
     * @param matchId the match ID
     * @return true if deleted
     */
    boolean deleteMatch(long matchId) throws IOException;
}
