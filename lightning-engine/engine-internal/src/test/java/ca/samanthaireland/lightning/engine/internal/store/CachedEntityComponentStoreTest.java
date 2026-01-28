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


package ca.samanthaireland.lightning.engine.internal.store;

import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.CachedEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import ca.samanthaireland.lightning.engine.internal.core.store.LockingEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.QueryCache;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CachedEntityComponentStore} decorator.
 */
@DisplayName("CachedEntityComponentStore")
class CachedEntityComponentStoreTest {
    private static final EcsProperties PROPERTIES = new EcsProperties(100000, 100);

    private static final int CARDINALITY = 5;
    private static final long POSITION_X = 0;
    private static final long POSITION_Y = 1;
    private static final long VELOCITY_X = 2;
    private static final long VELOCITY_Y = 3;
    private static final long HEALTH = 4;

    private ArrayEntityComponentStore baseStore;
    private CachedEntityComponentStore cachedStore;
    private QueryCache cache;

    @BeforeEach
    void setUp() {
        baseStore = new ArrayEntityComponentStore(PROPERTIES);
        cache = new QueryCache();
        cachedStore = new CachedEntityComponentStore(baseStore, cache);
    }

    @Nested
    @DisplayName("Delegation")
    class Delegation {

        @Test
        @DisplayName("delegates createEntity to base store")
        void delegatesCreateEntity() {
            cachedStore.createEntity(1);
            assertThat(baseStore.hasComponent(1, POSITION_X)).isFalse();
            cachedStore.attachComponent(1, POSITION_X, 100);
            assertThat(baseStore.getComponent(1, POSITION_X)).isEqualTo(100);
        }

