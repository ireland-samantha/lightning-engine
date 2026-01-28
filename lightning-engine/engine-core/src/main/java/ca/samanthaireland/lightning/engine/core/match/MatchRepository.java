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
 * Repository interface for Match persistence operations.
 *
 * <p>Follows the Repository pattern - provides pure CRUD operations
 * without business logic. Business rules belong in {@link MatchService}.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Only handles data persistence, no business logic</li>
 *   <li>ISP: Separate from MatchService with distinct responsibilities</li>
 * </ul>
 */
public interface MatchRepository {

    /**
     * Save a match to the repository.
     *
     * @param match the match to save (must not be null)
     * @return the saved match
     */
    Match save(Match match);

    /**
     * Delete a match by its ID.
     *
     * @param id the match ID
     */
    void deleteById(long id);

    /**
     * Find a match by its ID.
     *
     * @param id the match ID
     * @return an Optional containing the match if found, empty otherwise
     */
    Optional<Match> findById(long id);

    /**
     * Find all matches.
     *
     * @return a list of all matches (never null)
     */
    List<Match> findAll();

    /**
     * Check if a match exists by ID.
     *
     * @param id the match ID
     * @return true if the match exists
     */
    boolean existsById(long id);

    /**
     * Count the number of matches.
     *
     * @return the total count
     */
    long count();
}
