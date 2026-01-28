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

import ca.samanthaireland.lightning.engine.rendering.render2d.AbstractWindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.ListView;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

/**
 * A scrollable list view component.
 * OpenGL implementation of {@link ListView}.
 */
@Slf4j
public class GLListView extends AbstractWindowComponent implements ListView {

    private final List<String> items = new ArrayList<>();
    private int selectedIndex = -1;
    private final Set<Integer> selectedIndices = new HashSet<>();
    private boolean multiSelectEnabled = false;
    private int hoveredIndex = -1;
    private float scrollOffset = 0;
    private float itemHeight = 24.0f;
    private float fontSize = 14.0f;
    private int fontId = -1;

    private float[] backgroundColor;
    private float[] itemBackgroundColor;
    private float[] selectedBackgroundColor;
    private float[] hoveredBackgroundColor;
    private float[] textColor;
    private float[] borderColor;
    private float[] scrollbarColor;
    private float[] scrollbarTrackColor;

    private Consumer<Integer> onSelectionChanged;
    private Consumer<Integer> onItemDoubleClicked;

    private long lastClickTime = 0;
    private int lastClickIndex = -1;
    private static final long DOUBLE_CLICK_TIME_MS = 300;

    public GLListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.backgroundColor = GLColour.PANEL_BG;
        this.itemBackgroundColor = GLColour.PANEL_BG;
        this.selectedBackgroundColor = GLColour.SELECTED;
        this.hoveredBackgroundColor = GLColour.withAlpha(GLColour.SELECTED, 0.3f);
        this.textColor = GLColour.TEXT_PRIMARY;
        this.borderColor = GLColour.BORDER;
        this.scrollbarColor = GLColour.SCROLLBAR;
        this.scrollbarTrackColor = GLColour.SCROLLBAR_TRACK;
    }

    public void setItems(List<String> items) {
        this.items.clear();
        this.items.addAll(items);
        this.selectedIndex = -1;
        this.selectedIndices.clear();
        this.scrollOffset = 0;
    }

    public void addItem(String item) {
        items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            if (selectedIndex >= items.size()) {
                selectedIndex = items.size() - 1;
            }
        }
    }

    public void clearItems() {
        items.clear();
        selectedIndex = -1;
        selectedIndices.clear();
        scrollOffset = 0;
    }

    public List<String> getItems() {
        return items;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= -1 && index < items.size()) {
            this.selectedIndex = index;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(index);
            }
        }
    }

    public String getSelectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    public void setItemHeight(float itemHeight) {
        this.itemHeight = itemHeight;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setFontId(int fontId) {
        this.fontId = fontId;
    }

    // Multi-selection support

    @Override
    public void setMultiSelectEnabled(boolean enabled) {
        this.multiSelectEnabled = enabled;
        if (!enabled) {
            // When disabling, keep only the first selected index
            if (!selectedIndices.isEmpty()) {
                int first = selectedIndices.stream().min(Integer::compareTo).orElse(-1);
                selectedIndices.clear();
                selectedIndex = first;
            }
        } else if (selectedIndex >= 0) {
            // When enabling, add current selection to the set
            selectedIndices.add(selectedIndex);
        }
    }

    @Override
    public boolean isMultiSelectEnabled() {
        return multiSelectEnabled;
    }

    @Override
    public Set<Integer> getSelectedIndices() {
        if (multiSelectEnabled) {
            return new HashSet<>(selectedIndices);
        }
        return selectedIndex >= 0 ? Set.of(selectedIndex) : Set.of();
    }

    @Override
    public List<String> getSelectedItems() {
        List<String> result = new ArrayList<>();
        for (int idx : getSelectedIndices()) {
            if (idx >= 0 && idx < items.size()) {
                result.add(items.get(idx));
            }
        }
        return result;
    }

    @Override
    public void selectIndex(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        if (multiSelectEnabled) {
            selectedIndices.add(index);
            selectedIndex = index; // Keep track of last selected for backwards compat
        } else {
            selectedIndex = index;
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(index);
        }
    }

    @Override
    public void deselectIndex(int index) {
        if (multiSelectEnabled) {
            selectedIndices.remove(index);
            if (selectedIndex == index) {
                selectedIndex = selectedIndices.isEmpty() ? -1 : selectedIndices.iterator().next();
            }
        } else if (selectedIndex == index) {
            selectedIndex = -1;
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(-1);
        }
    }

    @Override
    public void toggleSelection(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        if (multiSelectEnabled) {
            if (selectedIndices.contains(index)) {
                deselectIndex(index);
            } else {
                selectIndex(index);
            }
        } else {
            // In single-select mode, toggle between selected and unselected
            if (selectedIndex == index) {
                selectedIndex = -1;
            } else {
                selectedIndex = index;
            }
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(selectedIndex);
            }
        }
    }

    @Override
    public void clearSelection() {
        selectedIndex = -1;
        selectedIndices.clear();
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(-1);
        }
    }

    @Override
    public void selectAll() {
        if (multiSelectEnabled) {
            for (int i = 0; i < items.size(); i++) {
                selectedIndices.add(i);
            }
            if (!items.isEmpty()) {
                selectedIndex = 0;
            }
        } else if (!items.isEmpty()) {
            selectedIndex = 0;
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(selectedIndex);
        }
    }

    public void setOnSelectionChanged(Consumer<Integer> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setOnItemDoubleClicked(Consumer<Integer> onItemDoubleClicked) {
        this.onItemDoubleClicked = onItemDoubleClicked;
    }

    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void render(Renderer renderer) {
        if (!visible) {
            return;
        }

        // Draw background
        renderer.fillRect(x, y, width, height, backgroundColor);

        // Draw border
        renderer.strokeRect(x, y, width, height, borderColor, 1.0f);

        // Calculate visible items
        float totalHeight = items.size() * itemHeight;
        float maxScroll = Math.max(0, totalHeight - height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int scrollbarWidth = needsScrollbar() ? 10 : 0;
        int contentWidth = width - scrollbarWidth - 2;

        // Clip content area
        renderer.save();
        renderer.intersectClip(x + 1, y + 1, contentWidth, height - 2);

        // Setup font
        renderer.setFont(fontId, fontSize);

        // Draw items
        int firstVisibleIndex = (int) (scrollOffset / itemHeight);
        int lastVisibleIndex = Math.min(items.size() - 1, (int) ((scrollOffset + height) / itemHeight));

        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
            float itemY = y + (i * itemHeight) - scrollOffset;

            // Draw item background
            float[] bgColor = itemBackgroundColor;
            boolean isSelected = multiSelectEnabled ? selectedIndices.contains(i) : (i == selectedIndex);
            if (isSelected) {
                bgColor = selectedBackgroundColor;
            } else if (i == hoveredIndex) {
                bgColor = hoveredBackgroundColor;
            }

            renderer.fillRect(x + 1, itemY, contentWidth, itemHeight, bgColor);

            // Draw item text
            renderer.drawText(x + 8, itemY + itemHeight / 2, items.get(i), textColor,
                    Renderer.ALIGN_LEFT | Renderer.ALIGN_MIDDLE);
        }

        renderer.restore();

        // Draw scrollbar if needed
        if (needsScrollbar()) {
            float scrollbarX = x + width - scrollbarWidth - 1;
            float scrollbarHeight = height - 2;
            float thumbHeight = Math.max(20, (height / totalHeight) * scrollbarHeight);
            float thumbY = y + 1 + (scrollOffset / maxScroll) * (scrollbarHeight - thumbHeight);

            // Draw scrollbar track
            renderer.fillRect(scrollbarX, y + 1, scrollbarWidth, scrollbarHeight, scrollbarTrackColor);

            // Draw scrollbar thumb
            renderer.fillRoundedRect(scrollbarX + 2, thumbY, scrollbarWidth - 4, thumbHeight, 3, scrollbarColor);
        }
    }

    private boolean needsScrollbar() {
        return items.size() * itemHeight > height;
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        if (InputConstants.isLeftButton(button) && InputConstants.isPress(action)) {
            int scrollbarWidth = needsScrollbar() ? 10 : 0;
            if (mx < x + width - scrollbarWidth) {
                // Click on item
                int clickedIndex = (int) ((my - y + scrollOffset) / itemHeight);
                if (clickedIndex >= 0 && clickedIndex < items.size()) {
                    long currentTime = System.currentTimeMillis();

                    // Check for double click
                    if (clickedIndex == lastClickIndex &&
                        currentTime - lastClickTime < DOUBLE_CLICK_TIME_MS) {
                        if (onItemDoubleClicked != null) {
                            onItemDoubleClicked.accept(clickedIndex);
                        }
                        lastClickTime = 0;
                    } else {
                        // Handle selection based on multi-select mode
                        if (multiSelectEnabled) {
                            toggleSelection(clickedIndex);
                        } else {
                            setSelectedIndex(clickedIndex);
                        }
                        lastClickTime = currentTime;
                        lastClickIndex = clickedIndex;
                    }
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onMouseMove(int mx, int my) {
        if (!visible) {
            return false;
        }

        if (contains(mx, my)) {
            int scrollbarWidth = needsScrollbar() ? 10 : 0;
            if (mx < x + width - scrollbarWidth) {
                int hovered = (int) ((my - y + scrollOffset) / itemHeight);
                hoveredIndex = (hovered >= 0 && hovered < items.size()) ? hovered : -1;
            } else {
                hoveredIndex = -1;
            }
            return true;
        } else {
            hoveredIndex = -1;
            return false;
        }
    }

    @Override
    public boolean onMouseScroll(int mx, int my, double scrollX, double scrollY) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        float totalHeight = items.size() * itemHeight;
        float maxScroll = Math.max(0, totalHeight - height);

        scrollOffset -= (float) (scrollY * itemHeight * 2);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        return true;
    }

    public void scrollToSelected() {
        if (selectedIndex >= 0) {
            float itemTop = selectedIndex * itemHeight;
            float itemBottom = itemTop + itemHeight;

            if (itemTop < scrollOffset) {
                scrollOffset = itemTop;
            } else if (itemBottom > scrollOffset + height) {
                scrollOffset = itemBottom - height;
            }
        }
    }
}
