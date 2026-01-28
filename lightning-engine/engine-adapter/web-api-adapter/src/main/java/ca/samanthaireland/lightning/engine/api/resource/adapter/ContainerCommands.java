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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ca.samanthaireland.api.proto.CommandProtos;

/**
 * Command builders for container-scoped game operations.
 *
 * <p>This class contains fluent builder interfaces and implementations for
 * constructing and sending game commands to the Lightning Engine server.
 * Each builder follows the builder pattern for flexible command construction.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Spawn an entity
 * container.forMatch(matchId).spawn()
 *     .forPlayer(1)
 *     .ofType(100)
 *     .execute();
 *
 * // Attach a sprite
 * container.forMatch(matchId).attachSprite()
 *     .toEntity(entityId)
 *     .usingResource(textureId)
 *     .sized(32, 32)
 *     .visible(true)
 *     .execute();
 *
 * // Custom command
 * container.forMatch(matchId).custom("myCommand")
 *     .param("key", value)
 *     .execute();
 * }</pre>
 */
public final class ContainerCommands {

    private ContainerCommands() {
        // Utility class - prevent instantiation
    }

    /**
     * Builder for the spawn command.
     *
     * <p>Creates a new entity for a player with a specific entity type.</p>
     */
    public interface SpawnBuilder {
        /**
         * Set the player who will own the spawned entity.
         *
         * @param playerId the player ID
         * @return this builder for chaining
         */
        SpawnBuilder forPlayer(long playerId);

        /**
         * Set the entity type to spawn.
         *
         * @param entityType the entity type ID
         * @return this builder for chaining
         */
        SpawnBuilder ofType(long entityType);

        /**
         * Execute the spawn command.
         *
         * @throws IOException if the command fails
         */
        void execute() throws IOException;
    }

    /**
     * Builder for the attachMovement command.
     *
     * <p>Attaches position and velocity to an entity for physics simulation.</p>
     */
    public interface AttachMovementBuilder {
        /**
         * Set the target entity.
         *
         * @param entityId the entity ID
         * @return this builder for chaining
         */
        AttachMovementBuilder entity(long entityId);

        /**
         * Set the initial position.
         *
         * @param x X coordinate
         * @param y Y coordinate
         * @param z Z coordinate
         * @return this builder for chaining
         */
        AttachMovementBuilder position(int x, int y, int z);

        /**
         * Set the initial velocity.
         *
         * @param vx X velocity
         * @param vy Y velocity
         * @param vz Z velocity
         * @return this builder for chaining
         */
        AttachMovementBuilder velocity(int vx, int vy, int vz);

        /**
         * Execute the attachMovement command.
         *
         * @throws IOException if the command fails
         */
        void execute() throws IOException;
    }

    /**
     * Builder for the attachSprite command.
     *
     * <p>Attaches a visual sprite to an entity for rendering.</p>
     */
    public interface AttachSpriteBuilder {
        /**
         * Set the target entity.
         *
         * @param entityId the entity ID
         * @return this builder for chaining
         */
        AttachSpriteBuilder toEntity(long entityId);

        /**
         * Set the resource (texture) to use.
         *
         * @param resourceId the resource ID
         * @return this builder for chaining
         */
        AttachSpriteBuilder usingResource(long resourceId);

        /**
         * Set the sprite dimensions.
         *
         * @param width sprite width in pixels
         * @param height sprite height in pixels
         * @return this builder for chaining
         */
        AttachSpriteBuilder sized(int width, int height);

        /**
         * Set sprite visibility.
         *
         * @param visible true to show, false to hide
         * @return this builder for chaining
         */
        AttachSpriteBuilder visible(boolean visible);

        /**
         * Execute the attachSprite command.
         *
         * @throws IOException if the command fails
         */
        void execute() throws IOException;
    }

    /**
     * Builder for custom commands with arbitrary parameters.
     *
     * <p>Allows sending any command name with dynamic parameters.</p>
     */
    public interface CustomCommandBuilder {
        /**
         * Add a parameter to the command.
         *
         * @param name parameter name
         * @param value parameter value (serializable)
         * @return this builder for chaining
         */
        CustomCommandBuilder param(String name, Object value);

        /**
         * Execute the custom command.
         *
         * @throws IOException if the command fails
         */
        void execute() throws IOException;
    }

