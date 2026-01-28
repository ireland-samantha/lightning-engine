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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AdapterConfig}.
 */
@DisplayName("AdapterConfig")
class AdapterConfigTest {

    @Nested
    @DisplayName("defaults()")
    class Defaults {

        @Test
        @DisplayName("should provide default connect timeout of 30 seconds")
        void shouldProvideDefaultConnectTimeout() {
            AdapterConfig config = AdapterConfig.defaults();

            assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("should provide default request timeout of 60 seconds")
        void shouldProvideDefaultRequestTimeout() {
            AdapterConfig config = AdapterConfig.defaults();

            assertThat(config.getRequestTimeout()).isEqualTo(Duration.ofSeconds(60));
        }
    }

    @Nested
    @DisplayName("withConnectTimeout()")
    class WithConnectTimeout {

        @Test
        @DisplayName("should use custom connect timeout")
        void shouldUseCustomConnectTimeout() {
            AdapterConfig config = AdapterConfig.withConnectTimeout(Duration.ofSeconds(10));

            assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("should use default request timeout")
        void shouldUseDefaultRequestTimeout() {
            AdapterConfig config = AdapterConfig.withConnectTimeout(Duration.ofSeconds(10));

            assertThat(config.getRequestTimeout()).isEqualTo(Duration.ofSeconds(60));
        }
    }

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("should use custom connect timeout")
        void shouldUseCustomConnectTimeout() {
            AdapterConfig config = AdapterConfig.of(Duration.ofSeconds(5), Duration.ofSeconds(120));

            assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("should use custom request timeout")
        void shouldUseCustomRequestTimeout() {
            AdapterConfig config = AdapterConfig.of(Duration.ofSeconds(5), Duration.ofSeconds(120));

            assertThat(config.getRequestTimeout()).isEqualTo(Duration.ofSeconds(120));
        }
    }

    @Nested
    @DisplayName("constants")
    class Constants {

        @Test
        @DisplayName("DEFAULT_CONNECT_TIMEOUT should be 30 seconds")
        void defaultConnectTimeoutShouldBe30Seconds() {
            assertThat(AdapterConfig.DEFAULT_CONNECT_TIMEOUT).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("DEFAULT_REQUEST_TIMEOUT should be 60 seconds")
        void defaultRequestTimeoutShouldBe60Seconds() {
            assertThat(AdapterConfig.DEFAULT_REQUEST_TIMEOUT).isEqualTo(Duration.ofSeconds(60));
        }
    }
}
