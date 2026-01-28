package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the setPosition command.
 */
public record SetPositionPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("positionX") Float positionX,
        @JsonProperty("positionY") Float positionY,
        @JsonProperty("positionZ") Float positionZ
) {
    public boolean hasEntityId() {
        return entityId != null;
    }

    public float getPositionX() {
        return positionX != null ? positionX : 0f;
    }

    public float getPositionY() {
        return positionY != null ? positionY : 0f;
    }

    public float getPositionZ() {
        return positionZ != null ? positionZ : 0f;
    }
}
