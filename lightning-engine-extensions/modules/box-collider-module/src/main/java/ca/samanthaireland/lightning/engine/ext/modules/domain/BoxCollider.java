package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing a box collider with dimensions and collision settings.
 *
 * <p>A box collider defines an axis-aligned bounding box (AABB) for collision detection.
 * The collider is positioned relative to an entity's position with an optional offset.
 */
public record BoxCollider(
        long entityId,
        float width,
        float height,
        float depth,
        float offsetX,
        float offsetY,
        float offsetZ,
        int layer,
        int mask,
        boolean isTrigger
) {

    /**
     * Default collision layer (layer 1).
     */
    public static final int DEFAULT_LAYER = 1;

    /**
     * Default collision mask (all layers).
     */
    public static final int DEFAULT_MASK = -1;

    /**
     * Creates a new box collider with validated dimensions.
     *
     * @throws IllegalArgumentException if width, height, or depth is not positive
     */
    public BoxCollider {
        if (width <= 0) {
            throw new IllegalArgumentException("Box collider width must be positive, got: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Box collider height must be positive, got: " + height);
        }
        if (depth <= 0) {
            throw new IllegalArgumentException("Box collider depth must be positive, got: " + depth);
        }
    }

    /**
     * Creates a box collider for a new entity (before ID assignment).
     */
    public static BoxCollider create(float width, float height, float depth,
                                     float offsetX, float offsetY, float offsetZ,
                                     int layer, int mask, boolean isTrigger) {
        return new BoxCollider(0, width, height, depth, offsetX, offsetY, offsetZ, layer, mask, isTrigger);
    }

    /**
     * Creates a simple box collider with default settings.
     */
    public static BoxCollider createSimple(float width, float height, float depth) {
        return new BoxCollider(0, width, height, depth, 0, 0, 0, DEFAULT_LAYER, DEFAULT_MASK, false);
    }

    /**
     * Returns a new BoxCollider with updated dimensions.
     */
    public BoxCollider withSize(float newWidth, float newHeight) {
        return new BoxCollider(entityId, newWidth, newHeight, depth, offsetX, offsetY, offsetZ, layer, mask, isTrigger);
    }

    /**
     * Returns a new BoxCollider with updated layer and mask.
     */
    public BoxCollider withLayerMask(int newLayer, int newMask) {
        return new BoxCollider(entityId, width, height, depth, offsetX, offsetY, offsetZ, newLayer, newMask, isTrigger);
    }

    /**
     * Returns a new BoxCollider with the specified entity ID.
     */
    public BoxCollider withEntityId(long newEntityId) {
        return new BoxCollider(newEntityId, width, height, depth, offsetX, offsetY, offsetZ, layer, mask, isTrigger);
    }
}
