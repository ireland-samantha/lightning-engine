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

package ca.samanthaireland.lightning.engine.internal.core.snapshot;

import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.DirtyTrackingEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CachingSnapshotProvider}.
 */
@DisplayName("CachingSnapshotProvider")
class CachingSnapshotProviderTest {

    private static final EcsProperties PROPERTIES = new EcsProperties(10000, 100);
    private static final long MATCH_ID = 1L;

    private static final BaseComponent TEST_FLAG = new TestComponent(1000, "TEST_FLAG");
    private static final BaseComponent POSITION_X = new TestComponent(1001, "POSITION_X");
    private static final BaseComponent POSITION_Y = new TestComponent(1002, "POSITION_Y");

    private ArrayEntityComponentStore baseStore;
    private DirtyTrackingEntityComponentStore dirtyStore;
    private CachingSnapshotProvider cachingProvider;
    private AtomicLong currentTick;

    @BeforeEach
    void setUp() {
        baseStore = new ArrayEntityComponentStore(PROPERTIES);
        dirtyStore = new DirtyTrackingEntityComponentStore(baseStore);
        currentTick = new AtomicLong(0);

        ModuleResolver moduleResolver = new TestModuleResolver();
        cachingProvider = new CachingSnapshotProvider(
                dirtyStore,
                moduleResolver,
                currentTick::get
        );
    }

    @Nested
    @DisplayName("Cache Behavior")
    class CacheBehavior {

        @Test
        @DisplayName("first request triggers full build")
        void firstRequestTriggerFullBuild() {
            createTestEntity(MATCH_ID);

            Snapshot snapshot = cachingProvider.createForMatch(MATCH_ID);

            assertThat(snapshot).isNotNull();
            assertThat(snapshot.isEmpty()).isFalse();

            SnapshotMetrics metrics = cachingProvider.getMetrics();
            assertThat(metrics.fullRebuilds()).isEqualTo(1);
            assertThat(metrics.cacheHits()).isEqualTo(0);
        }

        @Test
        @DisplayName("returns cached snapshot when no changes")
        void returnsCachedSnapshotWhenNoChanges() {
            createTestEntity(MATCH_ID);

            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();
            Snapshot snapshot2 = cachingProvider.createForMatch(MATCH_ID);

            assertThat(snapshot2).isNotNull();

            SnapshotMetrics metrics = cachingProvider.getMetrics();
            assertThat(metrics.cacheHits()).isEqualTo(1);
        }

        @Test
        @DisplayName("updates cache on changes")
        void updatesCacheOnChanges() {
            long entityId = createTestEntity(MATCH_ID);
            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();

            // Modify entity
            dirtyStore.attachComponent(entityId, POSITION_X, 999.0f);

            Snapshot snapshot = cachingProvider.createForMatch(MATCH_ID);

            assertThat(snapshot).isNotNull();
            SnapshotMetrics metrics = cachingProvider.getMetrics();
            assertThat(metrics.cacheHits()).isEqualTo(0);
            assertThat(metrics.cacheMisses()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Metrics")
    class Metrics {

        @Test
        @DisplayName("tracks total generations")
        void tracksTotalGenerations() {
            createTestEntity(MATCH_ID);

            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();
            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();
            cachingProvider.createForMatch(MATCH_ID);

            SnapshotMetrics metrics = cachingProvider.getMetrics();
            assertThat(metrics.totalGenerations()).isEqualTo(3);
        }

        @Test
        @DisplayName("calculates cache hit rate correctly")
        void calculatesCacheHitRateCorrectly() {
            createTestEntity(MATCH_ID);

            // First call - miss
            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();
            // Second call - hit
            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();
            // Third call - hit
            cachingProvider.createForMatch(MATCH_ID);

            SnapshotMetrics metrics = cachingProvider.getMetrics();
            assertThat(metrics.cacheHitRate()).isCloseTo(0.666, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("resets metrics correctly")
        void resetsMetricsCorrectly() {
            createTestEntity(MATCH_ID);
            cachingProvider.createForMatch(MATCH_ID);

            cachingProvider.resetMetrics();

            SnapshotMetrics metrics = cachingProvider.getMetrics();
            assertThat(metrics.totalGenerations()).isEqualTo(0);
            assertThat(metrics.cacheHits()).isEqualTo(0);
            assertThat(metrics.fullRebuilds()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Dirty Info")
    class DirtyInfoTracking {

        @Test
        @DisplayName("stores last dirty info for delta generation")
        void storesLastDirtyInfo() {
            long entityId = createTestEntity(MATCH_ID);
            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();

            dirtyStore.attachComponent(entityId, POSITION_X, 100.0f);
            cachingProvider.createForMatch(MATCH_ID);

            DirtyInfo lastDirty = cachingProvider.getLastDirtyInfo(MATCH_ID);
            assertThat(lastDirty.modified()).contains(entityId);
        }
    }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagement {

        @Test
        @DisplayName("invalidates cache for specific match")
        void invalidatesCacheForMatch() {
            createTestEntity(MATCH_ID);
            cachingProvider.createForMatch(MATCH_ID);

            cachingProvider.invalidateCache(MATCH_ID);

            assertThat(cachingProvider.getCacheSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("clears all caches")
        void clearsAllCaches() {
            createTestEntity(MATCH_ID);
            createTestEntity(2L);

            cachingProvider.createForMatch(MATCH_ID);
            cachingProvider.createForMatch(2L);

            assertThat(cachingProvider.getCacheSize()).isEqualTo(2);

            cachingProvider.clearCache();

            assertThat(cachingProvider.getCacheSize()).isEqualTo(0);
        }
    }

    private long createTestEntity(long matchId) {
        long entityId = dirtyStore.createEntityForMatch(matchId);
        dirtyStore.attachComponent(entityId, TEST_FLAG, 1.0f);
        dirtyStore.attachComponent(entityId, POSITION_X, 10.0f);
        dirtyStore.attachComponent(entityId, POSITION_Y, 20.0f);
        return entityId;
    }

    private static class TestComponent extends BaseComponent {
        TestComponent(long id, String name) {
            super(id, name);
        }
    }

    private static class TestModuleResolver implements ModuleResolver {
        @Override
        public List<EngineModule> resolveAllModules() {
            return List.of(new TestModule());
        }

        @Override
        public EngineModule resolveModule(String moduleName) {
            return "TestModule".equals(moduleName) ? new TestModule() : null;
        }

        @Override
        public List<String> getAvailableModules() {
            return List.of("TestModule");
        }

        @Override
        public boolean hasModule(String moduleName) {
            return "TestModule".equals(moduleName);
        }
    }

    private static class TestModule implements EngineModule {
        @Override
        public String getName() {
            return "TestModule";
        }

        @Override
        public ModuleVersion getVersion() {
            return ModuleVersion.of(1, 0);
        }

        @Override
        public List<ca.samanthaireland.lightning.engine.core.system.EngineSystem> createSystems() {
            return List.of();
        }

        @Override
        public List<ca.samanthaireland.lightning.engine.core.command.EngineCommand> createCommands() {
            return List.of();
        }

        @Override
        public BaseComponent createFlagComponent() {
            return TEST_FLAG;
        }

        @Override
        public List<BaseComponent> createComponents() {
            return List.of(POSITION_X, POSITION_Y);
        }
    }
}
