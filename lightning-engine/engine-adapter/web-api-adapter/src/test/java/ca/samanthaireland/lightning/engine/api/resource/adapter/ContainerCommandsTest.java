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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ca.samanthaireland.api.proto.CommandProtos;

/**
 * Unit tests for ContainerCommands builder pattern implementations.
 */
class ContainerCommandsTest {

    @Nested
    @DisplayName("HttpMatchCommands")
    class HttpMatchCommandsTests {

        private ContainerCommands.CommandSender mockSender;
        private ContainerCommands.HttpMatchCommands matchCommands;
        private static final long MATCH_ID = 42L;

        @BeforeEach
        void setUp() {
            mockSender = mock(ContainerCommands.CommandSender.class);
            matchCommands = new ContainerCommands.HttpMatchCommands(mockSender, MATCH_ID);
        }

        @Nested
        @DisplayName("SpawnBuilder")
        class SpawnBuilderTests {

            @Test
            @DisplayName("should build spawn command with default values")
            void shouldBuildSpawnWithDefaults() throws IOException {
                matchCommands.spawn().execute();

                ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(cmdCaptor.capture(), paramsCaptor.capture());

                assertThat(cmdCaptor.getValue()).isEqualTo("spawn");
                Map<String, Object> params = paramsCaptor.getValue();
                assertThat(params.get("matchId")).isEqualTo(MATCH_ID);
                assertThat(params.get("playerId")).isEqualTo(1L);
                assertThat(params.get("entityType")).isEqualTo(100L);
            }

            @Test
            @DisplayName("should build spawn command with custom player and type")
            void shouldBuildSpawnWithCustomValues() throws IOException {
                matchCommands.spawn()
                        .forPlayer(5)
                        .ofType(200)
                        .execute();

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(eq("spawn"), paramsCaptor.capture());

                Map<String, Object> params = paramsCaptor.getValue();
                assertThat(params.get("playerId")).isEqualTo(5L);
                assertThat(params.get("entityType")).isEqualTo(200L);
            }

            @Test
            @DisplayName("should support fluent chaining")
            void shouldSupportFluentChaining() throws IOException {
                ContainerCommands.SpawnBuilder builder = matchCommands.spawn();
                assertThat(builder.forPlayer(1)).isSameAs(builder);
                assertThat(builder.ofType(100)).isSameAs(builder);
            }
        }

        @Nested
        @DisplayName("AttachMovementBuilder")
        class AttachMovementBuilderTests {

            @Test
            @DisplayName("should build attachMovement command with all parameters")
            void shouldBuildAttachMovementWithAllParams() throws IOException {
                matchCommands.attachMovement()
                        .entity(10)
                        .position(100, 200, 300)
                        .velocity(5, -10, 0)
                        .execute();

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(eq("attachMovement"), paramsCaptor.capture());

                Map<String, Object> params = paramsCaptor.getValue();
                assertThat(params.get("entityId")).isEqualTo(10L);
                assertThat(params.get("positionX")).isEqualTo(100);
                assertThat(params.get("positionY")).isEqualTo(200);
                assertThat(params.get("positionZ")).isEqualTo(300);
                assertThat(params.get("velocityX")).isEqualTo(5);
                assertThat(params.get("velocityY")).isEqualTo(-10);
                assertThat(params.get("velocityZ")).isEqualTo(0);
            }

            @Test
            @DisplayName("should support fluent chaining")
            void shouldSupportFluentChaining() throws IOException {
                ContainerCommands.AttachMovementBuilder builder = matchCommands.attachMovement();
                assertThat(builder.entity(1)).isSameAs(builder);
                assertThat(builder.position(0, 0, 0)).isSameAs(builder);
                assertThat(builder.velocity(0, 0, 0)).isSameAs(builder);
            }
        }

        @Nested
        @DisplayName("AttachSpriteBuilder")
        class AttachSpriteBuilderTests {

            @Test
            @DisplayName("should build attachSprite command with default values")
            void shouldBuildAttachSpriteWithDefaults() throws IOException {
                matchCommands.attachSprite()
                        .toEntity(15)
                        .usingResource(99)
                        .execute();

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(eq("attachSprite"), paramsCaptor.capture());

                Map<String, Object> params = paramsCaptor.getValue();
                assertThat(params.get("entityId")).isEqualTo(15L);
                assertThat(params.get("resourceId")).isEqualTo(99L);
                assertThat(params.get("width")).isEqualTo(32);
                assertThat(params.get("height")).isEqualTo(32);
                assertThat(params.get("visible")).isEqualTo(true);
            }

