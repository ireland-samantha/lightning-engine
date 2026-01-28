/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ca.samanthaireland.lightning.engine.quarkus.api.websocket;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import ca.samanthaireland.api.proto.CommandProtos;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.ext.modules.SpawnPayload;

/**
 * Converts protobuf and JSON command requests to CommandPayload objects.
 */
@ApplicationScoped
public class CommandPayloadConverter {

    /**
     * Convert a protobuf CommandRequest to CommandPayload.
     */
    public CommandPayload convert(CommandProtos.CommandRequest request) {
        return switch (request.getPayloadCase()) {
            case SPAWN -> convertSpawnPayload(request);
            case ATTACH_RIGID_BODY -> convertAttachRigidBodyPayload(request);
            case ATTACH_SPRITE -> convertAttachSpritePayload(request);
            case GENERIC -> convertGenericPayload(request);
            default -> new ContextCommandPayload(request.getMatchId(), request.getPlayerId());
        };
    }

    /**
     * Convert a JSON command request to CommandPayload.
     */
    public CommandPayload convert(JsonCommandRequest request) {
        if (request.spawn() != null) {
            return new SpawnPayload(
                    request.matchId(),
                    request.playerId(),
                    request.spawn().entityType(),
                    request.spawn().positionX(),
                    request.spawn().positionY()
            );
        }

        if (request.attachRigidBody() != null) {
            var body = request.attachRigidBody();
            return new AttachRigidBodyCommandPayload(
                    request.matchId(),
                    request.playerId(),
                    body.entityId(),
                    body.mass(),
                    body.positionX(),
                    body.positionY(),
                    body.velocityX(),
                    body.velocityY()
            );
        }

        if (request.attachSprite() != null) {
            var sprite = request.attachSprite();
            return new AttachSpriteCommandPayload(
                    request.matchId(),
                    request.playerId(),
                    sprite.entityId(),
                    sprite.resourceId(),
                    sprite.width(),
                    sprite.height(),
                    sprite.visible()
            );
        }

        if (request.generic() != null) {
            var generic = request.generic();
            return new GenericCommandPayload(
                    request.matchId(),
                    request.playerId(),
                    generic.stringParams() != null ? generic.stringParams() : Map.of(),
                    generic.longParams() != null ? generic.longParams() : Map.of(),
                    generic.doubleParams() != null ? generic.doubleParams() : Map.of(),
                    generic.boolParams() != null ? generic.boolParams() : Map.of()
            );
        }

        return new ContextCommandPayload(request.matchId(), request.playerId());
    }

    private CommandPayload convertSpawnPayload(CommandProtos.CommandRequest request) {
        CommandProtos.SpawnPayload spawn = request.getSpawn();
        return new SpawnPayload(
                request.getMatchId(),
                request.getPlayerId(),
                spawn.getEntityType(),
                spawn.getPositionX(),
                spawn.getPositionY()
        );
    }

    private CommandPayload convertAttachRigidBodyPayload(CommandProtos.CommandRequest request) {
        CommandProtos.AttachRigidBodyPayload rigidBody = request.getAttachRigidBody();
        return new AttachRigidBodyCommandPayload(
                request.getMatchId(),
                request.getPlayerId(),
                rigidBody.getEntityId(),
                rigidBody.getMass(),
                rigidBody.getPositionX(),
                rigidBody.getPositionY(),
                rigidBody.getVelocityX(),
                rigidBody.getVelocityY()
        );
    }

    private CommandPayload convertAttachSpritePayload(CommandProtos.CommandRequest request) {
        CommandProtos.AttachSpritePayload sprite = request.getAttachSprite();
        return new AttachSpriteCommandPayload(
                request.getMatchId(),
                request.getPlayerId(),
                sprite.getEntityId(),
                sprite.getResourceId(),
                sprite.getWidth(),
                sprite.getHeight(),
                sprite.getVisible()
        );
    }

    private CommandPayload convertGenericPayload(CommandProtos.CommandRequest request) {
        CommandProtos.GenericPayload generic = request.getGeneric();
        return new GenericCommandPayload(
                request.getMatchId(),
                request.getPlayerId(),
                generic.getStringParamsMap(),
                generic.getLongParamsMap(),
                generic.getDoubleParamsMap(),
                generic.getBoolParamsMap()
        );
    }

    // ---- JSON Request DTOs ----

    /**
     * JSON command request DTO.
     */
    public record JsonCommandRequest(
            String commandName,
            long matchId,
            long playerId,
            JsonSpawnPayload spawn,
            JsonAttachRigidBodyPayload attachRigidBody,
            JsonAttachSpritePayload attachSprite,
            JsonGenericPayload generic
    ) {}

    public record JsonSpawnPayload(long entityType, long positionX, long positionY) {}

    public record JsonAttachRigidBodyPayload(
            long entityId, long mass,
            long positionX, long positionY,
            long velocityX, long velocityY
    ) {}

    public record JsonAttachSpritePayload(
            long entityId, long resourceId,
            long width, long height, boolean visible
    ) {}

    public record JsonGenericPayload(
            Map<String, String> stringParams,
            Map<String, Long> longParams,
            Map<String, Double> doubleParams,
            Map<String, Boolean> boolParams
    ) {}

    // ---- CommandPayload implementations ----

    /**
     * Payload for attachRigidBody commands.
     */
    public record AttachRigidBodyCommandPayload(
            long matchId, long playerId, long entityId, long mass,
            long positionX, long positionY, long velocityX, long velocityY
    ) implements CommandPayload {
        @Override
        public Map<String, Object> getPayload() {
            return Map.of(
                    "matchId", matchId,
                    "playerId", playerId,
                    "entityId", entityId,
                    "mass", mass,
                    "positionX", positionX,
                    "positionY", positionY,
                    "velocityX", velocityX,
                    "velocityY", velocityY
            );
        }
    }

    /**
     * Payload for attachSprite commands.
     */
    public record AttachSpriteCommandPayload(
            long matchId, long playerId, long entityId, long resourceId,
            long width, long height, boolean visible
    ) implements CommandPayload {
        @Override
        public Map<String, Object> getPayload() {
            return Map.of(
                    "matchId", matchId,
                    "playerId", playerId,
                    "entityId", entityId,
                    "resourceId", resourceId,
                    "width", width,
                    "height", height,
                    "visible", visible
            );
        }
    }

    /**
     * Payload for generic commands with arbitrary parameters.
     */
    public record GenericCommandPayload(
            long matchId,
            long playerId,
            Map<String, String> stringParams,
            Map<String, Long> longParams,
            Map<String, Double> doubleParams,
            Map<String, Boolean> boolParams
    ) implements CommandPayload {
        @Override
        public Map<String, Object> getPayload() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId);
            payload.put("playerId", playerId);
            payload.putAll(stringParams);
            longParams.forEach((k, v) -> payload.put(k, v.longValue()));
            doubleParams.forEach((k, v) -> payload.put(k, v.doubleValue()));
            payload.putAll(boolParams);
            return payload;
        }
    }

    /**
     * Payload with just match/player context.
     */
    public record ContextCommandPayload(long matchId, long playerId) implements CommandPayload {
        @Override
        public Map<String, Object> getPayload() {
            return Map.of();
        }
    }
}
