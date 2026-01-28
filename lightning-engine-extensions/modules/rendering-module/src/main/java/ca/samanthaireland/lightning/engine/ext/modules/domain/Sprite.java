package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing a sprite attached to an entity.
 *
 * <p>A sprite defines the visual representation of an entity including
 * resource reference, dimensions, rotation, z-index and visibility.
 */
public record Sprite(
        long entityId,
        long resourceId,
        float width,
        float height,
        float rotation,
        float zIndex,
        boolean visible
) {

    /**
     * Default dimensions for sprites.
     */
    public static final float DEFAULT_WIDTH = 32.0f;
    public static final float DEFAULT_HEIGHT = 32.0f;

    /**
     * Creates a new sprite with validated parameters.
     *
     * @throws IllegalArgumentException if dimensions are not positive
     */
    public Sprite {
        if (width <= 0) {
            throw new IllegalArgumentException("Sprite width must be positive, got: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Sprite height must be positive, got: " + height);
        }
    }

    /**
     * Creates a sprite with default values for optional parameters.
     *
     * @param entityId the entity to attach the sprite to
     * @param resourceId the resource ID for the sprite texture
     * @return a new sprite with default dimensions and visibility
     */
    public static Sprite createDefault(long entityId, long resourceId) {
        return new Sprite(entityId, resourceId, DEFAULT_WIDTH, DEFAULT_HEIGHT, 0.0f, 0.0f, true);
    }

    /**
     * Creates a sprite builder for more flexible construction.
     *
     * @param entityId the entity to attach the sprite to
     * @param resourceId the resource ID for the sprite texture
     * @return a new sprite builder
     */
    public static Builder builder(long entityId, long resourceId) {
        return new Builder(entityId, resourceId);
    }

    /**
     * Builder for creating Sprite instances with optional parameters.
     */
    public static class Builder {
        private final long entityId;
        private final long resourceId;
        private float width = DEFAULT_WIDTH;
        private float height = DEFAULT_HEIGHT;
        private float rotation = 0.0f;
        private float zIndex = 0.0f;
        private boolean visible = true;

        private Builder(long entityId, long resourceId) {
            this.entityId = entityId;
            this.resourceId = resourceId;
        }

        public Builder width(float width) {
            this.width = width;
            return this;
        }

        public Builder height(float height) {
            this.height = height;
            return this;
        }

        public Builder rotation(float rotation) {
            this.rotation = rotation;
            return this;
        }

        public Builder zIndex(float zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public Sprite build() {
            return new Sprite(entityId, resourceId, width, height, rotation, zIndex, visible);
        }
    }
}
