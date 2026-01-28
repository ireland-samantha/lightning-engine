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

import java.util.List;

/**
 * Interface for container panel components that can hold children.
 */
public interface Panel extends WindowComponent {

    /**
     * Add a child component to the panel.
     */
    void addChild(WindowComponent child);

    /**
     * Remove a child component from the panel.
     */
    void removeChild(WindowComponent child);

    /**
     * Clear all children from the panel.
     */
    void clearChildren();

    /**
     * Get all child components.
     */
    List<WindowComponent> getChildren();

    /**
     * Set the background color.
     */
    void setBackgroundColor(float[] color);

    /**
     * Set the border color.
     */
    void setBorderColor(float[] color);

    /**
     * Set the border width.
     */
    void setBorderWidth(float width);

    /**
     * Set the corner radius.
     */
    void setCornerRadius(float radius);

    /**
     * Set the panel title.
     */
    void setTitle(String title);

    /**
     * Set the title font size.
     */
    void setTitleFontSize(float fontSize);

    // === Scrolling Support ===

    /**
     * Enable or disable scrolling.
     * @param horizontal enable horizontal scrolling
     * @param vertical enable vertical scrolling
     */
    default void setScrollable(boolean horizontal, boolean vertical) {
        // Default implementation does nothing
    }

    /**
     * Check if horizontal scrolling is enabled.
     */
    default boolean isScrollableHorizontal() {
        return false;
    }

    /**
     * Check if vertical scrolling is enabled.
     */
    default boolean isScrollableVertical() {
        return false;
    }

    /**
     * Get the current horizontal scroll offset.
     */
    default float getScrollX() {
        return 0;
    }

    /**
     * Get the current vertical scroll offset.
     */
    default float getScrollY() {
        return 0;
    }

    /**
     * Set the scroll position.
     * @param x horizontal scroll offset (0 = left edge)
     * @param y vertical scroll offset (0 = top edge)
     */
    default void setScrollPosition(float x, float y) {
        // Default implementation does nothing
    }

    /**
     * Scroll to make a child component visible.
     * @param child the child component to scroll into view
     */
    default void scrollToChild(WindowComponent child) {
        // Default implementation does nothing
    }

    /**
     * Get the total content width (maximum extent of all children).
     */
    default int getContentWidth() {
        return 0;
    }

    /**
     * Get the total content height (maximum extent of all children).
     */
    default int getContentHeight() {
        return 0;
    }
}
