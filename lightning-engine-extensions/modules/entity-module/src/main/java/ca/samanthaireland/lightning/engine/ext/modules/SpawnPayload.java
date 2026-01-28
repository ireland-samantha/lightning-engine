package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;

import java.util.Map;

/**
 * Payload for spawn commands (used by protobuf adapter).
 */
public record SpawnPayload(
        long matchId,
        long playerId,
        long entityType,
        float positionX,
        float positionY
) implements CommandPayload {
    @Override
    public Map<String, Object> getPayload() {
        return Map.of(
                "matchId", matchId,
                "playerId", playerId,
                "entityType", entityType,
                "positionX", positionX,
                "positionY", positionY
        );
    }
}
