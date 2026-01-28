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
import ca.samanthaireland.lightning.engine.rendering.render2d.ContextMenu;
import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * A right-click context menu component.
 * OpenGL/NanoVG implementation of {@link ContextMenu}.
 */
@Slf4j
public class GLContextMenu extends AbstractWindowComponent implements ContextMenu {

    private final List<MenuItemImpl> items = new ArrayList<>();
    private boolean showing = false;
    private int hoveredIndex = -1;

    private static final int ITEM_HEIGHT = 24;
    private static final int ITEM_PADDING = 8;
    private static final int MIN_WIDTH = 120;
    private static final int SEPARATOR_HEIGHT = 9;

    private float[] backgroundColor;
    private float[] borderColor;
    private float[] textColor;
    private float[] disabledColor;
    private float[] hoverColor;
    private float fontSize;
    private int fontId;

    public GLContextMenu() {
        super(0, 0, MIN_WIDTH, 0);
        this.backgroundColor = GLColour.PANEL_BG;
        this.borderColor = GLColour.BORDER;
        this.textColor = GLColour.TEXT_PRIMARY;
        this.disabledColor = GLColour.TEXT_SECONDARY;
        this.hoverColor = GLColour.ACCENT;
        this.fontSize = 13.0f;
        this.fontId = -1;
        this.visible = false;
    }

    @Override
    public void addItem(String label, Runnable action) {
        addItem(label, action, true);
    }

    @Override
    public void addItem(String label, Runnable action, boolean enabled) {
        items.add(new MenuItemImpl(label, action, enabled, false));
        recalculateSize();
    }

    @Override
    public void addSeparator() {
        items.add(new MenuItemImpl(null, null, false, true));
        recalculateSize();
    }

    @Override
    public List<MenuItem> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public void clearItems() {
        items.clear();
        recalculateSize();
    }

    @Override
    public void show(int x, int y) {
        this.x = x;
        this.y = y;
        this.showing = true;
        this.visible = true;
        this.hoveredIndex = -1;
        log.debug("Context menu shown at ({}, {})", x, y);
    }

    @Override
    public void hide() {
        this.showing = false;
        this.visible = false;
        this.hoveredIndex = -1;
    }

    @Override
    public boolean isShowing() {
        return showing;
    }

    private void recalculateSize() {
        int totalHeight = 4; // Top padding
        for (MenuItemImpl item : items) {
            totalHeight += item.isSeparator ? SEPARATOR_HEIGHT : ITEM_HEIGHT;
        }
        totalHeight += 4; // Bottom padding
        this.height = totalHeight;
    }

    public void setFontId(int fontId) {
        this.fontId = fontId;
    }

    @Override
    public void render(Renderer renderer) {
        if (!visible || !showing) {
            return;
        }

        // Draw shadow
        renderer.fillRoundedRect(x + 2, y + 2, width, height, 4, GLColour.withAlpha(GLColour.BLACK, 0.3f));

        // Draw background
        renderer.fillRoundedRect(x, y, width, height, 4, backgroundColor);

        // Draw border
        renderer.strokeRoundedRect(x, y, width, height, 4, borderColor, 1.0f);

        // Setup text rendering
        renderer.setFont(fontId, fontSize);

        // Render items
        int itemY = y + 4;
        for (int i = 0; i < items.size(); i++) {
            MenuItemImpl item = items.get(i);

            if (item.isSeparator) {
                // Draw separator line
                renderer.drawLine(x + ITEM_PADDING, itemY + SEPARATOR_HEIGHT / 2.0f,
                        x + width - ITEM_PADDING, itemY + SEPARATOR_HEIGHT / 2.0f, borderColor, 1.0f);
                itemY += SEPARATOR_HEIGHT;
            } else {
                // Draw hover highlight
                if (i == hoveredIndex && item.enabled) {
                    renderer.fillRoundedRect(x + 2, itemY, width - 4, ITEM_HEIGHT, 2,
                            GLColour.withAlpha(hoverColor, 0.3f));
                }

                // Draw item text
                float[] itemTextColor = item.enabled ? textColor : disabledColor;
                renderer.drawText(x + ITEM_PADDING, itemY + ITEM_HEIGHT / 2.0f, item.label, itemTextColor,
                        Renderer.ALIGN_LEFT | Renderer.ALIGN_MIDDLE);

                itemY += ITEM_HEIGHT;
            }
        }
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        if (!showing) {
            return false;
        }

        // Click outside menu hides it
        if (!contains(mx, my)) {
            if (InputConstants.isPress(action)) {
                hide();
                return true;
            }
            return false;
        }

        // Left click release triggers action
        if (InputConstants.isLeftButton(button) && InputConstants.isRelease(action)) {
            int clickedIndex = getItemIndexAt(my);
            if (clickedIndex >= 0 && clickedIndex < items.size()) {
                MenuItemImpl item = items.get(clickedIndex);
                if (item.enabled && !item.isSeparator && item.action != null) {
                    hide();
                    item.action.run();
                    return true;
                }
            }
        }

        return true; // Consume all clicks inside menu
    }

    @Override
    public boolean onMouseMove(int mx, int my) {
        if (!showing) {
            return false;
        }

        if (contains(mx, my)) {
            hoveredIndex = getItemIndexAt(my);
        } else {
            hoveredIndex = -1;
        }

        return contains(mx, my);
    }

    private int getItemIndexAt(int my) {
        int itemY = y + 4;
        for (int i = 0; i < items.size(); i++) {
            MenuItemImpl item = items.get(i);
            int itemHeight = item.isSeparator ? SEPARATOR_HEIGHT : ITEM_HEIGHT;

            if (my >= itemY && my < itemY + itemHeight) {
                return i;
            }
            itemY += itemHeight;
        }
        return -1;
    }

    /**
     * Internal menu item implementation.
     */
    private static class MenuItemImpl implements MenuItem {
        private final String label;
        private final Runnable action;
        private final boolean enabled;
        private final boolean isSeparator;

        MenuItemImpl(String label, Runnable action, boolean enabled, boolean isSeparator) {
            this.label = label;
            this.action = action;
            this.enabled = enabled;
            this.isSeparator = isSeparator;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public Runnable getAction() {
            return action;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean isSeparator() {
            return isSeparator;
        }
    }
}
