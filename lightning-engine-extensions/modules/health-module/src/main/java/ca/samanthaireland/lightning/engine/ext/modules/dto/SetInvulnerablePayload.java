package ca.samanthaireland.lightning.engine.ext.modules.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the setInvulnerable command.
 */
public record SetInvulnerablePayload(
        @JsonProperty("entityId") long entityId,
        @JsonProperty("invulnerable") Float invulnerable
) {
    public boolean isInvulnerable() {
        return invulnerable != null && invulnerable > 0;
    }
}
