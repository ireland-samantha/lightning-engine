package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the applyImpulse command.
 */
public record ApplyImpulsePayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("impulseX") Float impulseX,
        @JsonProperty("impulseY") Float impulseY,
        @JsonProperty("impulseZ") Float impulseZ
) {
    public boolean hasEntityId() {
        return entityId != null;
    }

    public float getImpulseX() {
        return impulseX != null ? impulseX : 0f;
    }

    public float getImpulseY() {
        return impulseY != null ? impulseY : 0f;
    }

    public float getImpulseZ() {
        return impulseZ != null ? impulseZ : 0f;
    }
}
