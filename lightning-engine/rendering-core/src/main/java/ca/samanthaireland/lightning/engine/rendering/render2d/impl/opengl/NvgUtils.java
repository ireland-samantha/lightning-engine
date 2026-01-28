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

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Utility methods for common NanoVG rendering operations.
 * Reduces code duplication across GL components.
 */
public final class NvgUtils {

    private NvgUtils() {
        // Utility class
    }

    // ========== Font Setup ==========

    /**
     * Set up font for rendering.
     * Uses the specified fontId, or falls back to the default font from GLContext.
     *
     * @param nvg      the NanoVG context
     * @param fontId   the font ID, or -1 to use default
     * @param fontSize the font size
     */
    public static void setupFont(long nvg, int fontId, float fontSize) {
        int effectiveFontId = fontId >= 0 ? fontId : GLContext.getDefaultFontId();
        if (effectiveFontId >= 0) {
            nvgFontFaceId(nvg, effectiveFontId);
        }
        nvgFontSize(nvg, fontSize);
    }

    // ========== Rectangle Drawing ==========

    /**
     * Draw a filled rectangle.
     */
    public static void drawFilledRect(long nvg, float x, float y, float w, float h,
                                      float[] color, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgFillColor(nvg, GLColour.rgba(color, nvgColor));
        nvgFill(nvg);
    }

