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


package ca.samanthaireland.lightning.engine.rendering.gui;

import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLFontLoader;
import org.junit.jupiter.api.*;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GLFontLoader}.
 *
 * <p>These tests verify the font loading logic without requiring
 * an actual NanoVG context (which requires OpenGL).
 *
 * <p>Note: Although these tests use a mock NanoVG context, the GLFontLoader
 * class still depends on NanoVG native libraries for class loading.
 */
@DisplayName("FontLoader")
@DisabledIfLwjglUnavailable
class GLFontLoaderTest {

    // Use 0 as a mock context since we're testing non-NanoVG functionality
    private static final long MOCK_NVG_CONTEXT = 0;

    private GLFontLoader fontLoader;

    @BeforeEach
    void setUp() {
        fontLoader = new GLFontLoader(MOCK_NVG_CONTEXT);
    }

    @Nested
    @DisplayName("getSystemFontPaths")
    class GetSystemFontPaths {

        @Test
        @DisplayName("should return non-empty array of font paths")
        void shouldReturnNonEmptyArray() {
            String[] paths = fontLoader.getSystemFontPaths();

            assertThat(paths).isNotEmpty();
        }

        @Test
        @DisplayName("should return paths with ttf extension")
        void shouldReturnTtfPaths() {
            String[] paths = fontLoader.getSystemFontPaths();

            for (String path : paths) {
                assertThat(path).endsWith(".ttf");
            }
        }

        @Test
        @DisplayName("should return OS-specific paths")
        void shouldReturnOsSpecificPaths() {
            String[] paths = fontLoader.getSystemFontPaths();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("mac")) {
                assertThat(paths[0]).contains("/System/Library/Fonts");
            } else if (os.contains("win")) {
                assertThat(paths[0]).contains("Windows\\Fonts");
            } else {
                assertThat(paths[0]).contains("/usr/share/fonts");
            }
        }
    }

    @Nested
    @DisplayName("loadResourceToBuffer")
    class LoadResourceToBuffer {

        @Test
        @DisplayName("should return null for non-existent resource")
        void shouldReturnNullForNonExistentResource() {
            ByteBuffer result = fontLoader.loadResourceToBuffer("non-existent-resource.ttf");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for null path")
        void shouldReturnNullForNullPath() {
            // This tests the graceful handling of null input
            ByteBuffer result = fontLoader.loadResourceToBuffer(null);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("loadBundledFont")
    class LoadBundledFont {

        @Test
        @DisplayName("should return -1 when bundled font is missing")
        void shouldReturnNegativeOneWhenBundledFontMissing() {
            // Without a real NanoVG context, the font creation will fail
            // but this tests that the method handles missing resources gracefully
            int fontId = fontLoader.loadBundledFont();

            // Will be -1 because either the font doesn't exist or NanoVG context is invalid
            assertThat(fontId).isLessThan(0);
        }
    }

    @Nested
    @DisplayName("loadSystemFont")
    class LoadSystemFont {

        @Test
        @DisplayName("should return -1 without valid NanoVG context")
        void shouldReturnNegativeOneWithoutValidContext() {
            // Without a real NanoVG context, system font loading will fail
            int fontId = fontLoader.loadSystemFont();

            assertThat(fontId).isLessThan(0);
        }
    }

    @Nested
    @DisplayName("loadDefaultFont")
    class LoadDefaultFont {

        @Test
        @DisplayName("should return -1 without valid NanoVG context")
        void shouldReturnNegativeOneWithoutValidContext() {
            // Without a real NanoVG context, font loading will fail
            int fontId = fontLoader.loadDefaultFont();

            assertThat(fontId).isLessThan(0);
        }
    }

    @Nested
    @DisplayName("loadFontFromResource")
    class LoadFontFromResource {

        @Test
        @DisplayName("should return -1 for non-existent resource")
        void shouldReturnNegativeOneForNonExistentResource() {
            int fontId = fontLoader.loadFontFromResource("test", "non-existent.ttf");

            assertThat(fontId).isLessThan(0);
        }
    }

    @Nested
    @DisplayName("loadFontFromFile")
    class LoadFontFromFile {

        @Test
        @DisplayName("should return -1 for non-existent file")
        void shouldReturnNegativeOneForNonExistentFile() {
            int fontId = fontLoader.loadFontFromFile("test", "/non/existent/path.ttf");

            assertThat(fontId).isLessThan(0);
        }
    }
}
