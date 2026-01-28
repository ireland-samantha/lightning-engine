package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the deleteRigidBody command.
 */
public record DeleteRigidBodyPayload(
        @JsonProperty("entityId") Long entityId
) {
    public boolean hasEntityId() {
        return entityId != null;
    }
}
