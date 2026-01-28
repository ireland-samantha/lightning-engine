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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WebSocketConnectionLimiter}.
 */
class WebSocketConnectionLimiterTest {

    private WebSocketConnectionLimiter limiter;

    @BeforeEach
    void setUp() throws Exception {
        limiter = new WebSocketConnectionLimiter();
        // Set default limits via reflection (normally injected by Quarkus)
        setField(limiter, "maxConnectionsPerUser", 3);
        setField(limiter, "maxConnectionsPerContainer", 5);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("Acquiring connections")
    class AcquiringConnections {

        @Test
        void shouldAllowFirstConnection() {
            var result = limiter.tryAcquire("conn-1", "user-1", 1);

            assertThat(result).isInstanceOf(WebSocketConnectionLimiter.AcquireResult.Success.class);
        }

        @Test
        void shouldAllowMultipleConnectionsWithinUserLimit() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-1", 1);
            var result = limiter.tryAcquire("conn-3", "user-1", 1);

            assertThat(result).isInstanceOf(WebSocketConnectionLimiter.AcquireResult.Success.class);
        }

        @Test
        void shouldRejectWhenUserLimitExceeded() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-1", 1);
            limiter.tryAcquire("conn-3", "user-1", 1);

            var result = limiter.tryAcquire("conn-4", "user-1", 1);

            assertThat(result).isInstanceOf(WebSocketConnectionLimiter.AcquireResult.UserLimitExceeded.class);
            var exceeded = (WebSocketConnectionLimiter.AcquireResult.UserLimitExceeded) result;
            assertThat(exceeded.currentCount()).isEqualTo(3);
            assertThat(exceeded.maxAllowed()).isEqualTo(3);
        }

        @Test
        void shouldRejectWhenContainerLimitExceeded() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-2", 1);
            limiter.tryAcquire("conn-3", "user-3", 1);
            limiter.tryAcquire("conn-4", "user-4", 1);
            limiter.tryAcquire("conn-5", "user-5", 1);

            var result = limiter.tryAcquire("conn-6", "user-6", 1);

            assertThat(result).isInstanceOf(WebSocketConnectionLimiter.AcquireResult.ContainerLimitExceeded.class);
            var exceeded = (WebSocketConnectionLimiter.AcquireResult.ContainerLimitExceeded) result;
            assertThat(exceeded.currentCount()).isEqualTo(5);
            assertThat(exceeded.maxAllowed()).isEqualTo(5);
        }

        @Test
        void shouldAllowDifferentUsersOnDifferentContainers() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-2", 2);
            var result = limiter.tryAcquire("conn-3", "user-3", 3);

            assertThat(result).isInstanceOf(WebSocketConnectionLimiter.AcquireResult.Success.class);
        }
    }

    @Nested
    @DisplayName("Releasing connections")
    class ReleasingConnections {

        @Test
        void shouldDecrementUserCountOnRelease() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-1", 1);

            limiter.release("conn-1");

            assertThat(limiter.getUserConnectionCount("user-1")).isEqualTo(1);
        }

        @Test
        void shouldDecrementContainerCountOnRelease() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-2", 1);

            limiter.release("conn-1");

            assertThat(limiter.getContainerConnectionCount(1)).isEqualTo(1);
        }

        @Test
        void shouldAllowNewConnectionAfterRelease() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-1", 1);
            limiter.tryAcquire("conn-3", "user-1", 1);

            limiter.release("conn-1");
            var result = limiter.tryAcquire("conn-4", "user-1", 1);

            assertThat(result).isInstanceOf(WebSocketConnectionLimiter.AcquireResult.Success.class);
        }

        @Test
        void shouldHandleReleaseOfUnknownConnection() {
            // Should not throw
            limiter.release("unknown-connection");
        }

        @Test
        void shouldCleanupUserEntryWhenCountReachesZero() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.release("conn-1");

            assertThat(limiter.getUserConnectionCount("user-1")).isZero();
        }
    }

    @Nested
    @DisplayName("Connection counts")
    class ConnectionCounts {

        @Test
        void shouldReturnZeroForUnknownUser() {
            assertThat(limiter.getUserConnectionCount("unknown")).isZero();
        }

        @Test
        void shouldReturnZeroForUnknownContainer() {
            assertThat(limiter.getContainerConnectionCount(999)).isZero();
        }

        @Test
        void shouldTrackTotalConnections() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-2", 2);
            limiter.tryAcquire("conn-3", "user-1", 3);

            assertThat(limiter.getTotalConnectionCount()).isEqualTo(3);
        }

        @Test
        void shouldUpdateTotalOnRelease() {
            limiter.tryAcquire("conn-1", "user-1", 1);
            limiter.tryAcquire("conn-2", "user-2", 1);
            limiter.release("conn-1");

            assertThat(limiter.getTotalConnectionCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        void shouldExposeMaxConnectionsPerUser() {
            assertThat(limiter.getMaxConnectionsPerUser()).isEqualTo(3);
        }

        @Test
        void shouldExposeMaxConnectionsPerContainer() {
            assertThat(limiter.getMaxConnectionsPerContainer()).isEqualTo(5);
        }
    }
}
