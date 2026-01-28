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
 * Interface for color utilities and constants.
 *
 * <p>Provides standard colors and UI theme colors for rendering.
 * Colors are represented as float arrays in RGBA format [r, g, b, a]
 * where each component is in the range 0.0 to 1.0.
 */
public interface Colour {

    // Standard colors
    float[] WHITE = {1.0f, 1.0f, 1.0f, 1.0f};
    float[] BLACK = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] RED = {1.0f, 0.0f, 0.0f, 1.0f};
    float[] GREEN = {0.0f, 1.0f, 0.0f, 1.0f};
    float[] BLUE = {0.0f, 0.0f, 1.0f, 1.0f};
    float[] YELLOW = {1.0f, 1.0f, 0.0f, 1.0f};
    float[] CYAN = {0.0f, 1.0f, 1.0f, 1.0f};
    float[] MAGENTA = {1.0f, 0.0f, 1.0f, 1.0f};
    float[] TRANSPARENT = {0.0f, 0.0f, 0.0f, 0.0f};

    // UI Theme colors (dark theme)
    float[] BACKGROUND = {0.12f, 0.12f, 0.14f, 1.0f};
    float[] PANEL_BG = {0.18f, 0.18f, 0.20f, 1.0f};
    float[] BUTTON_BG = {0.25f, 0.25f, 0.28f, 1.0f};
    float[] BUTTON_HOVER = {0.30f, 0.30f, 0.35f, 1.0f};
    float[] BUTTON_PRESSED = {0.20f, 0.20f, 0.22f, 1.0f};
    float[] BORDER = {0.35f, 0.35f, 0.40f, 1.0f};
    float[] TEXT_PRIMARY = {0.90f, 0.90f, 0.92f, 1.0f};
    float[] TEXT_SECONDARY = {0.60f, 0.60f, 0.65f, 1.0f};
    float[] ACCENT = {0.30f, 0.60f, 0.90f, 1.0f};
    float[] SUCCESS = {0.30f, 0.75f, 0.40f, 1.0f};
    float[] WARNING = {0.90f, 0.70f, 0.20f, 1.0f};
    float[] ERROR = {0.90f, 0.30f, 0.30f, 1.0f};

    /**
     * Create a color with the specified alpha value.
     *
     * @param color the base color
     * @param alpha the alpha value (0.0 to 1.0)
     * @return a new color array with the specified alpha
     */
    static float[] withAlpha(float[] color, float alpha) {
        return new float[]{color[0], color[1], color[2], alpha};
    }

    /**
     * Blend two colors.
     *
     * @param color1 the first color
     * @param color2 the second color
     * @param factor the blend factor (0.0 = color1, 1.0 = color2)
     * @return the blended color
     */
    static float[] blend(float[] color1, float[] color2, float factor) {
        float invFactor = 1.0f - factor;
        return new float[]{
            color1[0] * invFactor + color2[0] * factor,
            color1[1] * invFactor + color2[1] * factor,
            color1[2] * invFactor + color2[2] * factor,
            color1[3] * invFactor + color2[3] * factor
        };
    }

    /**
     * Create a color from RGB values (0-255).
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return the color array
     */
    static float[] rgb(int r, int g, int b) {
        return new float[]{r / 255.0f, g / 255.0f, b / 255.0f, 1.0f};
    }

    /**
     * Create a color from RGBA values (0-255).
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @param a alpha component (0-255)
     * @return the color array
     */
    static float[] rgba(int r, int g, int b, int a) {
        return new float[]{r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f};
    }
}
