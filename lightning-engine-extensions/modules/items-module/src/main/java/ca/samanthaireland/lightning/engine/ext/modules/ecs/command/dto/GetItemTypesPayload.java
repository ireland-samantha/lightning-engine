package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the getItemTypes command.
 */
public record GetItemTypesPayload(
        @JsonProperty("matchId") Long matchId
) {
    public long getMatchId() {
        return matchId != null ? matchId : 0L;
    }
}
