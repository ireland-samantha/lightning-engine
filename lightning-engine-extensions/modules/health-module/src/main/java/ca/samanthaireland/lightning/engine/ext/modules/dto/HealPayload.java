package ca.samanthaireland.lightning.engine.ext.modules.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the heal command.
 */
public record HealPayload(
        @JsonProperty("entityId") long entityId,
        @JsonProperty("amount") Float amount
) {
    public float getAmount() {
        return amount != null ? amount : 0f;
    }
}
