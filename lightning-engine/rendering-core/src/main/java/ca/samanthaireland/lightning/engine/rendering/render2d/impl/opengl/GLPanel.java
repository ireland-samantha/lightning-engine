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
import ca.samanthaireland.lightning.engine.rendering.render2d.Panel;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * A container panel that can hold child components.
 * OpenGL/NanoVG implementation of {@link Panel}.
 */
@Slf4j
public class GLPanel extends AbstractWindowComponent implements Panel {

    private final List<WindowComponent> children = new ArrayList<>();
    private float[] backgroundColor;
    private float[] borderColor;
    private float borderWidth;
    private float cornerRadius;
    private String title;
    private float titleFontSize;
    private int titleFontId;

    // Scrolling support
    private boolean scrollableX = false;
    private boolean scrollableY = false;
    private float scrollX = 0;
    private float scrollY = 0;
    private static final float SCROLL_SPEED = 20.0f;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_MIN_LENGTH = 20;

    public GLPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.backgroundColor = GLColour.PANEL_BG;
        this.borderColor = GLColour.BORDER;
        this.borderWidth = 1.0f;
        this.cornerRadius = 4.0f;
        this.title = null;
        this.titleFontSize = 14.0f;
        this.titleFontId = -1;
    }

    public void addChild(WindowComponent child) {
        children.add(child);
    }

    public void removeChild(WindowComponent child) {
        children.remove(child);
    }

    public void clearChildren() {
        children.clear();
    }

    public List<WindowComponent> getChildren() {
        return children;
    }

    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBorderColor(float[] borderColor) {
        this.borderColor = borderColor;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitleFontSize(float titleFontSize) {
        this.titleFontSize = titleFontSize;
    }

    public void setTitleFontId(int titleFontId) {
        this.titleFontId = titleFontId;
    }

    // === Scrolling Implementation ===

    @Override
    public void setScrollable(boolean horizontal, boolean vertical) {
        this.scrollableX = horizontal;
        this.scrollableY = vertical;
    }

    @Override
    public boolean isScrollableHorizontal() {
        return scrollableX;
    }

    @Override
    public boolean isScrollableVertical() {
        return scrollableY;
    }

    @Override
    public float getScrollX() {
        return scrollX;
    }

    @Override
    public float getScrollY() {
        return scrollY;
    }

    @Override
    public void setScrollPosition(float x, float y) {
        this.scrollX = Math.max(0, x);
        this.scrollY = Math.max(0, y);
        clampScroll();
    }

    @Override
    public int getContentWidth() {
        int maxRight = 0;
        for (WindowComponent child : children) {
            int childRight = child.getX() + child.getWidth();
            if (childRight > maxRight) {
                maxRight = childRight;
            }
        }
        return maxRight;
    }

    @Override
    public int getContentHeight() {
        int maxBottom = 0;
        for (WindowComponent child : children) {
            int childBottom = child.getY() + child.getHeight();
            if (childBottom > maxBottom) {
                maxBottom = childBottom;
            }
        }
        return maxBottom;
    }

    @Override
    public void scrollToChild(WindowComponent child) {
        if (!children.contains(child)) {
            return;
        }

        int contentOffsetY = (title != null && !title.isEmpty()) ? (int) (titleFontSize + 12) : 0;
        int viewportWidth = width - (scrollableY ? SCROLLBAR_WIDTH : 0);
        int viewportHeight = height - contentOffsetY - (scrollableX ? SCROLLBAR_WIDTH : 0);

        // Scroll horizontally if needed
        if (scrollableX) {
            int childLeft = child.getX();
            int childRight = childLeft + child.getWidth();

            if (childLeft - scrollX < 0) {
                scrollX = childLeft;
            } else if (childRight - scrollX > viewportWidth) {
                scrollX = childRight - viewportWidth;
            }
        }

        // Scroll vertically if needed
        if (scrollableY) {
            int childTop = child.getY();
            int childBottom = childTop + child.getHeight();

            if (childTop - scrollY < 0) {
                scrollY = childTop;
            } else if (childBottom - scrollY > viewportHeight) {
                scrollY = childBottom - viewportHeight;
            }
        }

        clampScroll();
    }

    /**
     * Clamp scroll values to valid range.
     */
    private void clampScroll() {
        int contentOffsetY = (title != null && !title.isEmpty()) ? (int) (titleFontSize + 12) : 0;
        int viewportWidth = width - (scrollableY ? SCROLLBAR_WIDTH : 0);
        int viewportHeight = height - contentOffsetY - (scrollableX ? SCROLLBAR_WIDTH : 0);

        int contentWidth = getContentWidth();
        int contentHeight = getContentHeight();

        float maxScrollX = Math.max(0, contentWidth - viewportWidth);
        float maxScrollY = Math.max(0, contentHeight - viewportHeight);

        scrollX = Math.max(0, Math.min(scrollX, maxScrollX));
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
    }

    @Override
    public void render(Renderer renderer) {
        if (!visible) {
            return;
        }

        // Draw background
        renderer.fillRoundedRect(x, y, width, height, cornerRadius, backgroundColor);

        // Draw border
        if (borderWidth > 0) {
            renderer.strokeRoundedRect(x, y, width, height, cornerRadius, borderColor, borderWidth);
        }

        // Draw title if set
        int contentOffsetY = 0;
        if (title != null && !title.isEmpty()) {
            renderer.setFont(titleFontId, titleFontSize);
            renderer.drawText(x + 8, y + 6, title, GLColour.TEXT_PRIMARY,
                    Renderer.ALIGN_LEFT | Renderer.ALIGN_TOP);

            // Draw separator line
            contentOffsetY = (int) (titleFontSize + 12);
            renderer.drawLine(x + 1, y + contentOffsetY, x + width - 1, y + contentOffsetY, borderColor, 1);
        }

        // Calculate viewport dimensions (accounting for scrollbars)
        int viewportWidth = width - 2 - (scrollableY ? SCROLLBAR_WIDTH : 0);
        int viewportHeight = height - contentOffsetY - 2 - (scrollableX ? SCROLLBAR_WIDTH : 0);

        // Render children with scroll offset
        renderer.save();
        renderer.intersectClip(x + 1, y + contentOffsetY + 1, viewportWidth, viewportHeight);
        renderer.translate(-scrollX, -scrollY);

        for (WindowComponent child : children) {
            child.render(renderer);
        }

        renderer.restore();

        // Render scrollbars if needed
        if (scrollableY) {
            renderVerticalScrollbar(renderer, contentOffsetY, viewportHeight);
        }
        if (scrollableX) {
            renderHorizontalScrollbar(renderer, contentOffsetY, viewportWidth);
        }
    }

    /**
     * Render vertical scrollbar.
     */
    private void renderVerticalScrollbar(Renderer renderer, int contentOffsetY, int viewportHeight) {
        int contentHeight = getContentHeight();
        if (contentHeight <= viewportHeight) {
            return; // No scrollbar needed
        }

        int scrollbarX = x + width - SCROLLBAR_WIDTH - 1;
        int scrollbarY = y + contentOffsetY + 1;
        int scrollbarHeight = viewportHeight;

        // Draw scrollbar track
        renderer.fillRoundedRect(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight, 2,
                GLColour.withAlpha(GLColour.DARK_GRAY, 0.3f));

        // Calculate thumb size and position
        float ratio = (float) viewportHeight / contentHeight;
        int thumbHeight = Math.max(SCROLLBAR_MIN_LENGTH, (int) (scrollbarHeight * ratio));
        float maxScroll = contentHeight - viewportHeight;
        float scrollRatio = maxScroll > 0 ? scrollY / maxScroll : 0;
        int thumbY = scrollbarY + (int) ((scrollbarHeight - thumbHeight) * scrollRatio);

        // Draw scrollbar thumb
        renderer.fillRoundedRect(scrollbarX + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbHeight, 2,
                GLColour.withAlpha(GLColour.TEXT_SECONDARY, 0.6f));
    }

    /**
     * Render horizontal scrollbar.
     */
    private void renderHorizontalScrollbar(Renderer renderer, int contentOffsetY, int viewportWidth) {
        int contentWidth = getContentWidth();
        if (contentWidth <= viewportWidth) {
            return; // No scrollbar needed
        }

        int scrollbarX = x + 1;
        int scrollbarY = y + height - SCROLLBAR_WIDTH - 1;
        int scrollbarWidth = viewportWidth;

        // Draw scrollbar track
        renderer.fillRoundedRect(scrollbarX, scrollbarY, scrollbarWidth, SCROLLBAR_WIDTH, 2,
                GLColour.withAlpha(GLColour.DARK_GRAY, 0.3f));

        // Calculate thumb size and position
        float ratio = (float) viewportWidth / contentWidth;
        int thumbWidth = Math.max(SCROLLBAR_MIN_LENGTH, (int) (scrollbarWidth * ratio));
        float maxScroll = contentWidth - viewportWidth;
        float scrollRatio = maxScroll > 0 ? scrollX / maxScroll : 0;
        int thumbX = scrollbarX + (int) ((scrollbarWidth - thumbWidth) * scrollRatio);

        // Draw scrollbar thumb
        renderer.fillRoundedRect(thumbX, scrollbarY + 1, thumbWidth, SCROLLBAR_WIDTH - 2, 2,
                GLColour.withAlpha(GLColour.TEXT_SECONDARY, 0.6f));
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        // Adjust mouse coordinates for scroll offset
        int adjustedMx = (int) (mx + scrollX);
        int adjustedMy = (int) (my + scrollY);

        // On left-click press, clear focus from all text fields first
        // This ensures only the clicked text field gets focus
        if (InputConstants.isLeftButton(button) && InputConstants.isPress(action)) {
            clearTextFieldFocus();
        }

        // Propagate to children in reverse order (top-most first)
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).onMouseClick(adjustedMx, adjustedMy, button, action)) {
                return true;
            }
        }
        return true; // Panel consumes the click even if no child handles it
    }

    /**
     * Clear focus from all TextField children (recursively).
     */
    private void clearTextFieldFocus() {
        for (WindowComponent child : children) {
            if (child instanceof GLTextField textField) {
                textField.clearFocus();
            } else if (child instanceof GLPanel childPanel) {
                childPanel.clearTextFieldFocus();
            }
        }
    }

    @Override
    public boolean onMouseMove(int mx, int my) {
        if (!visible) {
            return false;
        }

        // Adjust mouse coordinates for scroll offset
        int adjustedMx = (int) (mx + scrollX);
        int adjustedMy = (int) (my + scrollY);

        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).onMouseMove(adjustedMx, adjustedMy);
        }
        return contains(mx, my);
    }

    @Override
    public boolean onMouseScroll(int mx, int my, double scrollDeltaX, double scrollDeltaY) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        // Adjust mouse coordinates for scroll offset and let children handle first
        int adjustedMx = (int) (mx + scrollX);
        int adjustedMy = (int) (my + scrollY);

        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).onMouseScroll(adjustedMx, adjustedMy, scrollDeltaX, scrollDeltaY)) {
                return true;
            }
        }

        // Handle panel scrolling if children didn't consume the scroll
        if (scrollableY && scrollDeltaY != 0) {
            scrollY -= (float) (scrollDeltaY * SCROLL_SPEED);
            clampScroll();
            return true;
        }
        if (scrollableX && scrollDeltaX != 0) {
            scrollX -= (float) (scrollDeltaX * SCROLL_SPEED);
            clampScroll();
            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyPress(int key, int action) {
        return onKeyPress(key, action, 0);
    }

    @Override
    public boolean onKeyPress(int key, int action, int mods) {
        if (!visible) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).onKeyPress(key, action, mods)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharInput(int codepoint) {
        if (!visible) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).onCharInput(codepoint)) {
                return true;
            }
        }
        return false;
    }
}
