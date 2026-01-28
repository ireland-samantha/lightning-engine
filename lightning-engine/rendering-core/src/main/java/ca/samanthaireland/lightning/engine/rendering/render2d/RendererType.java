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


package ca.samanthaireland.lightning.engine.rendering.render2d;

/**
 * Enumeration of available renderer backends.
 */
public enum RendererType {

    /**
     * NanoVG-based renderer (default).
     * Uses NanoVG for vector graphics rendering with hardware-accelerated anti-aliasing.
     */
    NANOVG("nanovg"),

    /**
     * Pure OpenGL renderer.
     * Uses direct OpenGL calls with custom shaders.
     * Provides more control and potentially better performance for specific use cases.
     */
    PURE_OPENGL("opengl");

    private final String cliName;

    RendererType(String cliName) {
        this.cliName = cliName;
    }

    /**
     * Get the CLI name for this renderer type.
     */
    public String getCliName() {
        return cliName;
    }

    /**
     * Parse a renderer type from a CLI argument string.
     *
     * @param value the CLI value (e.g., "nanovg", "opengl")
     * @return the RendererType, or NANOVG if not recognized
     */
    public static RendererType fromString(String value) {
        if (value == null) {
            return NANOVG;
        }
        for (RendererType type : values()) {
            if (type.cliName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return NANOVG;
    }
}