    /**
     * Interface for sending commands to a match.
     *
     * <p>Provides access to command builders and raw command sending.</p>
     */
    public interface MatchCommands {
        /**
         * Create a spawn command builder.
         *
         * @return a new spawn builder
         */
        SpawnBuilder spawn();

        /**
         * Create an attachMovement command builder.
         *
         * @return a new attachMovement builder
         */
        AttachMovementBuilder attachMovement();

        /**
         * Create an attachSprite command builder.
         *
         * @return a new attachSprite builder
         */
        AttachSpriteBuilder attachSprite();

        /**
         * Create a custom command builder.
         *
         * @param commandName the command name
         * @return a new custom command builder
         */
        CustomCommandBuilder custom(String commandName);

        /**
         * Send a raw command with payload.
         *
         * @param commandName the command name
         * @param payload the command payload
         * @throws IOException if the command fails
         */
        void send(String commandName, Map<String, Object> payload) throws IOException;
    }

    /**
     * Factory for creating command builder implementations.
     *
     * <p>Used internally by the adapter layer.</p>
     */
    public static class HttpMatchCommands implements MatchCommands {
        private final CommandSender sender;
        private final long matchId;

        /**
         * Creates a new HTTP match commands instance.
         *
         * @param sender the command sender interface
         * @param matchId the target match ID
         */
        public HttpMatchCommands(CommandSender sender, long matchId) {
            this.sender = sender;
            this.matchId = matchId;
        }

        @Override
        public SpawnBuilder spawn() {
            return new HttpSpawnBuilder(this);
        }

        @Override
        public AttachMovementBuilder attachMovement() {
            return new HttpAttachMovementBuilder(this);
        }

        @Override
        public AttachSpriteBuilder attachSprite() {
            return new HttpAttachSpriteBuilder(this);
        }

        @Override
        public CustomCommandBuilder custom(String commandName) {
            return new HttpCustomCommandBuilder(this, commandName);
        }

        @Override
        public void send(String commandName, Map<String, Object> payload) throws IOException {
            Map<String, Object> params = new HashMap<>(payload);
            params.put("matchId", matchId);
            sender.submitCommand(commandName, params);
        }

        long matchId() {
            return matchId;
        }
    }

    /**
     * Interface for submitting commands to the server.
     *
     * <p>Implemented by ContainerScope to allow command delegation.</p>
     */
    @FunctionalInterface
    public interface CommandSender {
        /**
         * Submit a command with parameters.
         *
         * @param commandName the command name
         * @param parameters the command parameters
         * @throws IOException if submission fails
         */
        void submitCommand(String commandName, Map<String, Object> parameters) throws IOException;
    }

    /**
     * HTTP spawn builder implementation.
     */
    static class HttpSpawnBuilder implements SpawnBuilder {
        private final HttpMatchCommands commands;
        private long playerId = 1;
        private long entityType = 100;

        HttpSpawnBuilder(HttpMatchCommands commands) {
            this.commands = commands;
        }

        @Override
        public SpawnBuilder forPlayer(long playerId) {
            this.playerId = playerId;
            return this;
        }

        @Override
        public SpawnBuilder ofType(long entityType) {
            this.entityType = entityType;
            return this;
        }

        @Override
        public void execute() throws IOException {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", commands.matchId());
            payload.put("playerId", playerId);
            payload.put("entityType", entityType);
            commands.send("spawn", payload);
        }
    }

    /**
     * HTTP attachMovement builder implementation.
     */
    static class HttpAttachMovementBuilder implements AttachMovementBuilder {
        private final HttpMatchCommands commands;
        private long entityId;
        private int posX, posY, posZ;
        private int velX, velY, velZ;

        HttpAttachMovementBuilder(HttpMatchCommands commands) {
            this.commands = commands;
        }

        @Override
        public AttachMovementBuilder entity(long entityId) {
            this.entityId = entityId;
            return this;
        }

        @Override
        public AttachMovementBuilder position(int x, int y, int z) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            return this;
        }

        @Override
        public AttachMovementBuilder velocity(int vx, int vy, int vz) {
            this.velX = vx;
            this.velY = vy;
            this.velZ = vz;
            return this;
        }

