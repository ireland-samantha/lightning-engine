package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the applyTorque command.
 */
public record ApplyTorquePayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("torque") Float torque
) {
    public boolean hasEntityId() {
        return entityId != null;
    }

    public float getTorque() {
        return torque != null ? torque : 0f;
    }
}
