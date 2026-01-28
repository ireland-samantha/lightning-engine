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

/**
 * Adapter interface for session management operations.
 *
 * <p>Handles player session operations: connect, disconnect, join match.
 */
public interface SessionAdapter {

    /**
     * Connect a player to a match (create session).
     *
     * @param matchId the match ID
     * @param playerId the player ID
     */
    void connectSession(long matchId, long playerId) throws IOException;

    /**
     * Disconnect a player from a match.
     *
     * @param matchId the match ID
     * @param playerId the player ID
     */
    void disconnectSession(long matchId, long playerId) throws IOException;

    /**
     * Join a player to a match.
     *
     * @param matchId the match ID
     * @param playerId the player ID
     */
    void joinMatch(long matchId, long playerId) throws IOException;
}
