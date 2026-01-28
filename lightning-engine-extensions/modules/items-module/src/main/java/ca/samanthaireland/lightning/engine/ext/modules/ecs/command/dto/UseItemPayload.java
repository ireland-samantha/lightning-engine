package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the useItem command.
 */
public record UseItemPayload(
        @JsonProperty("itemEntityId") Long itemEntityId,
        @JsonProperty("userEntityId") Long userEntityId
) {
    public long getItemEntityId() {
        return itemEntityId != null ? itemEntityId : 0L;
    }

    public long getUserEntityId() {
        return userEntityId != null ? userEntityId : 0L;
    }

    public boolean hasItemEntityId() {
        return itemEntityId != null;
    }

    public boolean hasUserEntityId() {
        return userEntityId != null;
    }
}
