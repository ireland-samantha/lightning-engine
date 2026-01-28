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
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for {@link CachingSnapshotProvider}.
 *
 * <p>Target: 100,000 entities under 10ms game loop tick time with caching.
 */
@DisplayName("CachingSnapshotProvider Performance")
@Tag("performance")
class CachingSnapshotProviderPerformanceTest {

    private static final long MATCH_ID = 1L;

    // Test components
    private static final BaseComponent TEST_FLAG = new TestComponent(1000, "TEST_FLAG");
    private static final BaseComponent ENTITY_ID = new TestComponent(1001, "ENTITY_ID");
    private static final BaseComponent POSITION_X = new TestComponent(1002, "POSITION_X");
    private static final BaseComponent POSITION_Y = new TestComponent(1003, "POSITION_Y");
    private static final BaseComponent VELOCITY_X = new TestComponent(1004, "VELOCITY_X");
    private static final BaseComponent VELOCITY_Y = new TestComponent(1005, "VELOCITY_Y");
    private static final BaseComponent HEALTH = new TestComponent(1006, "HEALTH");

    private ArrayEntityComponentStore baseStore;
    private DirtyTrackingEntityComponentStore dirtyStore;
    private CachingSnapshotProvider cachingProvider;
    private SnapshotProviderImpl directProvider;
    private AtomicLong currentTick;
    private List<Long> entityIds;

    @BeforeEach
    void setUp() {
        currentTick = new AtomicLong(0);
        entityIds = new ArrayList<>();
    }

    private void initializeWithEntityCount(int entityCount) {
        EcsProperties properties = new EcsProperties(entityCount + 1000, 100);
        baseStore = new ArrayEntityComponentStore(properties);
        dirtyStore = new DirtyTrackingEntityComponentStore(baseStore);

        ModuleResolver moduleResolver = new TestModuleResolver();
        cachingProvider = new CachingSnapshotProvider(
                dirtyStore,
                moduleResolver,
                currentTick::get
        );
        directProvider = new SnapshotProviderImpl(dirtyStore, moduleResolver);

        // Create entities
        for (int i = 0; i < entityCount; i++) {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.attachComponent(entityId, TEST_FLAG, 1.0f);
            dirtyStore.attachComponent(entityId, ENTITY_ID, entityId);
            dirtyStore.attachComponent(entityId, POSITION_X, i * 10.0f);
            dirtyStore.attachComponent(entityId, POSITION_Y, i * 5.0f);
            dirtyStore.attachComponent(entityId, VELOCITY_X, 1.0f);
            dirtyStore.attachComponent(entityId, VELOCITY_Y, 0.5f);
            dirtyStore.attachComponent(entityId, HEALTH, 100.0f);
            entityIds.add(entityId);
        }

        // Warm up - create initial cached snapshot
        cachingProvider.createForMatch(MATCH_ID);
        currentTick.incrementAndGet();
    }

    @Nested
    @DisplayName("10,000 Entities")
    class TenThousandEntities {

        @BeforeEach
        void setUp() {
            initializeWithEntityCount(10_000);
        }

