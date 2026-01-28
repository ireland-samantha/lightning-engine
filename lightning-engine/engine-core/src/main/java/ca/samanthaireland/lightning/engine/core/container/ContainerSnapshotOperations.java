/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;

import java.util.Optional;

/**
 * Fluent API for container-scoped snapshot operations.
 *
 * <p>Snapshots capture the state of entities and their components within a container.
 * Each snapshot is scoped to a specific match, and optionally filtered by player.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Get snapshot for a match
 * Snapshot snapshot = container.snapshots()
 *     .forMatch(matchId)
 *     .create();
 *
 * // Get player-scoped snapshot
 * Snapshot playerSnapshot = container.snapshots()
 *     .forMatch(matchId)
 *     .forPlayer(playerId)
 *     .create();
 * }</pre>
 */
public interface ContainerSnapshotOperations {

    /**
     * Create a snapshot for the specified match.
     *
     * @param matchId the match ID
     * @return the snapshot containing all entity component data for the match
     */
    Snapshot forMatch(long matchId);

    /**
     * Create a snapshot for the specified match filtered by player ownership.
     *
     * @param matchId the match ID
     * @param playerId the player ID to filter by
     * @return the snapshot containing entity component data for entities owned by the player
     */
    Snapshot forMatchAndPlayer(long matchId, long playerId);

    /**
     * Create a snapshot for the specified match with optional player filtering.
     *
     * @param matchId the match ID
     * @param playerId optional player ID to filter by
     * @return the snapshot
     */
    default Snapshot forMatch(long matchId, Optional<Long> playerId) {
        return playerId
                .map(pid -> forMatchAndPlayer(matchId, pid))
                .orElseGet(() -> forMatch(matchId));
    }
}
