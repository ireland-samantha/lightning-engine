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
import java.util.Optional;

/**
 * Service interface for Player business operations.
 *
 * <p>Provides business-level operations with validation, authorization,
 * and domain logic. Uses {@link PlayerRepository} for persistence.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Handles business logic only, delegates persistence to repository</li>
 *   <li>ISP: Separate from PlayerRepository with distinct responsibilities</li>
 *   <li>DIP: Depends on PlayerRepository abstraction</li>
 * </ul>
 */
public interface PlayerService {

    /**
     * Create a new player with validation.
     *
     * @param player the player to create (must not be null)
     * @return the created player
     * @throws IllegalArgumentException if player data is invalid
     */
    Player createPlayer(Player player);

    /**
     * Delete a player by ID.
     *
     * @param playerId the player ID
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if player doesn't exist
     */
    void deletePlayer(long playerId);

    /**
     * Get a player by ID.
     *
     * @param playerId the player ID
     * @return an Optional containing the player if found
     */
    Optional<Player> getPlayer(long playerId);

    /**
     * Get a player by ID, throwing if not found.
     *
     * @param playerId the player ID
     * @return the player
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if not found
     */
    Player getPlayerOrThrow(long playerId);

    /**
     * Get all players.
     *
     * @return list of all players
     */
    List<Player> getAllPlayers();

    /**
     * Check if a player exists.
     *
     * @param playerId the player ID
     * @return true if the player exists
     */
    boolean playerExists(long playerId);
}
