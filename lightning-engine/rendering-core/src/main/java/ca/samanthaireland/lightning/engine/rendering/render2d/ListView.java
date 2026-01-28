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
import java.util.Set;
import java.util.function.Consumer;

/**
 * Interface for scrollable list view components.
 */
public interface ListView extends WindowComponent {

    /**
     * Set the list items.
     */
    void setItems(List<String> items);

    /**
     * Get the list items.
     */
    List<String> getItems();

    /**
     * Add an item to the list.
     */
    void addItem(String item);

    /**
     * Remove an item at the specified index.
     */
    void removeItem(int index);

    /**
     * Clear all items.
     */
    void clearItems();

    /**
     * Get the selected index (for single-selection mode).
     */
    int getSelectedIndex();

    /**
     * Set the selected index (for single-selection mode).
     */
    void setSelectedIndex(int index);

    /**
     * Get the selected item (for single-selection mode).
     */
    String getSelectedItem();

    /**
     * Set the selection changed handler (receives index).
     */
    void setOnSelectionChanged(Consumer<Integer> onSelectionChanged);

    /**
     * Set the double-click handler (receives index).
     */
    void setOnItemDoubleClicked(Consumer<Integer> onItemDoubleClicked);

    /**
     * Set the item height.
     */
    void setItemHeight(float height);

    /**
     * Set the font size.
     */
    void setFontSize(float fontSize);

    // Multi-selection support

    /**
     * Enable or disable multi-selection mode.
     * In multi-selection mode, clicking toggles selection without clearing others.
     */
    default void setMultiSelectEnabled(boolean enabled) {
        // Default: no-op for backwards compatibility
    }

    /**
     * Check if multi-selection mode is enabled.
     */
    default boolean isMultiSelectEnabled() {
        return false;
    }

    /**
     * Get all selected indices (for multi-selection mode).
     */
    default Set<Integer> getSelectedIndices() {
        int idx = getSelectedIndex();
        return idx >= 0 ? Set.of(idx) : Set.of();
    }

    /**
     * Get all selected items (for multi-selection mode).
     */
    default List<String> getSelectedItems() {
        String item = getSelectedItem();
        return item != null ? List.of(item) : List.of();
    }

    /**
     * Select an index (adds to selection in multi-select mode).
     */
    default void selectIndex(int index) {
        setSelectedIndex(index);
    }

    /**
     * Deselect an index.
     */
    default void deselectIndex(int index) {
        if (getSelectedIndex() == index) {
            setSelectedIndex(-1);
        }
    }

    /**
     * Toggle selection at an index.
     */
    default void toggleSelection(int index) {
        if (getSelectedIndices().contains(index)) {
            deselectIndex(index);
        } else {
            selectIndex(index);
        }
    }

    /**
     * Clear all selections.
     */
    default void clearSelection() {
        setSelectedIndex(-1);
    }

    /**
     * Select all items.
     */
    default void selectAll() {
        // Default: select first item for single-select mode
        if (!getItems().isEmpty()) {
            setSelectedIndex(0);
        }
    }
}
