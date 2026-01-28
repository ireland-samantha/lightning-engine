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


package ca.samanthaireland.lightning.engine.rendering.testing.headless;

import ca.samanthaireland.lightning.engine.rendering.render2d.AbstractWindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.ListView;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mock ListView implementation for headless testing.
 */
public class MockListView extends AbstractWindowComponent implements ListView {

    private final List<String> items = new ArrayList<>();
    private int selectedIndex = -1;
    private float itemHeight = 24.0f;
    private float fontSize = 14.0f;

    private Consumer<Integer> onSelectionChanged;
    private Consumer<Integer> onItemDoubleClicked;

    public MockListView(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void setItems(List<String> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        this.selectedIndex = -1;
    }

    @Override
    public List<String> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public void addItem(String item) {
        items.add(item);
    }

    @Override
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            if (selectedIndex >= items.size()) {
                selectedIndex = items.size() - 1;
            }
        }
    }

    @Override
    public void clearItems() {
        items.clear();
        selectedIndex = -1;
    }

    @Override
    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    public void setSelectedIndex(int index) {
        int oldIndex = this.selectedIndex;
        this.selectedIndex = (index >= 0 && index < items.size()) ? index : -1;
        if (oldIndex != this.selectedIndex && onSelectionChanged != null) {
            onSelectionChanged.accept(this.selectedIndex);
        }
    }

    @Override
    public String getSelectedItem() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            return items.get(selectedIndex);
        }
        return null;
    }

    @Override
    public void setOnSelectionChanged(Consumer<Integer> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    @Override
    public void setOnItemDoubleClicked(Consumer<Integer> onItemDoubleClicked) {
        this.onItemDoubleClicked = onItemDoubleClicked;
    }

    @Override
    public void setItemHeight(float height) {
        this.itemHeight = height;
    }

    @Override
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public float getItemHeight() {
        return itemHeight;
    }

    public float getFontSize() {
        return fontSize;
    }

    @Override
    public void render(Renderer renderer) {
        // No rendering in headless mode
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        if (InputConstants.isLeftButton(button) && InputConstants.isPress(action) && !items.isEmpty()) {
            // Calculate which item was clicked
            int localY = my - y;
            int clickedIndex = (int) (localY / itemHeight);

            if (clickedIndex >= 0 && clickedIndex < items.size()) {
                setSelectedIndex(clickedIndex);
                return true;
            }
        }

        return true;
    }

    /**
     * Simulate a double-click on an item for testing.
     */
    public void simulateDoubleClick(int index) {
        if (index >= 0 && index < items.size() && onItemDoubleClicked != null) {
            onItemDoubleClicked.accept(index);
        }
    }
}
