package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the setVelocity command.
 */
public record SetVelocityPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("velocityX") Float velocityX,
        @JsonProperty("velocityY") Float velocityY,
        @JsonProperty("velocityZ") Float velocityZ
) {
    public boolean hasEntityId() {
        return entityId != null;
    }

    public float getVelocityX() {
        return velocityX != null ? velocityX : 0f;
    }

    public float getVelocityY() {
        return velocityY != null ? velocityY : 0f;
    }

    public float getVelocityZ() {
        return velocityZ != null ? velocityZ : 0f;
    }
}
