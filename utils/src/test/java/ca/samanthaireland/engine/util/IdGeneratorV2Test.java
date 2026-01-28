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

package ca.samanthaireland.lightning.engine.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IdGeneratorV2}.
 */
@DisplayName("IdGeneratorV2")
class IdGeneratorV2Test {

    @Nested
    @DisplayName("newId")
    class NewId {

        @Test
        @DisplayName("should return positive value")
        void shouldReturnPositiveValue() {
            long id = IdGeneratorV2.newId();

            assertThat(id).isPositive();
        }

        @Test
        @DisplayName("should return different values on consecutive calls")
        void shouldReturnDifferentValuesOnConsecutiveCalls() {
            Set<Long> ids = new HashSet<>();

            for (int i = 0; i < 1000; i++) {
                ids.add(IdGeneratorV2.newId());
            }

            assertThat(ids).hasSize(1000);
        }

        @Test
        @DisplayName("should be thread-safe with concurrent generation")
        void shouldBeThreadSafeWithConcurrentGeneration() throws InterruptedException {
            int threadCount = 10;
            int idsPerThread = 100;
            Set<Long> allIds = ConcurrentHashMap.newKeySet();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < idsPerThread; i++) {
                            allIds.add(IdGeneratorV2.newId());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertThat(allIds).hasSize(threadCount * idsPerThread);
        }

        @Test
        @DisplayName("should return non-negative value")
        void shouldReturnNonNegativeValue() {
            for (int i = 0; i < 10000; i++) {
                long id = IdGeneratorV2.newId();
                assertThat(id).isGreaterThanOrEqualTo(0);
            }
        }
    }
}