        @Test
        @DisplayName("cache hit should be under 1ms")
        void cacheHitShouldBeUnder1Ms() {
            long start = System.nanoTime();
            Snapshot snapshot = cachingProvider.createForMatch(MATCH_ID);
            long elapsed = System.nanoTime() - start;
            double elapsedMs = elapsed / 1_000_000.0;

            System.out.println("Cache hit (10k entities): " + elapsedMs + "ms");
            assertThat(elapsedMs).isLessThan(1.0);
            assertThat(snapshot.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("incremental update (1% dirty) should be under 5ms")
        void incrementalUpdate1PercentShouldBeUnder5Ms() {
            // Modify 1% of entities
            int dirtyCount = entityIds.size() / 100;
            Random random = new Random(42);
            for (int i = 0; i < dirtyCount; i++) {
                long entityId = entityIds.get(random.nextInt(entityIds.size()));
                dirtyStore.attachComponent(entityId, POSITION_X, random.nextFloat() * 1000);
            }

            long start = System.nanoTime();
            Snapshot snapshot = cachingProvider.createForMatch(MATCH_ID);
            long elapsed = System.nanoTime() - start;
            double elapsedMs = elapsed / 1_000_000.0;

            System.out.println("Incremental update 1% (10k entities): " + elapsedMs + "ms");
            assertThat(elapsedMs).isLessThan(5.0);
            assertThat(snapshot.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("100,000 Entities")
    class OneHundredThousandEntities {

        @BeforeEach
        void setUp() {
            initializeWithEntityCount(100_000);
        }

        @Test
        @DisplayName("cache hit should be under 1ms")
        void cacheHitShouldBeUnder1Ms() {
            long start = System.nanoTime();
            Snapshot snapshot = cachingProvider.createForMatch(MATCH_ID);
            long elapsed = System.nanoTime() - start;
            double elapsedMs = elapsed / 1_000_000.0;

            System.out.println("Cache hit (100k entities): " + elapsedMs + "ms");
            assertThat(elapsedMs).isLessThan(1.0);
            assertThat(snapshot.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("incremental update (0.1% dirty) should be under 10ms")
        void incrementalUpdate01PercentShouldBeUnder10Ms() {
            // Modify 0.1% of entities (100 entities)
            int dirtyCount = entityIds.size() / 1000;
            Random random = new Random(42);
            for (int i = 0; i < dirtyCount; i++) {
                long entityId = entityIds.get(random.nextInt(entityIds.size()));
                dirtyStore.attachComponent(entityId, POSITION_X, random.nextFloat() * 1000);
            }

            long start = System.nanoTime();
            Snapshot snapshot = cachingProvider.createForMatch(MATCH_ID);
            long elapsed = System.nanoTime() - start;
            double elapsedMs = elapsed / 1_000_000.0;

            System.out.println("Incremental update 0.1% (100k entities): " + elapsedMs + "ms");
            assertThat(elapsedMs).isLessThan(10.0);
            assertThat(snapshot.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("incremental update (1% dirty) should be under 15ms")
        void incrementalUpdate1PercentShouldBeUnder15Ms() {
            // Modify 1% of entities (1000 entities)
            int dirtyCount = entityIds.size() / 100;
            Random random = new Random(42);
            for (int i = 0; i < dirtyCount; i++) {
                long entityId = entityIds.get(random.nextInt(entityIds.size()));
                dirtyStore.attachComponent(entityId, POSITION_X, random.nextFloat() * 1000);
            }

            long start = System.nanoTime();
            Snapshot snapshot = cachingProvider.createForMatch(MATCH_ID);
            long elapsed = System.nanoTime() - start;
            double elapsedMs = elapsed / 1_000_000.0;

            System.out.println("Incremental update 1% (100k entities): " + elapsedMs + "ms");
            assertThat(elapsedMs).isLessThan(15.0);
            assertThat(snapshot.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Comparison: Caching vs Direct")
    class CachingVsDirectComparison {

        @BeforeEach
        void setUp() {
            initializeWithEntityCount(10_000);
        }

        @Test
        @DisplayName("caching provider should be faster than direct for cache hits")
        void cachingProviderShouldBeFasterForCacheHits() {
            // Measure caching provider (cache hit)
            long cachingStart = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                cachingProvider.createForMatch(MATCH_ID);
                currentTick.incrementAndGet();
            }
            long cachingElapsed = System.nanoTime() - cachingStart;
            double cachingMs = cachingElapsed / 1_000_000.0;

            // Reset dirty state for fair comparison
            dirtyStore.consumeDirtyInfo(MATCH_ID);

            // Measure direct provider
            long directStart = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                directProvider.createForMatch(MATCH_ID);
            }
            long directElapsed = System.nanoTime() - directStart;
            double directMs = directElapsed / 1_000_000.0;

            System.out.println("Caching provider (10 calls): " + cachingMs + "ms");
            System.out.println("Direct provider (10 calls): " + directMs + "ms");
            System.out.println("Speedup: " + (directMs / cachingMs) + "x");

            // Caching should be significantly faster
            assertThat(cachingMs).isLessThan(directMs);
        }
    }

    @Nested
    @DisplayName("Metrics Accuracy")
    class MetricsAccuracy {

        @BeforeEach
        void setUp() {
            initializeWithEntityCount(1_000);
        }

        @Test
        @DisplayName("metrics reflect actual cache behavior")
        void metricsReflectActualCacheBehavior() {
            // First call - cache miss
            cachingProvider.createForMatch(MATCH_ID);
            currentTick.incrementAndGet();

            // Next 5 calls - cache hits
            for (int i = 0; i < 5; i++) {
                cachingProvider.createForMatch(MATCH_ID);
                currentTick.incrementAndGet();
            }

            // Modify some entities
            for (int i = 0; i < 10; i++) {
                dirtyStore.attachComponent(entityIds.get(i), POSITION_X, 999.0f);
            }

            // Next call - cache miss (incremental)
            cachingProvider.createForMatch(MATCH_ID);

            SnapshotMetrics metrics = cachingProvider.getMetrics();

            System.out.println("Metrics: " + metrics);
            assertThat(metrics.totalGenerations()).isEqualTo(7);
            assertThat(metrics.cacheHits()).isEqualTo(5);
            assertThat(metrics.cacheMisses()).isEqualTo(2);
            assertThat(metrics.cacheHitRate()).isCloseTo(5.0 / 7.0, org.assertj.core.data.Offset.offset(0.01));
        }
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
            return List.of(ENTITY_ID, POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y, HEALTH);
        }
    }
}
