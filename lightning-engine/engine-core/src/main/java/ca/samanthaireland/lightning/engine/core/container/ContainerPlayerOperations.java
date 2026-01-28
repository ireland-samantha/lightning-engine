/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.core.match.Player;

import java.util.List;
import java.util.Optional;

/**
 * Fluent API for container-scoped player operations.
 *
 * <p>Players are isolated per container. A player created in one container
 * is not visible to other containers.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a player
 * Player player = container.players().create();
 *
 * // Get all players in this container
 * List<Player> all = container.players().all();
 *
 * // Get a specific player
 * container.players().get(playerId);
 * }</pre>
 */
public interface ContainerPlayerOperations {

    /**
     * Create a new player with auto-generated ID.
     *
     * @return the created player
     */
    Player create();

    /**
     * Create a new player with the specified ID.
     *
     * @param playerId the player ID
     * @return the created player
     */
    Player create(long playerId);

    /**
     * Get a player by ID.
     *
     * @param playerId the player ID
     * @return the player if found in this container
     */
    Optional<Player> get(long playerId);

    /**
     * Get all players in this container.
     *
     * @return list of all players in this container
     */
    List<Player> all();

    /**
     * Delete a player from this container.
     *
     * @param playerId the player ID
     * @return true if the player was deleted
     */
    boolean delete(long playerId);

    /**
     * Check if a player exists in this container.
     *
     * @param playerId the player ID
     * @return true if the player exists
     */
    boolean has(long playerId);

    /**
     * Get the count of players in this container.
     *
     * @return the number of players
     */
    int count();
}
