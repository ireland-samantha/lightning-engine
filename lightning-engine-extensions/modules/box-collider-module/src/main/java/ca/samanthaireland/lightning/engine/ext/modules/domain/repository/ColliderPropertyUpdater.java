package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionHandlerConfig;

/**
 * Interface for updating box collider configuration properties.
 *
 * <p>Separated from BoxColliderRepository to follow Interface Segregation Principle.
 * Use this interface when you only need to update collider properties.
 */
public interface ColliderPropertyUpdater {

    /**
     * Update the size of a box collider.
     *
     * @param entityId the entity ID
     * @param width the new width
     * @param height the new height
     */
    void updateSize(long entityId, float width, float height);

    /**
     * Update the collision layer and mask.
     *
     * @param entityId the entity ID
     * @param layer the new layer
     * @param mask the new mask
     */
    void updateLayerMask(long entityId, int layer, int mask);

    /**
     * Save collision handler configuration.
     *
     * @param entityId the entity ID
     * @param config the handler configuration
     */
    void saveHandlerConfig(long entityId, CollisionHandlerConfig config);
}