            @Test
            @DisplayName("should build attachSprite command with custom dimensions and visibility")
            void shouldBuildAttachSpriteWithCustomValues() throws IOException {
                matchCommands.attachSprite()
                        .toEntity(15)
                        .usingResource(99)
                        .sized(64, 128)
                        .visible(false)
                        .execute();

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(eq("attachSprite"), paramsCaptor.capture());

                Map<String, Object> params = paramsCaptor.getValue();
                assertThat(params.get("width")).isEqualTo(64);
                assertThat(params.get("height")).isEqualTo(128);
                assertThat(params.get("visible")).isEqualTo(false);
            }

            @Test
            @DisplayName("should support fluent chaining")
            void shouldSupportFluentChaining() {
                ContainerCommands.AttachSpriteBuilder builder = matchCommands.attachSprite();
                assertThat(builder.toEntity(1)).isSameAs(builder);
                assertThat(builder.usingResource(1)).isSameAs(builder);
                assertThat(builder.sized(32, 32)).isSameAs(builder);
                assertThat(builder.visible(true)).isSameAs(builder);
            }
        }

        @Nested
        @DisplayName("CustomCommandBuilder")
        class CustomCommandBuilderTests {

            @Test
            @DisplayName("should build custom command with arbitrary parameters")
            void shouldBuildCustomCommandWithParams() throws IOException {
                matchCommands.custom("myCustomCommand")
                        .param("stringParam", "hello")
                        .param("intParam", 42)
                        .param("boolParam", true)
                        .execute();

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(eq("myCustomCommand"), paramsCaptor.capture());

                Map<String, Object> params = paramsCaptor.getValue();
                assertThat(params.get("stringParam")).isEqualTo("hello");
                assertThat(params.get("intParam")).isEqualTo(42);
                assertThat(params.get("boolParam")).isEqualTo(true);
            }

            @Test
            @DisplayName("should override parameters when set multiple times")
            void shouldOverrideParams() throws IOException {
                matchCommands.custom("test")
                        .param("key", "first")
                        .param("key", "second")
                        .execute();

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(anyString(), paramsCaptor.capture());

                assertThat(paramsCaptor.getValue().get("key")).isEqualTo("second");
            }

            @Test
            @DisplayName("should support fluent chaining")
            void shouldSupportFluentChaining() {
                ContainerCommands.CustomCommandBuilder builder = matchCommands.custom("test");
                assertThat(builder.param("a", 1)).isSameAs(builder);
                assertThat(builder.param("b", 2)).isSameAs(builder);
            }
        }

        @Nested
        @DisplayName("send method")
        class SendMethodTests {

            @Test
            @DisplayName("should send raw command with payload and match ID")
            void shouldSendRawCommand() throws IOException {
                Map<String, Object> payload = Map.of("foo", "bar", "num", 123);
                matchCommands.send("rawCommand", payload);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockSender).submitCommand(eq("rawCommand"), paramsCaptor.capture());

