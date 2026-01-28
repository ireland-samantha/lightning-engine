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
 * Interface for text label components.
 */
public interface Label extends WindowComponent {

    /**
     * Text overflow behavior when text exceeds the label width.
     */
    enum OverflowMode {
        /** Text is not clipped, may extend beyond bounds */
        VISIBLE,
        /** Text is clipped at bounds */
        CLIP,
        /** Text is truncated with ellipsis (...) */
        ELLIPSIS,
        /** Text scrolls horizontally on hover */
        SCROLL
    }

    /**
     * Get the label text.
     */
    String getText();

    /**
     * Set the label text.
     */
    void setText(String text);

    /**
     * Get the font size.
     */
    float getFontSize();

    /**
     * Set the font size.
     */
    void setFontSize(float fontSize);

    /**
     * Get the text color.
     */
    float[] getTextColor();

    /**
     * Set the text color.
     */
    void setTextColor(float[] color);

    /**
     * Set the text alignment.
     */
    void setAlignment(int alignment);

    /**
     * Set the maximum width for the label. Text exceeding this width
     * will be handled according to the overflow mode.
     * @param maxWidth the maximum width in pixels, or 0 for unlimited
     */
    default void setMaxWidth(int maxWidth) {
        // Default implementation does nothing
    }

    /**
     * Set the overflow mode for handling text that exceeds the label width.
     * @param mode the overflow mode
     */
    default void setOverflowMode(OverflowMode mode) {
        // Default implementation does nothing
    }
}
