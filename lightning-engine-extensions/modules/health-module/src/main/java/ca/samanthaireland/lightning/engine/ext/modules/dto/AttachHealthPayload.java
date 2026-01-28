package ca.samanthaireland.lightning.engine.ext.modules.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the attachHealth command.
 */
public record AttachHealthPayload(
        @JsonProperty("entityId") long entityId,
        @JsonProperty("maxHP") Float maxHP,
        @JsonProperty("currentHP") Float currentHP
) {
    public float getMaxHP() {
        return maxHP != null ? maxHP : 100f;
    }

    public float getCurrentHP() {
        return currentHP != null ? currentHP : getMaxHP();
    }
}
