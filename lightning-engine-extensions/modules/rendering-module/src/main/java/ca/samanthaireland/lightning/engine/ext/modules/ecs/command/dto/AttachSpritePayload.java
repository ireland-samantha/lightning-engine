package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Sprite;

/**
 * Payload DTO for the attachSprite command.
 */
public record AttachSpritePayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("resourceId") Long resourceId,
        @JsonProperty("width") Float width,
        @JsonProperty("height") Float height,
        @JsonProperty("rotation") Float rotation,
        @JsonProperty("zIndex") Float zIndex,
        @JsonProperty("visible") Float visible
) {

    /**
     * Returns true if entityId was explicitly set in the payload.
     */
    public boolean hasEntityId() {
        return entityId != null;
    }

    /**
     * Returns true if resourceId was explicitly set in the payload.
     */
    public boolean hasResourceId() {
        return resourceId != null;
    }

    /**
     * Returns the width with a default value if not specified.
     */
    public float getWidth() {
        return width != null ? width : Sprite.DEFAULT_WIDTH;
    }

    /**
     * Returns the height with a default value if not specified.
     */
    public float getHeight() {
        return height != null ? height : Sprite.DEFAULT_HEIGHT;
    }

    /**
     * Returns the rotation with a default value of 0 if not specified.
     */
    public float getRotation() {
        return rotation != null ? rotation : 0.0f;
    }

    /**
     * Returns the zIndex with a default value of 0 if not specified.
     */
    public float getZIndex() {
        return zIndex != null ? zIndex : 0.0f;
    }

    /**
     * Returns the visible flag with a default value of true if not specified.
     */
    public boolean isVisible() {
        return visible == null || visible != 0.0f;
    }
}
