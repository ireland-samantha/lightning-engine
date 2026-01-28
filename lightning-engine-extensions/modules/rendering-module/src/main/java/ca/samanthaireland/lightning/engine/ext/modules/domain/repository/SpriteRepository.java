package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Sprite;

import java.util.Optional;

/**
 * Repository interface for Sprite entities.
 */
public interface SpriteRepository {

    /**
     * Save (attach) a sprite to an entity.
     *
     * @param sprite the sprite to save
     */
    void save(Sprite sprite);

    /**
     * Find a sprite by its entity ID.
     *
     * @param entityId the entity ID
     * @return the sprite if found
     */
    Optional<Sprite> findByEntityId(long entityId);

    /**
     * Check if an entity has a sprite attached.
     *
     * @param entityId the entity ID
     * @return true if the entity has a sprite
     */
    boolean exists(long entityId);
}
