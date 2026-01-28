package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the deleteBoxCollider command.
 */
public record DeleteBoxColliderPayload(
        @JsonProperty("entityId") Long entityId
) {
    public boolean hasEntityId() {
        return entityId != null && entityId != 0;
    }

    public long getEntityId() {
        return entityId != null ? entityId : 0L;
    }
}
