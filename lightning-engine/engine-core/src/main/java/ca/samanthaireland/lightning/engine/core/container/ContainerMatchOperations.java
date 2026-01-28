/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.core.match.Match;

import java.util.List;
import java.util.Optional;

/**
 * Fluent API for match operations within an ExecutionContainer.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a match
 * Match match = container.matches().create(matchConfig);
 *
 * // Get all matches
 * List<Match> all = container.matches().all();
 *
 * // Get specific match
 * Optional<Match> m = container.matches().get(matchId);
 *
 * // Delete match
 * container.matches().delete(matchId);
 * }</pre>
 */
public interface ContainerMatchOperations {

    /**
     * Creates a new match in this container.
     *
     * @param match the match configuration (ID may be 0 for auto-generation)
     * @return the created match with assigned ID and container ID
     */
    Match create(Match match);

    /**
     * Gets a match by ID.
     *
     * @param matchId the match ID
     * @return the match if found, empty otherwise
     */
    Optional<Match> get(long matchId);

    /**
     * Gets all matches in this container.
     *
     * @return list of all matches
     */
    List<Match> all();

    /**
     * Deletes a match from this container.
     *
     * @param matchId the match ID to delete
     * @return this operations instance for chaining
     */
    ContainerMatchOperations delete(long matchId);

    /**
     * Returns the number of matches in this container.
     *
     * @return match count
     */
    default int count() {
        return all().size();
    }
}