        @Override
        public void execute() throws IOException {
            Map<String, Object> payload = new HashMap<>();
            payload.put("entityId", entityId);
            payload.put("positionX", posX);
            payload.put("positionY", posY);
            payload.put("positionZ", posZ);
            payload.put("velocityX", velX);
            payload.put("velocityY", velY);
            payload.put("velocityZ", velZ);
            commands.send("attachMovement", payload);
        }
    }

    /**
     * HTTP attachSprite builder implementation.
     */
    static class HttpAttachSpriteBuilder implements AttachSpriteBuilder {
        private final HttpMatchCommands commands;
        private long entityId;
        private long resourceId;
        private int width = 32;
        private int height = 32;
        private boolean visible = true;

        HttpAttachSpriteBuilder(HttpMatchCommands commands) {
            this.commands = commands;
        }

        @Override
        public AttachSpriteBuilder toEntity(long entityId) {
            this.entityId = entityId;
            return this;
        }

        @Override
        public AttachSpriteBuilder usingResource(long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        @Override
        public AttachSpriteBuilder sized(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        @Override
        public AttachSpriteBuilder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        @Override
        public void execute() throws IOException {
            Map<String, Object> payload = new HashMap<>();
            payload.put("entityId", entityId);
            payload.put("resourceId", resourceId);
            payload.put("width", width);
            payload.put("height", height);
            payload.put("visible", visible);
            commands.send("attachSprite", payload);
        }
    }

    /**
     * HTTP custom command builder implementation.
     */
    static class HttpCustomCommandBuilder implements CustomCommandBuilder {
        private final HttpMatchCommands commands;
        private final String commandName;
        private final Map<String, Object> params = new HashMap<>();

        HttpCustomCommandBuilder(HttpMatchCommands commands, String commandName) {
            this.commands = commands;
            this.commandName = commandName;
        }

        @Override
        public CustomCommandBuilder param(String name, Object value) {
            params.put(name, value);
            return this;
        }

        @Override
        public void execute() throws IOException {
            commands.send(commandName, params);
        }
    }

    // ==================== WebSocket Implementations ====================

    /**
     * WebSocket-based match commands implementation.
     *
     * <p>Uses CommandWebSocketClient for sending commands via WebSocket
     * instead of HTTP REST API.</p>
     */
    public static class WebSocketMatchCommands implements MatchCommands {
        private final CommandWebSocketClient wsClient;
        private final long matchId;
        private final long defaultPlayerId;

        /**
         * Creates WebSocket match commands.
         *
         * @param wsClient the WebSocket client
         * @param matchId the target match ID
         */
        public WebSocketMatchCommands(CommandWebSocketClient wsClient, long matchId) {
            this(wsClient, matchId, 1);
        }

        /**
         * Creates WebSocket match commands with default player ID.
         *
         * @param wsClient the WebSocket client
         * @param matchId the target match ID
         * @param defaultPlayerId the default player ID for commands
         */
        public WebSocketMatchCommands(CommandWebSocketClient wsClient, long matchId, long defaultPlayerId) {
            this.wsClient = wsClient;
            this.matchId = matchId;
            this.defaultPlayerId = defaultPlayerId;
        }

        @Override
        public SpawnBuilder spawn() {
            return new WebSocketSpawnBuilder(this);
        }

        @Override
        public AttachMovementBuilder attachMovement() {
            return new WebSocketAttachMovementBuilder(this);
        }

        @Override
        public AttachSpriteBuilder attachSprite() {
            return new WebSocketAttachSpriteBuilder(this);
        }

        @Override
        public CustomCommandBuilder custom(String commandName) {
            return new WebSocketCustomCommandBuilder(this, commandName);
        }

        @Override
        public void send(String commandName, Map<String, Object> payload) throws IOException {
            wsClient.sendCommand(commandName, matchId, defaultPlayerId);
        }

        long matchId() {
            return matchId;
        }

        long defaultPlayerId() {
            return defaultPlayerId;
        }

        CommandWebSocketClient wsClient() {
            return wsClient;
        }
    }

    /**
     * WebSocket spawn builder implementation.
     */
    static class WebSocketSpawnBuilder implements SpawnBuilder {
        private final WebSocketMatchCommands commands;
        private long playerId = 1;
        private long entityType = 100;

        WebSocketSpawnBuilder(WebSocketMatchCommands commands) {
            this.commands = commands;
        }

        @Override
        public SpawnBuilder forPlayer(long playerId) {
            this.playerId = playerId;
            return this;
        }

        @Override
        public SpawnBuilder ofType(long entityType) {
            this.entityType = entityType;
            return this;
        }

        @Override
        public void execute() throws IOException {
            commands.wsClient().spawn(commands.matchId(), playerId, entityType, 0, 0);
        }
    }

    /**
     * WebSocket attachMovement builder implementation.
     */
    static class WebSocketAttachMovementBuilder implements AttachMovementBuilder {
        private final WebSocketMatchCommands commands;
        private long entityId;
        private int posX, posY, posZ;
        private int velX, velY, velZ;

        WebSocketAttachMovementBuilder(WebSocketMatchCommands commands) {
            this.commands = commands;
        }

        @Override
        public AttachMovementBuilder entity(long entityId) {
            this.entityId = entityId;
            return this;
        }

        @Override
        public AttachMovementBuilder position(int x, int y, int z) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            return this;
        }

        @Override
        public AttachMovementBuilder velocity(int vx, int vy, int vz) {
            this.velX = vx;
            this.velY = vy;
            this.velZ = vz;
            return this;
        }

        @Override
        public void execute() throws IOException {
            // attachMovement uses attachRigidBody under the hood with mass=1
            commands.wsClient().attachRigidBody(
                    commands.matchId(), commands.defaultPlayerId(),
                    entityId, 1, posX, posY, velX, velY);
        }
    }

    /**
     * WebSocket attachSprite builder implementation.
     */
    static class WebSocketAttachSpriteBuilder implements AttachSpriteBuilder {
        private final WebSocketMatchCommands commands;
        private long entityId;
        private long resourceId;
        private int width = 32;
        private int height = 32;
        private boolean visible = true;

        WebSocketAttachSpriteBuilder(WebSocketMatchCommands commands) {
            this.commands = commands;
        }

        @Override
        public AttachSpriteBuilder toEntity(long entityId) {
            this.entityId = entityId;
            return this;
        }

        @Override
        public AttachSpriteBuilder usingResource(long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        @Override
        public AttachSpriteBuilder sized(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        @Override
        public AttachSpriteBuilder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        @Override
        public void execute() throws IOException {
            commands.wsClient().attachSprite(
                    commands.matchId(), commands.defaultPlayerId(),
                    entityId, resourceId, width, height, visible);
        }
    }

    /**
     * WebSocket custom command builder implementation.
     *
     * <p>Uses GenericPayload to send arbitrary parameters via WebSocket.
     * For typed commands (spawn, attachRigidBody, attachSprite), use the
     * dedicated builders instead for better type safety.
     */
    static class WebSocketCustomCommandBuilder implements CustomCommandBuilder {
        private final WebSocketMatchCommands commands;
        private final String commandName;
        private final Map<String, Object> params = new HashMap<>();

        WebSocketCustomCommandBuilder(WebSocketMatchCommands commands, String commandName) {
            this.commands = commands;
            this.commandName = commandName;
        }

        @Override
        public CustomCommandBuilder param(String name, Object value) {
            params.put(name, value);
            return this;
        }

        @Override
        public void execute() throws IOException {
            CommandProtos.CommandRequest.Builder requestBuilder = CommandProtos.CommandRequest.newBuilder()
                    .setCommandName(commandName)
                    .setMatchId(commands.matchId())
                    .setPlayerId(commands.defaultPlayerId());

            // Build generic payload with all parameters
            CommandProtos.GenericPayload.Builder genericPayload = CommandProtos.GenericPayload.newBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Boolean) {
                    genericPayload.putBoolParams(key, (Boolean) value);
                } else if (value instanceof Number) {
                    Number num = (Number) value;
                    if (value instanceof Float || value instanceof Double) {
                        genericPayload.putDoubleParams(key, num.doubleValue());
                    } else {
                        genericPayload.putLongParams(key, num.longValue());
                    }
                } else if (value != null) {
                    genericPayload.putStringParams(key, value.toString());
                }
            }

            requestBuilder.setGeneric(genericPayload);
            commands.wsClient().send(requestBuilder.build());
        }
    }
}
