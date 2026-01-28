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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.samanthaireland.api.proto.CommandProtos;
import ca.samanthaireland.lightning.engine.ext.modules.SpawnPayload;

/**
 * Unit tests for {@link CommandPayloadConverter}.
 */
class CommandPayloadConverterTest {

    private CommandPayloadConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CommandPayloadConverter();
    }

    @Nested
    @DisplayName("Protobuf conversion")
    class ProtobufConversion {

        @Test
        void shouldConvertSpawnPayload() {
            var request = CommandProtos.CommandRequest.newBuilder()
                    .setCommandName("spawn")
                    .setMatchId(1)
                    .setPlayerId(2)
                    .setSpawn(CommandProtos.SpawnPayload.newBuilder()
                            .setEntityType(100)
                            .setPositionX(10)
                            .setPositionY(20)
                            .build())
                    .build();

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(SpawnPayload.class);
            var spawn = (SpawnPayload) payload;
            assertThat(spawn.matchId()).isEqualTo(1);
            assertThat(spawn.playerId()).isEqualTo(2);
            assertThat(spawn.entityType()).isEqualTo(100);
            assertThat(spawn.positionX()).isEqualTo(10);
            assertThat(spawn.positionY()).isEqualTo(20);
        }

        @Test
        void shouldConvertAttachRigidBodyPayload() {
            var request = CommandProtos.CommandRequest.newBuilder()
                    .setCommandName("attachRigidBody")
                    .setMatchId(1)
                    .setPlayerId(2)
                    .setAttachRigidBody(CommandProtos.AttachRigidBodyPayload.newBuilder()
                            .setEntityId(50)
                            .setMass(100)
                            .setPositionX(10)
                            .setPositionY(20)
                            .setVelocityX(5)
                            .setVelocityY(10)
                            .build())
                    .build();

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.AttachRigidBodyCommandPayload.class);
            var rigidBody = (CommandPayloadConverter.AttachRigidBodyCommandPayload) payload;
            assertThat(rigidBody.matchId()).isEqualTo(1);
            assertThat(rigidBody.playerId()).isEqualTo(2);
            assertThat(rigidBody.entityId()).isEqualTo(50);
            assertThat(rigidBody.mass()).isEqualTo(100);
            assertThat(rigidBody.positionX()).isEqualTo(10);
            assertThat(rigidBody.positionY()).isEqualTo(20);
            assertThat(rigidBody.velocityX()).isEqualTo(5);
            assertThat(rigidBody.velocityY()).isEqualTo(10);
        }

        @Test
        void shouldConvertAttachSpritePayload() {
            var request = CommandProtos.CommandRequest.newBuilder()
                    .setCommandName("attachSprite")
                    .setMatchId(1)
                    .setPlayerId(2)
                    .setAttachSprite(CommandProtos.AttachSpritePayload.newBuilder()
                            .setEntityId(50)
                            .setResourceId(200)
                            .setWidth(64)
                            .setHeight(64)
                            .setVisible(true)
                            .build())
                    .build();

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.AttachSpriteCommandPayload.class);
            var sprite = (CommandPayloadConverter.AttachSpriteCommandPayload) payload;
            assertThat(sprite.matchId()).isEqualTo(1);
            assertThat(sprite.playerId()).isEqualTo(2);
            assertThat(sprite.entityId()).isEqualTo(50);
            assertThat(sprite.resourceId()).isEqualTo(200);
            assertThat(sprite.width()).isEqualTo(64);
            assertThat(sprite.height()).isEqualTo(64);
            assertThat(sprite.visible()).isTrue();
        }

        @Test
        void shouldConvertGenericPayload() {
            var request = CommandProtos.CommandRequest.newBuilder()
                    .setCommandName("custom")
                    .setMatchId(1)
                    .setPlayerId(2)
                    .setGeneric(CommandProtos.GenericPayload.newBuilder()
                            .putStringParams("name", "test")
                            .putLongParams("count", 42)
                            .putDoubleParams("ratio", 3.14)
                            .putBoolParams("enabled", true)
                            .build())
                    .build();

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.GenericCommandPayload.class);
            var generic = (CommandPayloadConverter.GenericCommandPayload) payload;
            assertThat(generic.matchId()).isEqualTo(1);
            assertThat(generic.playerId()).isEqualTo(2);
            assertThat(generic.stringParams()).containsEntry("name", "test");
            assertThat(generic.longParams()).containsEntry("count", 42L);
            assertThat(generic.doubleParams()).containsEntry("ratio", 3.14);
            assertThat(generic.boolParams()).containsEntry("enabled", true);
        }

        @Test
        void shouldReturnContextPayloadForUnknownType() {
            var request = CommandProtos.CommandRequest.newBuilder()
                    .setCommandName("unknown")
                    .setMatchId(1)
                    .setPlayerId(2)
                    .build();

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.ContextCommandPayload.class);
            var context = (CommandPayloadConverter.ContextCommandPayload) payload;
            assertThat(context.matchId()).isEqualTo(1);
            assertThat(context.playerId()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("JSON conversion")
    class JsonConversion {

        @Test
        void shouldConvertJsonSpawnPayload() {
            var request = new CommandPayloadConverter.JsonCommandRequest(
                    "spawn",
                    1, 2,
                    new CommandPayloadConverter.JsonSpawnPayload(100, 10, 20),
                    null, null, null
            );

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(SpawnPayload.class);
            var spawn = (SpawnPayload) payload;
            assertThat(spawn.matchId()).isEqualTo(1);
            assertThat(spawn.playerId()).isEqualTo(2);
            assertThat(spawn.entityType()).isEqualTo(100);
            assertThat(spawn.positionX()).isEqualTo(10);
            assertThat(spawn.positionY()).isEqualTo(20);
        }

        @Test
        void shouldConvertJsonAttachRigidBodyPayload() {
            var request = new CommandPayloadConverter.JsonCommandRequest(
                    "attachRigidBody",
                    1, 2,
                    null,
                    new CommandPayloadConverter.JsonAttachRigidBodyPayload(50, 100, 10, 20, 5, 10),
                    null, null
            );

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.AttachRigidBodyCommandPayload.class);
            var rigidBody = (CommandPayloadConverter.AttachRigidBodyCommandPayload) payload;
            assertThat(rigidBody.entityId()).isEqualTo(50);
            assertThat(rigidBody.mass()).isEqualTo(100);
        }

        @Test
        void shouldConvertJsonAttachSpritePayload() {
            var request = new CommandPayloadConverter.JsonCommandRequest(
                    "attachSprite",
                    1, 2,
                    null, null,
                    new CommandPayloadConverter.JsonAttachSpritePayload(50, 200, 64, 64, true),
                    null
            );

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.AttachSpriteCommandPayload.class);
            var sprite = (CommandPayloadConverter.AttachSpriteCommandPayload) payload;
            assertThat(sprite.entityId()).isEqualTo(50);
            assertThat(sprite.resourceId()).isEqualTo(200);
            assertThat(sprite.visible()).isTrue();
        }

        @Test
        void shouldConvertJsonGenericPayload() {
            var request = new CommandPayloadConverter.JsonCommandRequest(
                    "custom",
                    1, 2,
                    null, null, null,
                    new CommandPayloadConverter.JsonGenericPayload(
                            Map.of("name", "test"),
                            Map.of("count", 42L),
                            Map.of("ratio", 3.14),
                            Map.of("enabled", true)
                    )
            );

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.GenericCommandPayload.class);
            var generic = (CommandPayloadConverter.GenericCommandPayload) payload;
            assertThat(generic.stringParams()).containsEntry("name", "test");
        }

        @Test
        void shouldHandleNullMapsInGenericPayload() {
            var request = new CommandPayloadConverter.JsonCommandRequest(
                    "custom",
                    1, 2,
                    null, null, null,
                    new CommandPayloadConverter.JsonGenericPayload(null, null, null, null)
            );

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.GenericCommandPayload.class);
            var generic = (CommandPayloadConverter.GenericCommandPayload) payload;
            assertThat(generic.stringParams()).isEmpty();
            assertThat(generic.longParams()).isEmpty();
            assertThat(generic.doubleParams()).isEmpty();
            assertThat(generic.boolParams()).isEmpty();
        }

        @Test
        void shouldReturnContextPayloadForEmptyRequest() {
            var request = new CommandPayloadConverter.JsonCommandRequest(
                    "empty",
                    1, 2,
                    null, null, null, null
            );

            var payload = converter.convert(request);

            assertThat(payload).isInstanceOf(CommandPayloadConverter.ContextCommandPayload.class);
        }
    }

    @Nested
    @DisplayName("Payload getPayload()")
    class PayloadGetPayload {

        @Test
        void attachRigidBodyShouldReturnCompletePayload() {
            var payload = new CommandPayloadConverter.AttachRigidBodyCommandPayload(1, 2, 50, 100, 10, 20, 5, 10);

            var map = payload.getPayload();

            assertThat(map).containsEntry("matchId", 1L);
            assertThat(map).containsEntry("playerId", 2L);
            assertThat(map).containsEntry("entityId", 50L);
            assertThat(map).containsEntry("mass", 100L);
            assertThat(map).containsEntry("positionX", 10L);
            assertThat(map).containsEntry("positionY", 20L);
            assertThat(map).containsEntry("velocityX", 5L);
            assertThat(map).containsEntry("velocityY", 10L);
        }

        @Test
        void attachSpriteShouldReturnCompletePayload() {
            var payload = new CommandPayloadConverter.AttachSpriteCommandPayload(1, 2, 50, 200, 64, 64, true);

            var map = payload.getPayload();

            assertThat(map).containsEntry("matchId", 1L);
            assertThat(map).containsEntry("playerId", 2L);
            assertThat(map).containsEntry("entityId", 50L);
            assertThat(map).containsEntry("resourceId", 200L);
            assertThat(map).containsEntry("width", 64L);
            assertThat(map).containsEntry("height", 64L);
            assertThat(map).containsEntry("visible", true);
        }

        @Test
        void genericShouldMergeAllParams() {
            var payload = new CommandPayloadConverter.GenericCommandPayload(
                    1, 2,
                    Map.of("str", "value"),
                    Map.of("lng", 42L),
                    Map.of("dbl", 3.14),
                    Map.of("bool", true)
            );

            var map = payload.getPayload();

            assertThat(map).containsEntry("matchId", 1L);
            assertThat(map).containsEntry("playerId", 2L);
            assertThat(map).containsEntry("str", "value");
            assertThat(map).containsEntry("lng", 42L);
            assertThat(map).containsEntry("dbl", 3.14);
            assertThat(map).containsEntry("bool", true);
        }

        @Test
        void contextShouldReturnEmptyPayload() {
            var payload = new CommandPayloadConverter.ContextCommandPayload(1, 2);

            var map = payload.getPayload();

            assertThat(map).isEmpty();
        }
    }
}
