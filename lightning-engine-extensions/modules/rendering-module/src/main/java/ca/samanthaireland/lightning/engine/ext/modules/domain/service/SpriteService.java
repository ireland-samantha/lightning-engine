package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Sprite;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.SpriteRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Domain service for sprite operations.
 */
@Slf4j
public class SpriteService {

    private final SpriteRepository spriteRepository;

    public SpriteService(SpriteRepository spriteRepository) {
        this.spriteRepository = spriteRepository;
    }

    /**
     * Attach a sprite to an entity.
     *
     * @param entityId the entity to attach the sprite to
     * @param resourceId the resource ID for the sprite texture
     * @param width sprite width (must be positive)
     * @param height sprite height (must be positive)
     * @param rotation sprite rotation in degrees
     * @param zIndex render order (higher values render on top)
     * @param visible whether the sprite is visible
     * @return the created sprite
     * @throws IllegalArgumentException if dimensions are not positive
     */
    public Sprite attachSprite(long entityId, long resourceId, float width, float height,
                               float rotation, float zIndex, boolean visible) {
        Sprite sprite = new Sprite(entityId, resourceId, width, height, rotation, zIndex, visible);
        spriteRepository.save(sprite);

        log.info("Attached sprite to entity {}: resourceId={}, size=({},{}), rotation={}, zIndex={}, visible={}",
                entityId, resourceId, width, height, rotation, zIndex, visible);

        return sprite;
    }

    /**
     * Attach a sprite to an entity using default values for optional parameters.
     *
     * @param entityId the entity to attach the sprite to
     * @param resourceId the resource ID for the sprite texture
     * @return the created sprite
     */
    public Sprite attachSpriteDefault(long entityId, long resourceId) {
        Sprite sprite = Sprite.createDefault(entityId, resourceId);
        spriteRepository.save(sprite);

        log.info("Attached default sprite to entity {}: resourceId={}", entityId, resourceId);

        return sprite;
    }

    /**
     * Find a sprite by entity ID.
     *
     * @param entityId the entity ID
     * @return the sprite if found
     */
    public Optional<Sprite> findByEntityId(long entityId) {
        return spriteRepository.findByEntityId(entityId);
    }

    /**
     * Check if an entity has a sprite attached.
     *
     * @param entityId the entity ID
     * @return true if the entity has a sprite
     */
    public boolean hasSprite(long entityId) {
        return spriteRepository.exists(entityId);
    }
}
