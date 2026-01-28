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
 * Repository interface for Player persistence operations.
 *
 * <p>Follows the Repository pattern - provides pure CRUD operations
 * without business logic. Business rules belong in {@link PlayerService}.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Only handles data persistence, no business logic</li>
 *   <li>ISP: Separate from PlayerService with distinct responsibilities</li>
 * </ul>
 */
public interface PlayerRepository {

    /**
     * Save a player to the repository.
     *
     * @param player the player to save (must not be null)
     * @return the saved player
     */
    Player save(Player player);

    /**
     * Delete a player by its ID.
     *
     * @param id the player ID
     */
    void deleteById(long id);

    /**
     * Find a player by its ID.
     *
     * @param id the player ID
     * @return an Optional containing the player if found, empty otherwise
     */
    Optional<Player> findById(long id);

    /**
     * Find all players.
     *
     * @return a list of all players (never null)
     */
    List<Player> findAll();

    /**
     * Check if a player exists by ID.
     *
     * @param id the player ID
     * @return true if the player exists
     */
    boolean existsById(long id);

    /**
     * Count the number of players.
     *
     * @return the total count
     */
    long count();
}
