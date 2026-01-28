package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the setColliderSize command.
 */
public record SetColliderSizePayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("width") Float width,
        @JsonProperty("height") Float height
) {
    public boolean hasEntityId() {
        return entityId != null && entityId != 0;
    }

    public long getEntityId() {
        return entityId != null ? entityId : 0L;
    }

    public float getWidth() {
        return width != null ? width : 1.0f;
    }

    public float getHeight() {
        return height != null ? height : 1.0f;
    }
}
