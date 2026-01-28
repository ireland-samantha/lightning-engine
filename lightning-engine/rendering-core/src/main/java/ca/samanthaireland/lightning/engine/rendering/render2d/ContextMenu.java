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
 * Interface for context menus (right-click menus).
 */
public interface ContextMenu extends WindowComponent {

    /**
     * A menu item with label and action.
     */
    interface MenuItem {
        String getLabel();
        Runnable getAction();
        boolean isEnabled();
        boolean isSeparator();
    }

    /**
     * Add a menu item.
     * @param label the display text
     * @param action the action to perform when clicked
     */
    void addItem(String label, Runnable action);

    /**
     * Add a menu item with enabled state.
     * @param label the display text
     * @param action the action to perform when clicked
     * @param enabled whether the item is clickable
     */
    void addItem(String label, Runnable action, boolean enabled);

    /**
     * Add a separator line.
     */
    void addSeparator();

    /**
     * Get all menu items.
     */
    List<MenuItem> getItems();

    /**
     * Clear all items.
     */
    void clearItems();

    /**
     * Show the context menu at the specified position.
     * @param x the x position
     * @param y the y position
     */
    void show(int x, int y);

    /**
     * Hide the context menu.
     */
    void hide();

    /**
     * Check if the menu is currently visible.
     */
    boolean isShowing();
}
