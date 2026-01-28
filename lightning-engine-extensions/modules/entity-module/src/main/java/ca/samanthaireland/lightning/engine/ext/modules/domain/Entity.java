package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing a game entity with type and player association.
 *
 * <p>An entity represents any game object that can be spawned, positioned,
 * and associated with a player.
 */
public record Entity(long id, long entityType, long playerId) {

    /**
     * Creates an entity without an assigned ID (for creation).
     *
     * @param entityType the type of entity
     * @param playerId the player who owns this entity
     * @return new entity with id 0
     */
    public static Entity create(long entityType, long playerId) {
        return new Entity(0, entityType, playerId);
    }
}
