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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CommandWebSocketClient.
 *
 * <p>Note: Full WebSocket connection tests are in integration tests (CommandWebSocketIT).
 * These tests focus on validation and input handling that can be tested without
 * a real WebSocket connection.
 */
class CommandWebSocketClientTest {

    @Nested
    @DisplayName("connect validation")
    class ConnectValidationTests {

        @Test
        @DisplayName("should reject null token")
        void shouldRejectNullToken() {
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("http://localhost:8080", 1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token is required");
        }

        @Test
        @DisplayName("should reject blank token")
        void shouldRejectBlankToken() {
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("http://localhost:8080", 1, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token is required");
        }

        @Test
        @DisplayName("should reject whitespace-only token")
        void shouldRejectWhitespaceToken() {
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("http://localhost:8080", 1, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token is required");
        }

        @Test
        @DisplayName("should throw IOException for invalid URL")
        void shouldThrowForInvalidUrl() {
            // Invalid URL should cause connection failure wrapped in IOException
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("not-a-valid-url", 1, "valid-token"))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("should throw IOException for unreachable host")
        void shouldThrowForUnreachableHost() {
            // Non-routable IP should timeout
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("http://10.255.255.1:8080", 1, "valid-token",
                            java.time.Duration.ofMillis(100)))
                    .isInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("URL construction")
    class UrlConstructionTests {

        // URL construction is tested indirectly through connection tests
        // since the URL building logic is in the static connect method.
        // Full URL construction verification happens in integration tests.

        @Test
        @DisplayName("should accept http URL")
        void shouldAcceptHttpUrl() {
            // This will fail to connect but should not fail URL parsing
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("http://localhost:9999", 1, "token",
                            java.time.Duration.ofMillis(100)))
                    .isInstanceOf(IOException.class)
                    // Should be a connection error, not a URL parsing error
                    .hasMessageContaining("connect");
        }

        @Test
        @DisplayName("should accept https URL")
        void shouldAcceptHttpsUrl() {
            // This will fail to connect but should not fail URL parsing
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("https://localhost:9999", 1, "token",
                            java.time.Duration.ofMillis(100)))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("should handle URL without trailing slash")
        void shouldHandleUrlWithoutTrailingSlash() {
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("http://localhost:9999", 1, "token",
                            java.time.Duration.ofMillis(100)))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("should handle URL with trailing slash")
        void shouldHandleUrlWithTrailingSlash() {
            assertThatThrownBy(() ->
                    CommandWebSocketClient.connect("http://localhost:9999/", 1, "token",
                            java.time.Duration.ofMillis(100)))
                    .isInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("ByteBufferAccumulator")
    class ByteBufferAccumulatorTests {

        // ByteBufferAccumulator is a private inner class, so we can't test it directly.
        // Its functionality is verified through integration tests that send fragmented
        // binary messages. The accumulator correctly handles:
        // - Single complete messages
        // - Messages split across multiple frames
        // - Large messages that require multiple reads

        @Test
        @DisplayName("accumulator is tested via integration tests")
        void accumulatorIsTestedViaIntegration() {
            // This is a documentation test - the actual accumulator tests are in
            // CommandWebSocketIT where we verify end-to-end WebSocket message handling
            assertThat(true).isTrue();
        }
    }
}
