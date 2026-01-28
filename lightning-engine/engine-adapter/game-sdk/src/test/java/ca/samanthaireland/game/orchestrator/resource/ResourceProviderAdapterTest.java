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


package ca.samanthaireland.game.orchestrator.resource;

import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;
import ca.samanthaireland.game.orchestrator.ResourceDownloadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResourceProviderAdapter")
class ResourceProviderAdapterTest {

    private MockContainerScope mockScope;
    private ResourceProviderAdapter provider;

    @BeforeEach
    void setUp() {
        mockScope = new MockContainerScope();
        provider = new ResourceProviderAdapter(mockScope);
    }

    @Nested
    @DisplayName("uploadResource")
    class UploadResource {

        @Test
        @DisplayName("should delegate to ContainerScope")
        void delegatesToScope() throws IOException {
            mockScope.uploadReturnId = 42L;

            long result = provider.uploadResource("test.png", "TEXTURE", new byte[]{1, 2, 3});

            assertThat(result).isEqualTo(42L);
            assertThat(mockScope.uploadedName).isEqualTo("test.png");
            assertThat(mockScope.uploadedType).isEqualTo("TEXTURE");
            assertThat(mockScope.uploadedData).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("should propagate IOException")
        void propagatesIOException() {
            mockScope.throwOnUpload = new IOException("Upload failed");

            assertThatThrownBy(() -> provider.uploadResource("test.png", "TEXTURE", new byte[]{1}))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Upload failed");
        }
    }

    @Nested
    @DisplayName("downloadResource")
    class DownloadResource {

        @Test
        @DisplayName("should delegate to ContainerScope and convert result")
        void delegatesToScopeAndConverts() throws IOException {
            mockScope.resourceResponse = new ContainerAdapter.ResourceResponse(42L, "test.png", "TEXTURE");

            Optional<ResourceProvider.ResourceData> result = provider.downloadResource(42L);

            assertThat(result).isPresent();
            assertThat(result.get().resourceId()).isEqualTo(42L);
            assertThat(result.get().resourceName()).isEqualTo("test.png");
            assertThat(result.get().resourceType()).isEqualTo("TEXTURE");
        }