                Map<String, Object> params = paramsCaptor.getValue();
                assertThat(params.get("foo")).isEqualTo("bar");
                assertThat(params.get("num")).isEqualTo(123);
                assertThat(params.get("matchId")).isEqualTo(MATCH_ID);
            }
        }
    }

    @Nested
    @DisplayName("WebSocketMatchCommands")
    class WebSocketMatchCommandsTests {

        private CommandWebSocketClient mockWsClient;
        private ContainerCommands.WebSocketMatchCommands matchCommands;
        private static final long MATCH_ID = 42L;
        private static final long DEFAULT_PLAYER_ID = 1L;

        @BeforeEach
        void setUp() {
            mockWsClient = mock(CommandWebSocketClient.class);
            matchCommands = new ContainerCommands.WebSocketMatchCommands(mockWsClient, MATCH_ID);
        }

        @Nested
        @DisplayName("SpawnBuilder")
        class SpawnBuilderTests {

            @Test
            @DisplayName("should call wsClient.spawn with correct parameters")
            void shouldCallWsClientSpawn() throws IOException {
                matchCommands.spawn()
                        .forPlayer(5)
                        .ofType(200)
                        .execute();

                verify(mockWsClient).spawn(MATCH_ID, 5, 200, 0, 0);
            }

            @Test
            @DisplayName("should use default values when not specified")
            void shouldUseDefaults() throws IOException {
                matchCommands.spawn().execute();

                verify(mockWsClient).spawn(MATCH_ID, 1, 100, 0, 0);
            }
        }

        @Nested
        @DisplayName("AttachMovementBuilder")
        class AttachMovementBuilderTests {

            @Test
            @DisplayName("should call wsClient.attachRigidBody with correct parameters")
            void shouldCallWsClientAttachRigidBody() throws IOException {
                matchCommands.attachMovement()
                        .entity(10)
                        .position(100, 200, 0)
                        .velocity(5, -10, 0)
                        .execute();

                verify(mockWsClient).attachRigidBody(MATCH_ID, DEFAULT_PLAYER_ID, 10, 1, 100, 200, 5, -10);
            }
        }

        @Nested
        @DisplayName("AttachSpriteBuilder")
        class AttachSpriteBuilderTests {

            @Test
            @DisplayName("should call wsClient.attachSprite with correct parameters")
            void shouldCallWsClientAttachSprite() throws IOException {
                matchCommands.attachSprite()
                        .toEntity(15)
                        .usingResource(99)
                        .sized(64, 128)
                        .visible(false)
                        .execute();

                verify(mockWsClient).attachSprite(MATCH_ID, DEFAULT_PLAYER_ID, 15, 99, 64, 128, false);
            }

            @Test
            @DisplayName("should use default dimensions and visibility")
            void shouldUseDefaults() throws IOException {
                matchCommands.attachSprite()
                        .toEntity(1)
                        .usingResource(1)
                        .execute();

                verify(mockWsClient).attachSprite(MATCH_ID, DEFAULT_PLAYER_ID, 1, 1, 32, 32, true);
            }
        }

        @Nested
        @DisplayName("CustomCommandBuilder")
        class CustomCommandBuilderTests {

            @Test
            @DisplayName("should convert boolean parameters correctly")
            void shouldConvertBooleanParams() throws IOException {
                when(mockWsClient.send(any(CommandProtos.CommandRequest.class)))
                        .thenReturn(CommandProtos.CommandResponse.getDefaultInstance());

                matchCommands.custom("test")
                        .param("flag", true)
                        .execute();

                ArgumentCaptor<CommandProtos.CommandRequest> captor =
                        ArgumentCaptor.forClass(CommandProtos.CommandRequest.class);
                verify(mockWsClient).send(captor.capture());

                CommandProtos.CommandRequest request = captor.getValue();
                assertThat(request.getCommandName()).isEqualTo("test");
                assertThat(request.getMatchId()).isEqualTo(MATCH_ID);
                assertThat(request.getGeneric().getBoolParamsMap().get("flag")).isTrue();
            }

            @Test
            @DisplayName("should convert integer parameters to long")
            void shouldConvertIntegerParams() throws IOException {
                when(mockWsClient.send(any(CommandProtos.CommandRequest.class)))
                        .thenReturn(CommandProtos.CommandResponse.getDefaultInstance());

                matchCommands.custom("test")
                        .param("count", 42)
                        .execute();

                ArgumentCaptor<CommandProtos.CommandRequest> captor =
                        ArgumentCaptor.forClass(CommandProtos.CommandRequest.class);
                verify(mockWsClient).send(captor.capture());

                assertThat(captor.getValue().getGeneric().getLongParamsMap().get("count")).isEqualTo(42L);
            }

            @Test
            @DisplayName("should convert float parameters to double")
            void shouldConvertFloatParams() throws IOException {
                when(mockWsClient.send(any(CommandProtos.CommandRequest.class)))
                        .thenReturn(CommandProtos.CommandResponse.getDefaultInstance());

                matchCommands.custom("test")
                        .param("ratio", 3.14f)
                        .execute();

                ArgumentCaptor<CommandProtos.CommandRequest> captor =
                        ArgumentCaptor.forClass(CommandProtos.CommandRequest.class);
                verify(mockWsClient).send(captor.capture());

                assertThat(captor.getValue().getGeneric().getDoubleParamsMap().get("ratio"))
                        .isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.01));
            }

            @Test
            @DisplayName("should convert objects to string")
            void shouldConvertObjectsToString() throws IOException {
                when(mockWsClient.send(any(CommandProtos.CommandRequest.class)))
                        .thenReturn(CommandProtos.CommandResponse.getDefaultInstance());

                matchCommands.custom("test")
                        .param("name", "test-value")
                        .execute();

                ArgumentCaptor<CommandProtos.CommandRequest> captor =
                        ArgumentCaptor.forClass(CommandProtos.CommandRequest.class);
                verify(mockWsClient).send(captor.capture());

                assertThat(captor.getValue().getGeneric().getStringParamsMap().get("name"))
                        .isEqualTo("test-value");
            }
        }

        @Nested
        @DisplayName("Constructor with custom player ID")
        class CustomPlayerIdTests {

            @Test
            @DisplayName("should use custom default player ID")
            void shouldUseCustomPlayerId() throws IOException {
                ContainerCommands.WebSocketMatchCommands customCommands =
                        new ContainerCommands.WebSocketMatchCommands(mockWsClient, MATCH_ID, 99L);

                customCommands.spawn().execute();

                verify(mockWsClient).spawn(MATCH_ID, 1, 100, 0, 0);
            }
        }
    }
}
