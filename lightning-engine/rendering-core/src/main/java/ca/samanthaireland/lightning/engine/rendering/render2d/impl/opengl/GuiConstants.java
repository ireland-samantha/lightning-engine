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

/**
 * Constants for GUI rendering.
 * Consolidates magic numbers from various GUI components.
 */
public final class GuiConstants {

    private GuiConstants() {
        // Utility class
    }

    // ========== Typography ==========

    /** Small font size for secondary text. */
    public static final float FONT_SIZE_SMALL = 12.0f;

    /** Default font size for most text. */
    public static final float FONT_SIZE_DEFAULT = 14.0f;

    /** Large font size for headings. */
    public static final float FONT_SIZE_LARGE = 16.0f;

    /** Extra large font size for titles. */
    public static final float FONT_SIZE_XLARGE = 18.0f;

    // ========== Sizing ==========

    /** Compact item height for dense lists. */
    public static final float ITEM_HEIGHT_COMPACT = 22.0f;

    /** Default item height for lists and trees. */
    public static final float ITEM_HEIGHT_DEFAULT = 24.0f;

    /** Comfortable item height for spacious layouts. */
    public static final float ITEM_HEIGHT_COMFORTABLE = 28.0f;

    /** Default corner radius for rounded components. */
    public static final float CORNER_RADIUS = 4.0f;

    /** Default border width. */
    public static final float BORDER_WIDTH = 1.0f;

    /** Default padding inside components. */
    public static final float PADDING = 8.0f;

    /** Small padding for tight layouts. */
    public static final float PADDING_SMALL = 4.0f;

    // ========== Scrolling ==========

    /** Default scrollbar width. */
    public static final int SCROLLBAR_WIDTH = 8;

    /** Wide scrollbar width. */
    public static final int SCROLLBAR_WIDTH_WIDE = 10;

    /** Minimum scrollbar thumb length. */
    public static final int SCROLLBAR_MIN_LENGTH = 20;

    /** Scroll speed multiplier. */
    public static final float SCROLL_SPEED = 20.0f;

    // ========== Timing ==========

    /** Double-click detection time in milliseconds. */
    public static final long DOUBLE_CLICK_MS = 300;

    /** Cursor blink interval in milliseconds. */
    public static final long CURSOR_BLINK_MS = 530;

    // ========== Tree View ==========

    /** Indentation width for tree nodes. */
    public static final float TREE_INDENT_WIDTH = 20.0f;

    /** Expand/collapse icon size. */
    public static final float TREE_ICON_SIZE = 8.0f;

    // ========== Text Field ==========

    /** Cursor width in pixels. */
    public static final float CURSOR_WIDTH = 2.0f;

    /** Text field horizontal padding. */
    public static final float TEXT_FIELD_PADDING = 6.0f;
}