        @Test
        @DisplayName("should return empty when scope returns empty")
        void returnsEmptyWhenScopeReturnsEmpty() throws IOException {
            mockScope.resourceResponse = null;

            Optional<ResourceProvider.ResourceData> result = provider.downloadResource(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteResource")
    class DeleteResource {

        @Test
        @DisplayName("should delegate to ContainerScope")
        void delegatesToScope() throws IOException {
            provider.deleteResource(42L);

            assertThat(mockScope.deletedResourceId).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("caching operations")
    class CachingOperations {

        @Test
        @DisplayName("ensureResource should be no-op for base adapter")
        void ensureResourceIsNoOp() {
            provider.ensureResource(42L);
        }

        @Test
        @DisplayName("getLocalPath should always return empty")
        void getLocalPathReturnsEmpty() {
            assertThat(provider.getLocalPath(42L)).isEmpty();
        }

        @Test
        @DisplayName("isAvailableLocally should always return false")
        void isAvailableLocallyReturnsFalse() {
            assertThat(provider.isAvailableLocally(42L)).isFalse();
        }

        @Test
        @DisplayName("clearCache should be no-op")
        void clearCacheIsNoOp() {
            provider.clearCache();
        }

        @Test
        @DisplayName("getCacheDirectory should return empty")
        void getCacheDirectoryReturnsEmpty() {
            assertThat(provider.getCacheDirectory()).isEmpty();
        }
    }

    @Nested
    @DisplayName("downloadListener")
    class DownloadListener {

        @Test
        @DisplayName("should store and return download listener")
        void storesDownloadListener() {
            AtomicReference<ResourceDownloadEvent> eventRef = new AtomicReference<>();

            provider.setDownloadListener(eventRef::set);

            assertThat(provider.getDownloadListener()).isNotNull();
        }
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("should be no-op for base adapter")
        void closeIsNoOp() {
            provider.close();
        }
    }

    /**
     * Mock ContainerScope for testing.
     */
    static class MockContainerScope implements ContainerAdapter.ContainerScope {
        long uploadReturnId = 0;
        String uploadedName;
        String uploadedType;
        byte[] uploadedData;
        IOException throwOnUpload;
        ContainerAdapter.ResourceResponse resourceResponse;
        Long deletedResourceId;

        @Override
        public long uploadResource(String name, String type, byte[] data) throws IOException {
            if (throwOnUpload != null) throw throwOnUpload;
            uploadedName = name;
            uploadedType = type;
            uploadedData = data;
            return uploadReturnId;
        }

        @Override
        public Optional<ContainerAdapter.ResourceResponse> getResource(long resourceId) throws IOException {
            return Optional.ofNullable(resourceResponse);
        }

        @Override
        public boolean deleteResource(long resourceId) throws IOException {
            deletedResourceId = resourceId;
            return true;
        }

        @Override
        public List<ContainerAdapter.ResourceResponse> listResources() throws IOException {
            return List.of();
        }

        @Override
        public ContainerAdapter.UploadResourceBuilder upload() {
            return new ContainerAdapter.UploadResourceBuilder() {
                private String name;
                private String type = "TEXTURE";
                private byte[] data;

                @Override public ContainerAdapter.UploadResourceBuilder name(String n) { this.name = n; return this; }
                @Override public ContainerAdapter.UploadResourceBuilder type(String t) { this.type = t; return this; }
                @Override public ContainerAdapter.UploadResourceBuilder data(byte[] d) { this.data = d; return this; }
                @Override public long execute() throws IOException { return uploadResource(name, type, data); }
            };
        }

        // Other required methods with minimal implementations
        @Override public ContainerAdapter.MatchResponse createMatch(List<String> modules) throws IOException { return null; }
        @Override public ContainerAdapter.MatchResponse createMatch(List<String> modules, List<String> ais) throws IOException { return null; }
        @Override public List<ContainerAdapter.MatchResponse> getMatches() throws IOException { return List.of(); }
        @Override public Optional<ContainerAdapter.MatchResponse> getMatch(long matchId) throws IOException { return Optional.empty(); }
        @Override public boolean deleteMatch(long matchId) throws IOException { return false; }
        @Override public void submitCommand(String commandName, Map<String, Object> parameters) throws IOException {}
        @Override public long tick() throws IOException { return 0; }
        @Override public long currentTick() throws IOException { return 0; }
        @Override public void play(int intervalMs) throws IOException {}
        @Override public void stopAuto() throws IOException {}
        @Override public Optional<ContainerAdapter.SnapshotResponse> getSnapshot(long matchId) throws IOException { return Optional.empty(); }
        @Override public ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerCommands.MatchCommands forMatch(long matchId) { return null; }
        @Override public List<String> listModules() throws IOException { return List.of(); }
        @Override public List<String> listAI() throws IOException { return List.of(); }
        @Override public long createPlayer(Long playerId) throws IOException { return 0; }
        @Override public List<Long> listPlayers() throws IOException { return List.of(); }
        @Override public boolean deletePlayer(long playerId) throws IOException { return false; }
        @Override public void connectSession(long matchId, long playerId) throws IOException {}
        @Override public void disconnectSession(long matchId, long playerId) throws IOException {}
        @Override public void joinMatch(long matchId, long playerId) throws IOException {}
        @Override public ca.samanthaireland.lightning.engine.api.resource.adapter.dto.MatchHistorySummaryDto getMatchHistorySummary(long matchId) throws IOException { return null; }
        @Override public List<ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto> getHistorySnapshots(long matchId, ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistoryQueryParams params) throws IOException { return List.of(); }
        @Override public List<ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto> getLatestHistorySnapshots(long matchId, int limit) throws IOException { return List.of(); }
        @Override public Optional<ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto> getHistorySnapshotAtTick(long matchId, long tick) throws IOException { return Optional.empty(); }
    }
}
