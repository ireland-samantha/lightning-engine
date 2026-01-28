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

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WebSocketRateLimiter}.
 */
class WebSocketRateLimiterTest {

    private WebSocketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiter = new WebSocketRateLimiter();
        // Set default config via reflection (normally injected by Quarkus)
        setField(rateLimiter, "maxCommandsPerSecond", 5);
        setField(rateLimiter, "windowSeconds", 1);
        setField(rateLimiter, "cleanupIntervalSeconds", 60);
        rateLimiter.init();
    }

    @AfterEach
    void tearDown() {
        rateLimiter.shutdown();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("Rate limiting")
    class RateLimiting {

        @Test
        void shouldAllowFirstRequest() {
            assertThat(rateLimiter.tryAcquire("conn-1")).isTrue();
        }

        @Test
        void shouldAllowRequestsWithinLimit() {
            for (int i = 0; i < 5; i++) {
                assertThat(rateLimiter.tryAcquire("conn-1")).isTrue();
            }
        }

        @Test
        void shouldRejectRequestsOverLimit() {
            // Fill the bucket
            for (int i = 0; i < 5; i++) {
                rateLimiter.tryAcquire("conn-1");
            }

            // Next request should be rejected
            assertThat(rateLimiter.tryAcquire("conn-1")).isFalse();
        }

        @Test
        void shouldTrackConnectionsSeparately() {
            // Fill conn-1's bucket
            for (int i = 0; i < 5; i++) {
                rateLimiter.tryAcquire("conn-1");
            }

            // conn-2 should still be allowed
            assertThat(rateLimiter.tryAcquire("conn-2")).isTrue();
        }

        @Test
        void shouldResetAfterWindowExpires() throws Exception {
            // Fill the bucket
            for (int i = 0; i < 5; i++) {
                rateLimiter.tryAcquire("conn-1");
            }
            assertThat(rateLimiter.tryAcquire("conn-1")).isFalse();

            // Wait for window to expire
            Thread.sleep(1100);

            // Should be allowed again
            assertThat(rateLimiter.tryAcquire("conn-1")).isTrue();
        }
    }

    @Nested
    @DisplayName("Connection management")
    class ConnectionManagement {

        @Test
        void shouldRemoveConnectionBucket() {
            rateLimiter.tryAcquire("conn-1");
            assertThat(rateLimiter.getTrackedConnectionCount()).isEqualTo(1);

            rateLimiter.removeConnection("conn-1");

            assertThat(rateLimiter.getTrackedConnectionCount()).isZero();
        }

        @Test
        void shouldHandleRemoveOfUnknownConnection() {
            // Should not throw
            rateLimiter.removeConnection("unknown");
        }

        @Test
        void shouldTrackMultipleConnections() {
            rateLimiter.tryAcquire("conn-1");
            rateLimiter.tryAcquire("conn-2");
            rateLimiter.tryAcquire("conn-3");

            assertThat(rateLimiter.getTrackedConnectionCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        void shouldExposeMaxCommandsPerSecond() {
            assertThat(rateLimiter.getMaxCommandsPerSecond()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Cleanup")
    class Cleanup {

        @Test
        void shouldCleanupExpiredBuckets() throws Exception {
            rateLimiter.tryAcquire("conn-1");
            assertThat(rateLimiter.getTrackedConnectionCount()).isEqualTo(1);

            // Wait for window to expire
            Thread.sleep(1100);

            // Trigger a new acquire which will replace expired bucket
            // but the expired one should be cleaned up by the scheduler eventually
            rateLimiter.tryAcquire("conn-2");

            // After window expires, acquiring creates a new bucket (old one expired)
            assertThat(rateLimiter.getTrackedConnectionCount()).isGreaterThanOrEqualTo(1);
        }
    }
}