    /**
     * Draw a filled rounded rectangle.
     */
    public static void drawFilledRoundedRect(long nvg, float x, float y, float w, float h,
                                             float radius, float[] color, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, w, h, radius);
        nvgFillColor(nvg, GLColour.rgba(color, nvgColor));
        nvgFill(nvg);
    }

    /**
     * Draw a stroked rectangle (border only).
     */
    public static void drawStrokedRect(long nvg, float x, float y, float w, float h,
                                       float[] color, float strokeWidth, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgStrokeColor(nvg, GLColour.rgba(color, nvgColor));
        nvgStrokeWidth(nvg, strokeWidth);
        nvgStroke(nvg);
    }

    /**
     * Draw a stroked rounded rectangle (border only).
     */
    public static void drawStrokedRoundedRect(long nvg, float x, float y, float w, float h,
                                              float radius, float[] color, float strokeWidth, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, w, h, radius);
        nvgStrokeColor(nvg, GLColour.rgba(color, nvgColor));
        nvgStrokeWidth(nvg, strokeWidth);
        nvgStroke(nvg);
    }

    /**
     * Draw a filled and stroked rectangle.
     */
    public static void drawRect(long nvg, float x, float y, float w, float h,
                                float[] fillColor, float[] strokeColor, float strokeWidth, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgFillColor(nvg, GLColour.rgba(fillColor, nvgColor));
        nvgFill(nvg);
        if (strokeWidth > 0) {
            nvgStrokeColor(nvg, GLColour.rgba(strokeColor, nvgColor));
            nvgStrokeWidth(nvg, strokeWidth);
            nvgStroke(nvg);
        }
    }

    /**
     * Draw a filled and stroked rounded rectangle.
     */
    public static void drawRoundedRect(long nvg, float x, float y, float w, float h, float radius,
                                       float[] fillColor, float[] strokeColor, float strokeWidth, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, w, h, radius);
        nvgFillColor(nvg, GLColour.rgba(fillColor, nvgColor));
        nvgFill(nvg);
        if (strokeWidth > 0) {
            nvgStrokeColor(nvg, GLColour.rgba(strokeColor, nvgColor));
            nvgStrokeWidth(nvg, strokeWidth);
            nvgStroke(nvg);
        }
    }

    // ========== Scrollbar Drawing ==========

    /**
     * Draw a vertical scrollbar.
     *
     * @param nvg           the NanoVG context
     * @param x             scrollbar x position
     * @param y             scrollbar y position
     * @param width         scrollbar width
     * @param height        scrollbar height
     * @param scrollOffset  current scroll offset
     * @param maxScroll     maximum scroll value
     * @param contentHeight total content height
     * @param trackColor    scrollbar track color
     * @param thumbColor    scrollbar thumb color
     * @param nvgColor      reusable NVGColor instance
     */
    public static void drawVerticalScrollbar(long nvg, float x, float y, float width, float height,
                                             float scrollOffset, float maxScroll, float contentHeight,
                                             float[] trackColor, float[] thumbColor, NVGColor nvgColor) {
        if (contentHeight <= height || maxScroll <= 0) {
            return; // No scrollbar needed
        }

        // Draw scrollbar track
        drawFilledRect(nvg, x, y, width, height, trackColor, nvgColor);

        // Calculate thumb size and position
        float thumbHeight = Math.max(20, (height / contentHeight) * height);
        float thumbY = y + (scrollOffset / maxScroll) * (height - thumbHeight);

        // Draw scrollbar thumb
        drawFilledRoundedRect(nvg, x + 2, thumbY, width - 4, thumbHeight, 3, thumbColor, nvgColor);
    }

    /**
     * Draw a horizontal scrollbar.
     *
     * @param nvg          the NanoVG context
     * @param x            scrollbar x position
     * @param y            scrollbar y position
     * @param width        scrollbar width
     * @param height       scrollbar height
     * @param scrollOffset current scroll offset
     * @param maxScroll    maximum scroll value
     * @param contentWidth total content width
     * @param trackColor   scrollbar track color
     * @param thumbColor   scrollbar thumb color
     * @param nvgColor     reusable NVGColor instance
     */
    public static void drawHorizontalScrollbar(long nvg, float x, float y, float width, float height,
                                               float scrollOffset, float maxScroll, float contentWidth,
                                               float[] trackColor, float[] thumbColor, NVGColor nvgColor) {
        if (contentWidth <= width || maxScroll <= 0) {
            return; // No scrollbar needed
        }

        // Draw scrollbar track
        drawFilledRect(nvg, x, y, width, height, trackColor, nvgColor);

        // Calculate thumb size and position
        float thumbWidth = Math.max(20, (width / contentWidth) * width);
        float thumbX = x + (scrollOffset / maxScroll) * (width - thumbWidth);

        // Draw scrollbar thumb
        drawFilledRoundedRect(nvg, thumbX, y + 2, thumbWidth, height - 4, 3, thumbColor, nvgColor);
    }

    // ========== Text Drawing ==========

    /**
     * Draw text at the specified position.
     *
     * @param nvg      the NanoVG context
     * @param x        text x position
     * @param y        text y position
     * @param text     the text to draw
     * @param color    text color
     * @param align    NanoVG alignment flags (e.g., NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE)
     * @param nvgColor reusable NVGColor instance
     */
    public static void drawText(long nvg, float x, float y, String text, float[] color,
                                int align, NVGColor nvgColor) {
        nvgTextAlign(nvg, align);
        nvgFillColor(nvg, GLColour.rgba(color, nvgColor));
        nvgText(nvg, x, y, text);
    }

    /**
     * Draw text with font setup.
     *
     * @param nvg      the NanoVG context
     * @param x        text x position
     * @param y        text y position
     * @param text     the text to draw
     * @param color    text color
     * @param fontId   font ID, or -1 for default
     * @param fontSize font size
     * @param align    NanoVG alignment flags
     * @param nvgColor reusable NVGColor instance
     */
    public static void drawText(long nvg, float x, float y, String text, float[] color,
                                int fontId, float fontSize, int align, NVGColor nvgColor) {
        setupFont(nvg, fontId, fontSize);
        drawText(nvg, x, y, text, color, align, nvgColor);
    }

    // ========== Line Drawing ==========

    /**
     * Draw a horizontal line.
     */
    public static void drawHorizontalLine(long nvg, float x1, float x2, float y,
                                          float[] color, float strokeWidth, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x1, y);
        nvgLineTo(nvg, x2, y);
        nvgStrokeColor(nvg, GLColour.rgba(color, nvgColor));
        nvgStrokeWidth(nvg, strokeWidth);
        nvgStroke(nvg);
    }

    /**
     * Draw a vertical line.
     */
    public static void drawVerticalLine(long nvg, float x, float y1, float y2,
                                        float[] color, float strokeWidth, NVGColor nvgColor) {
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x, y1);
        nvgLineTo(nvg, x, y2);
        nvgStrokeColor(nvg, GLColour.rgba(color, nvgColor));
        nvgStrokeWidth(nvg, strokeWidth);
        nvgStroke(nvg);
    }
}
