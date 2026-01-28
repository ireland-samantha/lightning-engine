package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the attachRigidBody command.
 */
public record AttachRigidBodyPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("positionX") Float positionX,
        @JsonProperty("positionY") Float positionY,
        @JsonProperty("positionZ") Float positionZ,
        @JsonProperty("velocityX") Float velocityX,
        @JsonProperty("velocityY") Float velocityY,
        @JsonProperty("velocityZ") Float velocityZ,
        @JsonProperty("mass") Float mass,
        @JsonProperty("linearDrag") Float linearDrag,
        @JsonProperty("angularDrag") Float angularDrag,
        @JsonProperty("inertia") Float inertia
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

    public float getVelocityX() {
        return velocityX != null ? velocityX : 0f;
    }

    public float getVelocityY() {
        return velocityY != null ? velocityY : 0f;
    }

    public float getVelocityZ() {
        return velocityZ != null ? velocityZ : 0f;
    }

    public float getMass() {
        return mass != null ? mass : 1.0f;
    }

    public float getLinearDrag() {
        return linearDrag != null ? linearDrag : 0f;
    }

    public float getAngularDrag() {
        return angularDrag != null ? angularDrag : 0f;
    }

    public float getInertia() {
        return inertia != null ? inertia : 1.0f;
    }
}
