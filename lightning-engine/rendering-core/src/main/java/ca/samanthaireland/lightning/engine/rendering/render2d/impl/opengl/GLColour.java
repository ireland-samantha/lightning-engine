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


package ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl;

import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.nanovg.NanoVG.nvgRGBAf;

/**
 * Color utility class for GUI rendering with NanoVG.
 */
public final class GLColour {

    private GLColour() {}

    // Common colors
    public static final float[] WHITE = {1.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] BLACK = {0.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] GRAY = {0.5f, 0.5f, 0.5f, 1.0f};
    public static final float[] DARK_GRAY = {0.2f, 0.2f, 0.2f, 1.0f};
    public static final float[] LIGHT_GRAY = {0.8f, 0.8f, 0.8f, 1.0f};
    public static final float[] RED = {1.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] GREEN = {0.0f, 1.0f, 0.0f, 1.0f};
    public static final float[] BLUE = {0.0f, 0.0f, 1.0f, 1.0f};
    public static final float[] YELLOW = {1.0f, 1.0f, 0.0f, 1.0f};
    public static final float[] CYAN = {0.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] MAGENTA = {1.0f, 0.0f, 1.0f, 1.0f};
    public static final float[] TRANSPARENT = {0.0f, 0.0f, 0.0f, 0.0f};

    // Status colors
    public static final float[] SUCCESS = {0.2f, 0.8f, 0.2f, 1.0f};
    public static final float[] WARNING = {0.9f, 0.7f, 0.1f, 1.0f};
    public static final float[] ERROR = {0.9f, 0.2f, 0.2f, 1.0f};

    // UI theme colors
    public static final float[] BACKGROUND = {0.15f, 0.15f, 0.18f, 1.0f};
    public static final float[] PANEL_BG = {0.2f, 0.2f, 0.24f, 1.0f};
    public static final float[] BUTTON_BG = {0.3f, 0.3f, 0.35f, 1.0f};
    public static final float[] BUTTON_HOVER = {0.4f, 0.4f, 0.45f, 1.0f};
    public static final float[] BUTTON_PRESSED = {0.25f, 0.25f, 0.3f, 1.0f};
    public static final float[] TEXT_PRIMARY = {0.9f, 0.9f, 0.9f, 1.0f};
    public static final float[] TEXT_SECONDARY = {0.6f, 0.6f, 0.65f, 1.0f};
    public static final float[] ACCENT = {0.3f, 0.6f, 0.9f, 1.0f};
    public static final float[] ACCENT_HOVER = {0.4f, 0.7f, 1.0f, 1.0f};
    public static final float[] BORDER = {0.35f, 0.35f, 0.4f, 1.0f};
    public static final float[] SELECTED = {0.3f, 0.5f, 0.7f, 0.5f};
    public static final float[] SCROLLBAR = {0.4f, 0.4f, 0.45f, 0.8f};
    public static final float[] SCROLLBAR_TRACK = {0.25f, 0.25f, 0.3f, 0.5f};

    /**
     * Create an NVGColor from RGBA float values.
     */
    public static NVGColor rgba(float r, float g, float b, float a, NVGColor dest) {
        return nvgRGBAf(r, g, b, a, dest);
    }

    /**
     * Create an NVGColor from a float array [r, g, b, a].
     */
    public static NVGColor rgba(float[] color, NVGColor dest) {
        return nvgRGBAf(color[0], color[1], color[2], color[3], dest);
    }

    /**
     * Create a color with modified alpha.
     */
    public static float[] withAlpha(float[] color, float alpha) {
        return new float[]{color[0], color[1], color[2], alpha};
    }

    /**
     * Blend two colors.
     */
    public static float[] blend(float[] c1, float[] c2, float t) {
        return new float[]{
            c1[0] + (c2[0] - c1[0]) * t,
            c1[1] + (c2[1] - c1[1]) * t,
            c1[2] + (c2[2] - c1[2]) * t,
            c1[3] + (c2[3] - c1[3]) * t
        };
    }
}