        @Test
        @DisplayName("delegates attachComponent to base store")
        void delegatesAttachComponent() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 42);
            assertThat(baseStore.getComponent(1, POSITION_X)).isEqualTo(42);
        }

        @Test
        @DisplayName("delegates attachComponents to base store")
        void delegatesAttachComponents() {
            cachedStore.createEntity(1);
            cachedStore.attachComponents(1, new long[]{POSITION_X, POSITION_Y}, new float[]{10, 20});
            assertThat(baseStore.getComponent(1, POSITION_X)).isEqualTo(10f);
            assertThat(baseStore.getComponent(1, POSITION_Y)).isEqualTo(20f);
        }

        @Test
        @DisplayName("delegates deleteEntity to base store")
        void delegatesDeleteEntity() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 100);
            cachedStore.deleteEntity(1);
            assertThat(baseStore.isNull(baseStore.getComponent(1, POSITION_X))).isTrue();
        }

        @Test
        @DisplayName("delegates removeComponent to base store")
        void delegatesRemoveComponent() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 100);
            cachedStore.removeComponent(1, POSITION_X);
            assertThat(baseStore.hasComponent(1, POSITION_X)).isFalse();
        }

        @Test
        @DisplayName("delegates getComponent to base store")
        void delegatesGetComponent() {
            baseStore.createEntity(1);
            baseStore.attachComponent(1, POSITION_X, 99);
            assertThat(cachedStore.getComponent(1, POSITION_X)).isEqualTo(99);
        }

        @Test
        @DisplayName("delegates hasComponent to base store")
        void delegatesHasComponent() {
            baseStore.createEntity(1);
            baseStore.attachComponent(1, POSITION_X, 99);
            assertThat(cachedStore.hasComponent(1, POSITION_X)).isTrue();
            assertThat(cachedStore.hasComponent(1, POSITION_Y)).isFalse();
        }

        @Test
        @DisplayName("delegates reset to base store and clears cache")
        void delegatesReset() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 100);

            cachedStore.getEntitiesWithComponents(POSITION_X);
            assertThat(cache.size()).isEqualTo(1);

            cachedStore.reset();

            assertThat(baseStore.hasComponent(1, POSITION_X)).isFalse();
            assertThat(cache.size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Query Caching")
    class QueryCaching {

        @Test
        @DisplayName("caches query results")
        void cachesQueryResults() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.createEntity(2);
            cachedStore.attachComponent(2, POSITION_X, 20);

            Set<Long> result1 = cachedStore.getEntitiesWithComponents(POSITION_X);
            assertThat(cache.getMissCount()).isEqualTo(1);
            assertThat(cache.getHitCount()).isEqualTo(0);

            Set<Long> result2 = cachedStore.getEntitiesWithComponents(POSITION_X);
            assertThat(cache.getMissCount()).isEqualTo(1); // Still 1 - this was a cache hit
            assertThat(cache.getHitCount()).isEqualTo(1);

            assertThat(result1).isEqualTo(result2);
            assertThat(result1).containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("caches different query patterns separately")
        void cachesDifferentQueryPatterns() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.attachComponent(1, POSITION_Y, 20);
            cachedStore.attachComponent(1, VELOCITY_X, 5);

            Set<Long> positionQuery = cachedStore.getEntitiesWithComponents(POSITION_X);
            Set<Long> velocityQuery = cachedStore.getEntitiesWithComponents(VELOCITY_X);
            Set<Long> positionBothQuery = cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);

            assertThat(cache.getMissCount()).isEqualTo(3);
            assertThat(cache.size()).isEqualTo(3);

            cachedStore.getEntitiesWithComponents(POSITION_X);
            cachedStore.getEntitiesWithComponents(VELOCITY_X);
            cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);

            assertThat(cache.getHitCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("query order doesn't affect cache key")
        void queryOrderDoesntAffectCacheKey() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.attachComponent(1, POSITION_Y, 20);

            cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);
            assertThat(cache.getMissCount()).isEqualTo(1);

            cachedStore.getEntitiesWithComponents(POSITION_Y, POSITION_X);
            assertThat(cache.getHitCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Cache Invalidation")
    class CacheInvalidation {

        @Test
        @DisplayName("createEntity invalidates all cached queries")
        void createEntityInvalidatesAllQueries() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);

            cachedStore.getEntitiesWithComponents(POSITION_X);
            assertThat(cache.size()).isEqualTo(1);

            cachedStore.createEntity(2);

            assertThat(cache.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("deleteEntity invalidates all cached queries")
        void deleteEntityInvalidatesAllQueries() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.createEntity(2);
            cachedStore.attachComponent(2, POSITION_Y, 20);

            cachedStore.getEntitiesWithComponents(POSITION_X);
            cachedStore.getEntitiesWithComponents(POSITION_Y);
            assertThat(cache.size()).isEqualTo(2);

            cachedStore.deleteEntity(1);

            assertThat(cache.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("attachComponent invalidates queries containing that component")
        void attachComponentInvalidatesRelevantQueries() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.attachComponent(1, POSITION_Y, 20);
            cachedStore.attachComponent(1, VELOCITY_X, 5);

            cachedStore.getEntitiesWithComponents(POSITION_X);
            cachedStore.getEntitiesWithComponents(VELOCITY_X);
            cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);
            assertThat(cache.size()).isEqualTo(3);

            cachedStore.attachComponent(1, POSITION_X, 100);

            assertThat(cache.size()).isEqualTo(1);

            cache.resetStats();
            cachedStore.getEntitiesWithComponents(VELOCITY_X);
            assertThat(cache.getHitCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("attachComponents invalidates queries containing any of those components")
        void attachComponentsInvalidatesRelevantQueries() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.attachComponent(1, POSITION_Y, 20);
            cachedStore.attachComponent(1, VELOCITY_X, 5);
            cachedStore.attachComponent(1, HEALTH, 100);

            cachedStore.getEntitiesWithComponents(POSITION_X);
            cachedStore.getEntitiesWithComponents(VELOCITY_X);
            cachedStore.getEntitiesWithComponents(HEALTH);
            assertThat(cache.size()).isEqualTo(3);

            cachedStore.attachComponents(1, new long[]{POSITION_X, VELOCITY_X}, new float[]{50, 60});

            assertThat(cache.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("removeComponent invalidates queries containing that component")
        void removeComponentInvalidatesRelevantQueries() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.attachComponent(1, VELOCITY_X, 5);

            cachedStore.getEntitiesWithComponents(POSITION_X);
            cachedStore.getEntitiesWithComponents(VELOCITY_X);
            assertThat(cache.size()).isEqualTo(2);

            cachedStore.removeComponent(1, POSITION_X);

            assertThat(cache.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Correctness")
    class Correctness {

        @Test
        @DisplayName("cached results reflect actual state after invalidation")
        void cachedResultsReflectActualState() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);

            Set<Long> result1 = cachedStore.getEntitiesWithComponents(POSITION_X);
            assertThat(result1).containsExactly(1L);

            cachedStore.createEntity(2);
            cachedStore.attachComponent(2, POSITION_X, 20);

            Set<Long> result2 = cachedStore.getEntitiesWithComponents(POSITION_X);
            assertThat(result2).containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("returns copy of cached result to prevent external modification")
        void returnsCopyOfCachedResult() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);

            Set<Long> result1 = cachedStore.getEntitiesWithComponents(POSITION_X);
            Set<Long> result2 = cachedStore.getEntitiesWithComponents(POSITION_X);

            assertThat(result1).isEqualTo(result2);
        }

        @Test
        @DisplayName("works correctly with multi-component queries")
        void worksWithMultiComponentQueries() {
            cachedStore.createEntity(1);
            cachedStore.attachComponent(1, POSITION_X, 10);
            cachedStore.attachComponent(1, POSITION_Y, 20);

            cachedStore.createEntity(2);
            cachedStore.attachComponent(2, POSITION_X, 30);

            cachedStore.createEntity(3);
            cachedStore.attachComponent(3, POSITION_X, 40);
            cachedStore.attachComponent(3, POSITION_Y, 50);

            Set<Long> result = cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);
            assertThat(result).containsExactlyInAnyOrder(1L, 3L);

            cache.resetStats();
            cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);
            assertThat(cache.getHitCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Accessor Methods")
    class AccessorMethods {

        @Test
        @DisplayName("getCache returns the cache instance")
        void getCacheReturnsCache() {
            assertThat(cachedStore.getCache()).isSameAs(cache);
        }

        @Test
        @DisplayName("getDelegate returns the delegate store")
        void getDelegateReturnsDelegate() {
            assertThat(cachedStore.getDelegate()).isSameAs(baseStore);
        }

        @Test
        @DisplayName("default constructor creates its own cache")
        void defaultConstructorCreatesOwnCache() {
            CachedEntityComponentStore storeWithDefaultCache = new CachedEntityComponentStore(baseStore);
            assertThat(storeWithDefaultCache.getCache()).isNotNull();
            assertThat(storeWithDefaultCache.getCache()).isNotSameAs(cache);
        }
    }

    @Nested
    @DisplayName("Thread Safety (with LockingEntityComponentStore)")
    class ThreadSafety {

        private EntityComponentStore threadSafeStore;

        @BeforeEach
        void setUpThreadSafe() {
            baseStore.reset();
            cache = new QueryCache();
            cachedStore = new CachedEntityComponentStore(baseStore, cache);
            threadSafeStore = LockingEntityComponentStore.wrap(cachedStore);
        }

        @RepeatedTest(5)
        @DisplayName("concurrent reads are safe")
        void concurrentReadsAreSafe() throws Exception {
            // Setup: Create entities with components
            for (int i = 1; i <= 100; i++) {
                threadSafeStore.createEntity(i);
                threadSafeStore.attachComponent(i, POSITION_X, i * 10);
                threadSafeStore.attachComponent(i, POSITION_Y, i * 20);
            }

            int numThreads = 10;
            int queriesPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);
            AtomicBoolean failed = new AtomicBoolean(false);

            for (int t = 0; t < numThreads; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < queriesPerThread; i++) {
                            Set<Long> result = threadSafeStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);
                            if (result.size() != 100) {
                                failed.set(true);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(failed.get()).isFalse();
        }

        @RepeatedTest(5)
        @DisplayName("concurrent reads and writes are safe")
        void concurrentReadsAndWritesAreSafe() throws Exception {
            // Setup: Create initial entities
            for (int i = 1; i <= 50; i++) {
                threadSafeStore.createEntity(i);
                threadSafeStore.attachComponent(i, POSITION_X, i * 10);
            }

            int numReaders = 5;
            int numWriters = 3;
            int operationsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(numReaders + numWriters);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numReaders + numWriters);
            AtomicBoolean failed = new AtomicBoolean(false);
            AtomicInteger nextEntityId = new AtomicInteger(51);

            // Reader threads
            for (int t = 0; t < numReaders; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < operationsPerThread; i++) {
                            Set<Long> result = threadSafeStore.getEntitiesWithComponents(POSITION_X);
                            // Result should never be null
                            if (result == null) {
                                failed.set(true);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Writer threads
            for (int t = 0; t < numWriters; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < operationsPerThread; i++) {
                            int entityId = nextEntityId.getAndIncrement();
                            threadSafeStore.createEntity(entityId);
                            threadSafeStore.attachComponent(entityId, POSITION_X, entityId * 10);
                            threadSafeStore.attachComponent(entityId, POSITION_Y, entityId * 20);
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(failed.get()).isFalse();
        }

        @RepeatedTest(5)
        @DisplayName("cache invalidation is thread-safe")
        void cacheInvalidationIsThreadSafe() throws Exception {
            // Setup
            for (int i = 1; i <= 10; i++) {
                threadSafeStore.createEntity(i);
                threadSafeStore.attachComponent(i, POSITION_X, i * 10);
                threadSafeStore.attachComponent(i, VELOCITY_X, i * 5);
            }

            int numThreads = 6;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);
            AtomicBoolean failed = new AtomicBoolean(false);

            // Mix of query, attach, and remove operations
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < 100; i++) {
                            switch (threadId % 3) {
                                case 0 -> {
                                    // Query
                                    threadSafeStore.getEntitiesWithComponents(POSITION_X);
                                }
                                case 1 -> {
                                    // Attach
                                    threadSafeStore.attachComponent((i % 10) + 1, POSITION_X, i);
                                }
                                case 2 -> {
                                    // Query different component
                                    threadSafeStore.getEntitiesWithComponents(VELOCITY_X);
                                }
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(failed.get()).isFalse();
        }

        @Test
        @DisplayName("reset is thread-safe")
        void resetIsThreadSafe() throws Exception {
            // Setup
            for (int i = 1; i <= 10; i++) {
                threadSafeStore.createEntity(i);
                threadSafeStore.attachComponent(i, POSITION_X, i * 10);
            }

            int numThreads = 4;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);
            AtomicBoolean failed = new AtomicBoolean(false);

            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < 20; i++) {
                            if (threadId == 0 && i % 5 == 0) {
                                threadSafeStore.reset();
                                // Re-create some entities after reset
                                threadSafeStore.createEntity(1);
                                threadSafeStore.attachComponent(1, POSITION_X, 10);
                            } else {
                                threadSafeStore.getEntitiesWithComponents(POSITION_X);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(failed.get()).isFalse();
        }
    }
}
