package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the attachCollisionHandler command.
 */
public record AttachCollisionHandlerPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("handlerType") Integer handlerType,
        @JsonProperty("param1") Float param1,
        @JsonProperty("param2") Float param2
) {
    public boolean hasEntityId() {
        return entityId != null && entityId != 0;
    }

    public long getEntityId() {
        return entityId != null ? entityId : 0L;
    }

    public int getHandlerType() {
        return handlerType != null ? handlerType : 0;
    }

    public float getParam1() {
        return param1 != null ? param1 : 0f;
    }

    public float getParam2() {
        return param2 != null ? param2 : 0f;
    }
}
