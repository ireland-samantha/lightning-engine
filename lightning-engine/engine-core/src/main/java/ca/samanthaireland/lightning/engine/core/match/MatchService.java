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
 * Service interface for Match business operations.
 *
 * <p>Provides business-level operations with validation, authorization,
 * and domain logic. Uses {@link MatchRepository} for persistence.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Handles business logic only, delegates persistence to repository</li>
 *   <li>ISP: Separate from MatchRepository with distinct responsibilities</li>
 *   <li>DIP: Depends on MatchRepository abstraction</li>
 * </ul>
 */
public interface MatchService {

    /**
     * Create a new match with validation.
     *
     * <p>Validates that all enabled modules exist before creating.
     *
     * @param match the match to create (must not be null)
     * @return the created match with server-generated ID
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if any module doesn't exist
     */
    Match createMatch(Match match);

    /**
     * Create a new match with server-generated ID.
     *
     * <p>Validates that all enabled modules exist before creating.
     * The match ID is generated server-side.
     *
     * @param enabledModules the list of module names to enable
     * @return the created match with server-generated ID
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if any module doesn't exist
     */
    default Match createMatch(List<String> enabledModules) {
        return createMatch(new Match(0, enabledModules, List.of()));
    }

    /**
     * Delete a match by ID.
     *
     * @param matchId the match ID
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if match doesn't exist
     */
    void deleteMatch(long matchId);

    /**
     * Get a match by ID.
     *
     * @param matchId the match ID
     * @return an Optional containing the match if found
     */
    Optional<Match> getMatch(long matchId);

    /**
     * Get a match by ID, throwing if not found.
     *
     * @param matchId the match ID
     * @return the match
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if not found
     */
    Match getMatchOrThrow(long matchId);

    /**
     * Get all matches.
     *
     * @return list of all matches
     */
    List<Match> getAllMatches();

    /**
     * Check if a match exists.
     *
     * @param matchId the match ID
     * @return true if the match exists
     */
    boolean matchExists(long matchId);
}
