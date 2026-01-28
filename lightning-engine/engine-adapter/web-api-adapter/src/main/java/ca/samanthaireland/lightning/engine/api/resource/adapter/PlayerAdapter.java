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

/**
 * Adapter interface for player management operations.
 *
 * <p>Handles player CRUD operations within a container.
 */
public interface PlayerAdapter {

    /**
     * Create a player in this container.
     *
     * @param playerId optional player ID (auto-generated if null or 0)
     * @return the created player ID
     */
    long createPlayer(Long playerId) throws IOException;

    /**
     * Get all players in this container.
     *
     * @return list of player IDs
     */
    List<Long> listPlayers() throws IOException;

    /**
     * Delete a player from this container.
     *
     * @param playerId the player ID
     * @return true if deleted
     */
    boolean deletePlayer(long playerId) throws IOException;
}
