package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the attachMovement command.
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public record AttachMovementPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("positionX") Long positionX,
        @JsonProperty("positionY") Long positionY,
        @JsonProperty("positionZ") Long positionZ,
        @JsonProperty("velocityX") Long velocityX,
        @JsonProperty("velocityY") Long velocityY,
        @JsonProperty("velocityZ") Long velocityZ
) {
    /**
     * Returns true if entityId was provided.
     */
    public boolean hasEntityId() {
        return entityId != null;
    }

    /**
     * Returns the entityId or 0 if not provided.
     */
    public long entityIdOrDefault() {
        return entityId != null ? entityId : 0L;
    }

    /**
     * Returns the positionX or 0 if not provided.
     */
    public float positionXOrDefault() {
        return positionX != null ? positionX : 0L;
    }

    /**
     * Returns the positionY or 0 if not provided.
     */
    public float positionYOrDefault() {
        return positionY != null ? positionY : 0L;
    }

    /**
     * Returns the positionZ or 0 if not provided.
     */
    public float positionZOrDefault() {
        return positionZ != null ? positionZ : 0L;
    }

    /**
     * Returns the velocityX or 0 if not provided.
     */
    public float velocityXOrDefault() {
        return velocityX != null ? velocityX : 0L;
    }

    /**
     * Returns the velocityY or 0 if not provided.
     */
    public float velocityYOrDefault() {
        return velocityY != null ? velocityY : 0L;
    }

    /**
     * Returns the velocityZ or 0 if not provided.
     */
    public float velocityZOrDefault() {
        return velocityZ != null ? velocityZ : 0L;
    }
}
