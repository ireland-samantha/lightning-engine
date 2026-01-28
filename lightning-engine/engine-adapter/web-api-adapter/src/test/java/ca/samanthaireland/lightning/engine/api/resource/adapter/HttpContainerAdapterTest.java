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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("HttpContainerAdapter")
class HttpContainerAdapterTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> stringResponse;

    @Mock
    private HttpResponse<Void> voidResponse;

    private ContainerAdapter.HttpContainerAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new ContainerAdapter.HttpContainerAdapter("http://localhost:8080", httpClient);
    }

    @Nested
    @DisplayName("createContainer")
    class CreateContainer {

        @Test
        @DisplayName("should create container successfully")
        void shouldCreateContainerSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(201);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"test-container\",\"status\":\"CREATED\"}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            ContainerAdapter.ContainerResponse result = adapter.createContainer("test-container");

            assertThat(result.id()).isEqualTo(1);
            assertThat(result.name()).isEqualTo("test-container");
            assertThat(result.status()).isEqualTo("CREATED");
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(400);
            when(stringResponse.body()).thenReturn("Bad request");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.createContainer("invalid"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 400");
        }

        @Test
        @DisplayName("should handle interruption")
        void shouldHandleInterruption() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Interrupted"));

            assertThatThrownBy(() -> adapter.createContainer("container"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("interrupted");
        }
    }

    @Nested
    @DisplayName("create builder")
    class CreateBuilder {

        @Test
        @DisplayName("should create container with builder")
        void shouldCreateContainerWithBuilder() throws Exception {
            when(stringResponse.statusCode()).thenReturn(201);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"game-server\",\"status\":\"CREATED\"}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            ContainerAdapter.ContainerResponse result = adapter.create()
                    .name("game-server")
                    .withModules("EntityModule", "PhysicsModule")
                    .withAIs("BasicAI")
                    .execute();

            assertThat(result.name()).isEqualTo("game-server");
        }

        @Test
        @DisplayName("should throw when name not provided")
        void shouldThrowWhenNameNotProvided() {
            assertThatThrownBy(() -> adapter.create().execute())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("getContainer")
    class GetContainer {

        @Test
        @DisplayName("should return container when found")
        void shouldReturnContainerWhenFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"container-1\",\"status\":\"RUNNING\"}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<ContainerAdapter.ContainerResponse> result = adapter.getContainer(1L);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("container-1");
            assertThat(result.get().status()).isEqualTo("RUNNING");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<ContainerAdapter.ContainerResponse> result = adapter.getContainer(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllContainers")
    class GetAllContainers {

        @Test
        @DisplayName("should return all containers")
        void shouldReturnAllContainers() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "[{\"id\":1,\"name\":\"container-1\",\"status\":\"RUNNING\"}," +
                    "{\"id\":2,\"name\":\"container-2\",\"status\":\"STOPPED\"}]"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            List<ContainerAdapter.ContainerResponse> result = adapter.getAllContainers();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("container-1");
            assertThat(result.get(1).name()).isEqualTo("container-2");
        }

        @Test
        @DisplayName("should return empty list when no containers")
        void shouldReturnEmptyListWhenNoContainers() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn("[]");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            List<ContainerAdapter.ContainerResponse> result = adapter.getAllContainers();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteContainer")
    class DeleteContainer {

        @Test
        @DisplayName("should return true when deleted")
        void shouldReturnTrueWhenDeleted() throws Exception {
            when(voidResponse.statusCode()).thenReturn(204);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(voidResponse);

            boolean result = adapter.deleteContainer(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true when deleted with 200")
        void shouldReturnTrueWhenDeletedWith200() throws Exception {
            when(voidResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(voidResponse);

            boolean result = adapter.deleteContainer(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when not found")
        void shouldReturnFalseWhenNotFound() throws Exception {
            when(voidResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(voidResponse);

            boolean result = adapter.deleteContainer(999L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("startContainer")
    class StartContainer {

        @Test
        @DisplayName("should start container successfully")
        void shouldStartContainerSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"container-1\",\"status\":\"RUNNING\"}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            ContainerAdapter.ContainerResponse result = adapter.startContainer(1L);

            assertThat(result.status()).isEqualTo("RUNNING");
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(400);
            when(stringResponse.body()).thenReturn("Container already running");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.startContainer(1L))
                    .isInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("stopContainer")
    class StopContainer {

        @Test
        @DisplayName("should stop container successfully")
        void shouldStopContainerSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"container-1\",\"status\":\"STOPPED\"}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            ContainerAdapter.ContainerResponse result = adapter.stopContainer(1L);

            assertThat(result.status()).isEqualTo("STOPPED");
        }
    }

    @Nested
    @DisplayName("ContainerScope")
    class ContainerScopeTests {

        private ContainerAdapter.ContainerScope scope;

        @BeforeEach
        void setUp() {
            scope = adapter.forContainer(1L);
        }

        @Nested
        @DisplayName("createMatch")
        class CreateMatch {

            @Test
            @DisplayName("should create match successfully")
            void shouldCreateMatchSuccessfully() throws Exception {
                when(stringResponse.statusCode()).thenReturn(201);
                when(stringResponse.body()).thenReturn(
                        "{\"id\":100,\"modules\":[\"EntityModule\"],\"ais\":[]}"
                );
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                ContainerAdapter.MatchResponse result = scope.createMatch(List.of("EntityModule"));

                assertThat(result.id()).isEqualTo(100);
                assertThat(result.enabledModules()).contains("EntityModule");
            }

            @Test
            @DisplayName("should create match with modules and AIs")
            void shouldCreateMatchWithModulesAndAIs() throws Exception {
                when(stringResponse.statusCode()).thenReturn(201);
                when(stringResponse.body()).thenReturn(
                        "{\"id\":100,\"modules\":[\"EntityModule\"],\"ais\":[\"BasicAI\"]}"
                );
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                ContainerAdapter.MatchResponse result = scope.createMatch(
                        List.of("EntityModule"), List.of("BasicAI"));

                assertThat(result.enabledAIs()).contains("BasicAI");
            }
        }

        @Nested
        @DisplayName("getMatches")
        class GetMatches {

            @Test
            @DisplayName("should return all matches")
            void shouldReturnAllMatches() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn(
                        "[{\"id\":1,\"modules\":[],\"ais\":[]},{\"id\":2,\"modules\":[],\"ais\":[]}]"
                );
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                List<ContainerAdapter.MatchResponse> result = scope.getMatches();

                assertThat(result).hasSize(2);
            }
        }

        @Nested
        @DisplayName("getMatch")
        class GetMatch {

            @Test
            @DisplayName("should return match when found")
            void shouldReturnMatchWhenFound() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn(
                        "{\"id\":100,\"modules\":[\"EntityModule\"],\"ais\":[]}"
                );
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                Optional<ContainerAdapter.MatchResponse> result = scope.getMatch(100L);

                assertThat(result).isPresent();
                assertThat(result.get().id()).isEqualTo(100);
            }

            @Test
            @DisplayName("should return empty when not found")
            void shouldReturnEmptyWhenNotFound() throws Exception {
                when(stringResponse.statusCode()).thenReturn(404);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                Optional<ContainerAdapter.MatchResponse> result = scope.getMatch(999L);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("deleteMatch")
        class DeleteMatch {

            @Test
            @DisplayName("should return true when deleted")
            void shouldReturnTrueWhenDeleted() throws Exception {
                when(voidResponse.statusCode()).thenReturn(204);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(voidResponse);

                boolean result = scope.deleteMatch(100L);

                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("tick operations")
        class TickOperations {

            @Test
            @DisplayName("should advance tick")
            void shouldAdvanceTick() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn("{\"tick\":42}");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                long result = scope.tick();

                assertThat(result).isEqualTo(42);
            }

            @Test
            @DisplayName("should get current tick")
            void shouldGetCurrentTick() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn("{\"tick\":100}");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                long result = scope.currentTick();

                assertThat(result).isEqualTo(100);
            }

            @Test
            @DisplayName("should start auto-play")
            void shouldStartAutoPlay() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                scope.play(16);

                // No exception means success
            }

            @Test
            @DisplayName("should stop auto-play")
            void shouldStopAutoPlay() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                scope.stopAuto();

                // No exception means success
            }
        }

        @Nested
        @DisplayName("submitCommand")
        class SubmitCommand {

            @Test
            @DisplayName("should submit command successfully")
            void shouldSubmitCommandSuccessfully() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                scope.submitCommand("spawn", Map.of("entityType", 100));

                // No exception means success
            }

            @Test
            @DisplayName("should throw IOException on failure")
            void shouldThrowIOExceptionOnFailure() throws Exception {
                when(stringResponse.statusCode()).thenReturn(400);
                when(stringResponse.body()).thenReturn("Invalid command");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                assertThatThrownBy(() -> scope.submitCommand("invalid", Map.of()))
                        .isInstanceOf(IOException.class);
            }
        }

        @Nested
        @DisplayName("getSnapshot")
        class GetSnapshot {

            @Test
            @DisplayName("should return snapshot when found")
            void shouldReturnSnapshotWhenFound() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn(
                        "{\"matchId\":100,\"tick\":42,\"data\":{\"EntityModule\":{}}}"
                );
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                Optional<ContainerAdapter.SnapshotResponse> result = scope.getSnapshot(100L);

                assertThat(result).isPresent();
                assertThat(result.get().matchId()).isEqualTo(100);
                assertThat(result.get().tick()).isEqualTo(42);
            }

            @Test
            @DisplayName("should return empty when not found")
            void shouldReturnEmptyWhenNotFound() throws Exception {
                when(stringResponse.statusCode()).thenReturn(404);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                Optional<ContainerAdapter.SnapshotResponse> result = scope.getSnapshot(999L);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("resource operations")
        class ResourceOperations {

            @Test
            @DisplayName("should list resources")
            void shouldListResources() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn(
                        "[{\"resourceId\":1,\"resourceName\":\"sprite.png\",\"resourceType\":\"TEXTURE\"}]"
                );
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                List<ContainerAdapter.ResourceResponse> result = scope.listResources();

                assertThat(result).hasSize(1);
                assertThat(result.get(0).resourceName()).isEqualTo("sprite.png");
            }

            @Test
            @DisplayName("should get resource by id")
            void shouldGetResourceById() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn(
                        "{\"resourceId\":1,\"resourceName\":\"sprite.png\",\"resourceType\":\"TEXTURE\"}"
                );
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                Optional<ContainerAdapter.ResourceResponse> result = scope.getResource(1L);

                assertThat(result).isPresent();
                assertThat(result.get().resourceType()).isEqualTo("TEXTURE");
            }

            @Test
            @DisplayName("should delete resource")
            void shouldDeleteResource() throws Exception {
                when(voidResponse.statusCode()).thenReturn(204);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(voidResponse);

                boolean result = scope.deleteResource(1L);

                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("should upload resource")
            void shouldUploadResource() throws Exception {
                when(stringResponse.statusCode()).thenReturn(201);
                when(stringResponse.body()).thenReturn("{\"resourceId\":123}");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                long result = scope.uploadResource("test.png", "TEXTURE", new byte[]{1, 2, 3});

                assertThat(result).isEqualTo(123);
            }
        }

        @Nested
        @DisplayName("player operations")
        class PlayerOperations {

            @Test
            @DisplayName("should create player")
            void shouldCreatePlayer() throws Exception {
                when(stringResponse.statusCode()).thenReturn(201);
                when(stringResponse.body()).thenReturn("{\"id\":50}");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                long result = scope.createPlayer(null);

                assertThat(result).isEqualTo(50);
            }

            @Test
            @DisplayName("should create player with specific id")
            void shouldCreatePlayerWithSpecificId() throws Exception {
                when(stringResponse.statusCode()).thenReturn(201);
                when(stringResponse.body()).thenReturn("{\"id\":100}");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                long result = scope.createPlayer(100L);

                assertThat(result).isEqualTo(100);
            }

            @Test
            @DisplayName("should list players")
            void shouldListPlayers() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn("[{\"id\":1},{\"id\":2},{\"id\":3}]");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                List<Long> result = scope.listPlayers();

                assertThat(result).containsExactly(1L, 2L, 3L);
            }

            @Test
            @DisplayName("should delete player")
            void shouldDeletePlayer() throws Exception {
                when(voidResponse.statusCode()).thenReturn(204);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(voidResponse);

                boolean result = scope.deletePlayer(1L);

                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("session operations")
        class SessionOperations {

            @Test
            @DisplayName("should connect session")
            void shouldConnectSession() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                scope.connectSession(100L, 50L);

                // No exception means success
            }

            @Test
            @DisplayName("should disconnect session")
            void shouldDisconnectSession() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                scope.disconnectSession(100L, 50L);

                // No exception means success
            }

            @Test
            @DisplayName("should join match")
            void shouldJoinMatch() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                scope.joinMatch(100L, 50L);

                // No exception means success
            }
        }

        @Nested
        @DisplayName("module and AI operations")
        class ModuleAndAIOperations {

            @Test
            @DisplayName("should list modules")
            void shouldListModules() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn("[\"EntityModule\",\"PhysicsModule\"]");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                List<String> result = scope.listModules();

                assertThat(result).containsExactly("EntityModule", "PhysicsModule");
            }

            @Test
            @DisplayName("should list AI")
            void shouldListAI() throws Exception {
                when(stringResponse.statusCode()).thenReturn(200);
                when(stringResponse.body()).thenReturn("[\"BasicAI\",\"AdvancedAI\"]");
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(stringResponse);

                List<String> result = scope.listAI();

                assertThat(result).containsExactly("BasicAI", "AdvancedAI");
            }
        }
    }

    @Nested
    @DisplayName("UploadResourceBuilder")
    class UploadResourceBuilderTests {

        @Test
        @DisplayName("should upload resource with builder")
        void shouldUploadResourceWithBuilder() throws Exception {
            when(stringResponse.statusCode()).thenReturn(201);
            when(stringResponse.body()).thenReturn("{\"resourceId\":123}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            ContainerAdapter.ContainerScope scope = adapter.forContainer(1L);
            long result = scope.upload()
                    .name("test.png")
                    .type("TEXTURE")
                    .data(new byte[]{1, 2, 3})
                    .execute();

            assertThat(result).isEqualTo(123);
        }

        @Test
        @DisplayName("should throw when name not provided")
        void shouldThrowWhenNameNotProvided() {
            ContainerAdapter.ContainerScope scope = adapter.forContainer(1L);

            assertThatThrownBy(() -> scope.upload()
                    .type("TEXTURE")
                    .data(new byte[]{1, 2, 3})
                    .execute())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("should throw when data not provided")
        void shouldThrowWhenDataNotProvided() {
            ContainerAdapter.ContainerScope scope = adapter.forContainer(1L);

            assertThatThrownBy(() -> scope.upload()
                    .name("test.png")
                    .type("TEXTURE")
                    .execute())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("data is required");
        }
    }

    @Nested
    @DisplayName("URL normalization")
    class UrlNormalization {

        @Test
        @DisplayName("should remove trailing slash from base URL")
        void shouldRemoveTrailingSlashFromBaseUrl() throws Exception {
            ContainerAdapter.HttpContainerAdapter adapterWithTrailingSlash =
                    new ContainerAdapter.HttpContainerAdapter("http://localhost:8080/", httpClient);

            when(stringResponse.statusCode()).thenReturn(201);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"container\",\"status\":\"CREATED\"}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            ContainerAdapter.ContainerResponse result = adapterWithTrailingSlash.createContainer("container");

            assertThat(result.id()).isEqualTo(1);
        }
    }
}
