package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the applyForce command.
 */
public record ApplyForcePayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("forceX") Float forceX,
        @JsonProperty("forceY") Float forceY,
        @JsonProperty("forceZ") Float forceZ
) {
    public boolean hasEntityId() {
        return entityId != null;
    }

    public float getForceX() {
        return forceX != null ? forceX : 0f;
    }

    public float getForceY() {
        return forceY != null ? forceY : 0f;
    }

    public float getForceZ() {
        return forceZ != null ? forceZ : 0f;
    }
}
