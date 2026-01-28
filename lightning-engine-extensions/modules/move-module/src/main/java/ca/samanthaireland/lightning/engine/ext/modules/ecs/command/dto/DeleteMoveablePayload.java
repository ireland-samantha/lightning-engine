package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the DeleteMoveable command.
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public record DeleteMoveablePayload(
        @JsonProperty("id") Long id
) {
    /**
     * Returns true if id was provided.
     */
    public boolean hasId() {
        return id != null;
    }

    /**
     * Returns the id or 0 if not provided.
     */
    public long idOrDefault() {
        return id != null ? id : 0L;
    }
}
