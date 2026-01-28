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
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.TextField;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A single-line text input field.
 * OpenGL implementation of {@link TextField}.
 */
@Slf4j
public class GLTextField extends AbstractWindowComponent implements TextField {

    private StringBuilder text = new StringBuilder();
    private String placeholder = "";
    private int cursorPosition = 0;
    private boolean focused = false;
    private float fontSize = 14.0f;
    private int fontId = -1;

    // Text selection
    private int selectionStart = -1;  // -1 means no selection
    private int selectionEnd = -1;
    private boolean dragging = false;
    private float[] selectionColor;

    // Context menu
    private GLContextMenu contextMenu;

    private float[] backgroundColor;
    private float[] focusedBackgroundColor;
    private float[] textColor;
    private float[] placeholderColor;
    private float[] borderColor;
    private float[] focusedBorderColor;
    private float[] cursorColor;

    private Consumer<String> onTextChanged;
    private Consumer<String> onSubmit;

    private long lastBlinkTime = 0;
    private boolean cursorVisible = true;
    private static final long CURSOR_BLINK_MS = 500;

    // Horizontal scrolling support
    private float scrollOffset = 0;
    private static final float TEXT_PADDING = 8.0f;
    private static final float SCROLL_MARGIN = 20.0f; // Keep cursor this far from edges

    // Cached NVG context for measuring
    private long cachedNvg = 0;

    public GLTextField(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.backgroundColor = GLColour.DARK_GRAY;
        this.focusedBackgroundColor = GLColour.withAlpha(GLColour.DARK_GRAY, 1.0f);
        this.textColor = GLColour.TEXT_PRIMARY;
        this.placeholderColor = GLColour.TEXT_SECONDARY;
        this.borderColor = GLColour.BORDER;
        this.focusedBorderColor = GLColour.ACCENT;
        this.cursorColor = GLColour.TEXT_PRIMARY;
        this.selectionColor = GLColour.withAlpha(GLColour.ACCENT, 0.4f);

        // Create context menu
        this.contextMenu = new GLContextMenu();
        setupContextMenu();
    }

    private void setupContextMenu() {
        contextMenu.clearItems();
        contextMenu.addItem("Cut", this::cutSelection, false);
        contextMenu.addItem("Copy", this::copySelection, false);
        contextMenu.addItem("Paste", this::pasteFromClipboard);
        contextMenu.addSeparator();
        contextMenu.addItem("Select All", this::selectAll);
    }

    private void updateContextMenuState() {
        boolean hasSelection = hasSelection();
        contextMenu.clearItems();
        contextMenu.addItem("Cut", this::cutSelection, hasSelection);
        contextMenu.addItem("Copy", this::copySelection, hasSelection);
        contextMenu.addItem("Paste", this::pasteFromClipboard);
        contextMenu.addSeparator();
        contextMenu.addItem("Select All", this::selectAll);
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String text) {
        this.text = new StringBuilder(text != null ? text : "");
        this.cursorPosition = this.text.length();
        clearSelection();
        if (onTextChanged != null) {
            onTextChanged.accept(getText());
        }
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setFontId(int fontId) {
        this.fontId = fontId;
    }

    public void setOnTextChanged(Consumer<String> onTextChanged) {
        this.onTextChanged = onTextChanged;
    }

    public void setOnSubmit(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            cursorVisible = true;
            lastBlinkTime = System.currentTimeMillis();
        }
    }

    @Override
    public void render(Renderer renderer) {
        if (!visible) {
            return;
        }

        // Update cursor blink
        if (focused) {
            long now = System.currentTimeMillis();
            if (now - lastBlinkTime > CURSOR_BLINK_MS) {
                cursorVisible = !cursorVisible;
                lastBlinkTime = now;
            }
        }

        // Draw background
        renderer.fillRoundedRect(x, y, width, height, 4, focused ? focusedBackgroundColor : backgroundColor);

        // Draw border
        renderer.strokeRoundedRect(x, y, width, height, 4, focused ? focusedBorderColor : borderColor,
                focused ? 2.0f : 1.0f);

        // Setup text rendering
        renderer.setFont(fontId, fontSize);

        float textY = y + height / 2.0f;
        float contentWidth = width - TEXT_PADDING * 2;

        // Calculate cursor position for scrolling
        String displayText = text.toString();
        String textBeforeCursor = displayText.substring(0, cursorPosition);
        float[] cursorBounds = new float[4];
        renderer.measureTextBounds(textBeforeCursor.isEmpty() ? "M" : textBeforeCursor, cursorBounds);
        float cursorX = textBeforeCursor.isEmpty() ? 0 : cursorBounds[2] - cursorBounds[0];

        // Update scroll offset to keep cursor visible
        if (focused) {
            updateScrollOffset(cursorX, contentWidth);
        }

        // Clip text to field bounds
        renderer.save();
        renderer.intersectClip(x + TEXT_PADDING, y, contentWidth, height);

        float textX = x + TEXT_PADDING - scrollOffset;

        // Draw selection highlight if there's a selection
        if (hasSelection() && focused) {
            int selStart = Math.min(selectionStart, selectionEnd);
            int selEnd = Math.max(selectionStart, selectionEnd);

            // Calculate selection bounds
            String beforeSel = displayText.substring(0, selStart);
            String selectedText = displayText.substring(selStart, selEnd);

            float[] beforeBounds = new float[4];
            float[] selBounds = new float[4];
            renderer.measureTextBounds(beforeSel.isEmpty() ? "" : beforeSel, beforeBounds);
            renderer.measureTextBounds(selectedText, selBounds);

            float selStartX = textX + (beforeSel.isEmpty() ? 0 : beforeBounds[2] - beforeBounds[0]);
            float selWidth = selBounds[2] - selBounds[0];

            // Draw selection rectangle
            renderer.fillRect(selStartX, y + 4, selWidth, height - 8, selectionColor);
        }

        // Draw text or placeholder
        if (displayText.isEmpty() && !placeholder.isEmpty() && !focused) {
            renderer.drawText(x + TEXT_PADDING, textY, placeholder, placeholderColor,
                    Renderer.ALIGN_LEFT | Renderer.ALIGN_MIDDLE);
        } else {
            renderer.drawText(textX, textY, displayText, textColor,
                    Renderer.ALIGN_LEFT | Renderer.ALIGN_MIDDLE);
        }

        // Draw cursor if focused (and no selection, or at cursor position)
        if (focused && cursorVisible && !hasSelection()) {
            float cursorScreenX = textX + cursorX;
            renderer.drawLine(cursorScreenX, y + 4, cursorScreenX, y + height - 4, cursorColor, 1.5f);
        }

        renderer.restore();

        // Context menu is rendered as overlay by the window, not here

        // Draw scroll indicators if text is clipped
        if (!displayText.isEmpty()) {
            float[] textBounds = new float[4];
            renderer.measureTextBounds(displayText, textBounds);
            float textWidth = textBounds[2] - textBounds[0];

            // Left fade indicator
            if (scrollOffset > 0) {
                renderer.fillRect(x + 2, y + 2, 6, height - 4, GLColour.withAlpha(borderColor, 0.5f));
            }

            // Right fade indicator
            if (textWidth - scrollOffset > contentWidth) {
                renderer.fillRect(x + width - 8, y + 2, 6, height - 4, GLColour.withAlpha(borderColor, 0.5f));
            }
        }
    }

    /**
     * Update scroll offset to keep cursor visible within the text field.
     */
    private void updateScrollOffset(float cursorX, float contentWidth) {
        // Cursor position relative to scroll
        float cursorScreenPos = cursorX - scrollOffset;

        // Scroll right if cursor is near right edge
        if (cursorScreenPos > contentWidth - SCROLL_MARGIN) {
            scrollOffset = cursorX - contentWidth + SCROLL_MARGIN;
        }

        // Scroll left if cursor is near left edge
        if (cursorScreenPos < SCROLL_MARGIN) {
            scrollOffset = Math.max(0, cursorX - SCROLL_MARGIN);
        }

        // Don't scroll past the beginning
        scrollOffset = Math.max(0, scrollOffset);
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        if (!visible) {
            return false;
        }

        // Handle context menu clicks first
        if (contextMenu.isShowing()) {
            if (contextMenu.onMouseClick(mx, my, button, action)) {
                return true;
            }
            // Click outside menu hides it
            if (InputConstants.isPress(action)) {
                contextMenu.hide();
            }
        }

        // Right-click shows context menu
        if (InputConstants.isRightButton(button) && InputConstants.isPress(action) && contains(mx, my)) {
            focused = true;
            updateContextMenuState();
            contextMenu.show(mx, my);
            GLContext.showOverlay(contextMenu);
            return true;
        }

        // Left-click focuses and positions cursor
        if (InputConstants.isLeftButton(button) && InputConstants.isPress(action)) {
            boolean wasFocused = focused;
            boolean clickedInside = contains(mx, my);
            focused = clickedInside;

            if (clickedInside) {
                // Hide context menu on left click
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                    GLContext.hideOverlay(contextMenu);
                }

                // Clear selection on click without shift
                clearSelection();

                // Position cursor at click location
                cursorPosition = getCharIndexAtX(mx);
                cursorVisible = true;
                lastBlinkTime = System.currentTimeMillis();
                dragging = true;
                selectionStart = cursorPosition;
            }

            return focused || wasFocused;
        }

        // Left-click release ends dragging
        if (InputConstants.isLeftButton(button) && InputConstants.isRelease(action)) {
            dragging = false;
            return focused;
        }

        return false;
    }

    @Override
    public boolean onMouseMove(int mx, int my) {
        if (!visible) {
            return false;
        }

        // Handle context menu hover
        if (contextMenu.isShowing()) {
            contextMenu.onMouseMove(mx, my);
        }

        // Handle text selection dragging
        if (dragging && focused) {
            int newPos = getCharIndexAtX(mx);
            if (newPos != cursorPosition) {
                selectionEnd = newPos;
                cursorPosition = newPos;
                resetCursorBlink();
            }
            return true;
        }

        return contains(mx, my);
    }

    /**
     * Get the character index at the given x coordinate.
     */
    private int getCharIndexAtX(int mx) {
        if (text.length() == 0) {
            return 0;
        }

        float localX = mx - x - TEXT_PADDING + scrollOffset;
        if (localX <= 0) {
            return 0;
        }

        // Binary search for the character position
        // This is a simplified approach - could use nvgTextGlyphPositions for accuracy
        int low = 0, high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            float charWidth = estimateTextWidth(text.substring(0, mid));
            if (charWidth <= localX) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Estimate text width without NVG context (approximate).
     * For accurate measurement, use nvgTextBounds during render.
     */
    private float estimateTextWidth(String str) {
        // Approximate: assume average char width based on font size
        return str.length() * fontSize * 0.5f;
    }

    @Override
    public boolean onKeyPress(int key, int action) {
        return onKeyPress(key, action, 0);
    }

    @Override
    public boolean onKeyPress(int key, int action, int mods) {
        if (!focused || InputConstants.isRelease(action)) { // Only handle press and repeat
            return false;
        }

        // Check for Ctrl (Windows/Linux) or Cmd (macOS) modifier
        boolean isCtrlOrCmd = (mods & (GLFW_MOD_CONTROL | GLFW_MOD_SUPER)) != 0;
        boolean isShift = (mods & GLFW_MOD_SHIFT) != 0;

        // Handle copy/paste shortcuts
        if (isCtrlOrCmd) {
            switch (key) {
                case GLFW_KEY_V: // Paste
                    pasteFromClipboard();
                    return true;

                case GLFW_KEY_C: // Copy
                    copySelection();
                    return true;

                case GLFW_KEY_X: // Cut
                    cutSelection();
                    return true;

                case GLFW_KEY_A: // Select all
                    selectAll();
                    return true;
            }
        }

        switch (key) {
            case GLFW_KEY_BACKSPACE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition > 0) {
                    text.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                    if (onTextChanged != null) {
                        onTextChanged.accept(getText());
                    }
                }
                resetCursorBlink();
                return true;

            case GLFW_KEY_DELETE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    text.deleteCharAt(cursorPosition);
                    if (onTextChanged != null) {
                        onTextChanged.accept(getText());
                    }
                }
                resetCursorBlink();
                return true;

            case GLFW_KEY_LEFT:
                if (isShift) {
                    // Extend selection left
                    if (!hasSelection()) {
                        selectionStart = cursorPosition;
                    }
                    if (cursorPosition > 0) {
                        cursorPosition--;
                        selectionEnd = cursorPosition;
                    }
                } else {
                    // Move cursor, clear selection
                    if (hasSelection()) {
                        cursorPosition = Math.min(selectionStart, selectionEnd);
                        clearSelection();
                    } else if (cursorPosition > 0) {
                        cursorPosition--;
                    }
                }
                resetCursorBlink();
                return true;

            case GLFW_KEY_RIGHT:
                if (isShift) {
                    // Extend selection right
                    if (!hasSelection()) {
                        selectionStart = cursorPosition;
                    }
                    if (cursorPosition < text.length()) {
                        cursorPosition++;
                        selectionEnd = cursorPosition;
                    }
                } else {
                    // Move cursor, clear selection
                    if (hasSelection()) {
                        cursorPosition = Math.max(selectionStart, selectionEnd);
                        clearSelection();
                    } else if (cursorPosition < text.length()) {
                        cursorPosition++;
                    }
                }
                resetCursorBlink();
                return true;

            case GLFW_KEY_HOME:
                if (isShift) {
                    if (!hasSelection()) {
                        selectionStart = cursorPosition;
                    }
                    cursorPosition = 0;
                    selectionEnd = cursorPosition;
                } else {
                    clearSelection();
                    cursorPosition = 0;
                }
                resetCursorBlink();
                return true;

            case GLFW_KEY_END:
                if (isShift) {
                    if (!hasSelection()) {
                        selectionStart = cursorPosition;
                    }
                    cursorPosition = text.length();
                    selectionEnd = cursorPosition;
                } else {
                    clearSelection();
                    cursorPosition = text.length();
                }
                resetCursorBlink();
                return true;

            case GLFW_KEY_ENTER:
                if (onSubmit != null) {
                    onSubmit.accept(getText());
                }
                return true;

            default:
                return false;
        }
    }

    // === Selection Methods ===

    /**
     * Check if there is a text selection.
     */
    public boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
    }

    /**
     * Get the selected text.
     */
    public String getSelectedText() {
        if (!hasSelection()) {
            return "";
        }
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return text.substring(start, end);
    }

    /**
     * Clear the current selection.
     */
    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    /**
     * Select all text.
     */
    public void selectAll() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
        resetCursorBlink();
    }

    /**
     * Delete the selected text.
     */
    private void deleteSelection() {
        if (!hasSelection()) {
            return;
        }
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        text.delete(start, end);
        cursorPosition = start;
        clearSelection();
        if (onTextChanged != null) {
            onTextChanged.accept(getText());
        }
    }

    /**
     * Copy selected text to clipboard.
     */
    private void copySelection() {
        try {
            String textToCopy = hasSelection() ? getSelectedText() : text.toString();
            if (!textToCopy.isEmpty()) {
                glfwSetClipboardString(MemoryUtil.NULL, textToCopy);
                log.debug("Copied {} characters to clipboard", textToCopy.length());
            }
        } catch (Exception e) {
            log.warn("Failed to copy to clipboard: {}", e.getMessage());
        }
    }

    /**
     * Cut selected text to clipboard.
     */
    private void cutSelection() {
        if (hasSelection()) {
            copySelection();
            deleteSelection();
        } else {
            // Cut all if no selection
            copySelection();
            clearText();
        }
    }

    /**
     * Paste text from the system clipboard at the current cursor position.
     */
    private void pasteFromClipboard() {
        try {
            String clipboardText = glfwGetClipboardString(MemoryUtil.NULL);
            if (clipboardText != null && !clipboardText.isEmpty()) {
                // Filter out control characters and newlines
                String filtered = clipboardText.replaceAll("[\\p{Cntrl}]", "");
                if (!filtered.isEmpty()) {
                    // Delete selection first if any
                    if (hasSelection()) {
                        deleteSelection();
                    }
                    text.insert(cursorPosition, filtered);
                    cursorPosition += filtered.length();
                    resetCursorBlink();
                    if (onTextChanged != null) {
                        onTextChanged.accept(getText());
                    }
                    log.debug("Pasted {} characters", filtered.length());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to paste from clipboard: {}", e.getMessage());
        }
    }

    /**
     * Clear all text.
     */
    private void clearText() {
        text.setLength(0);
        cursorPosition = 0;
        clearSelection();
        resetCursorBlink();
        if (onTextChanged != null) {
            onTextChanged.accept(getText());
        }
    }

    @Override
    public boolean onCharInput(int codepoint) {
        if (!focused) {
            return false;
        }

        char c = (char) codepoint;
        if (Character.isISOControl(c)) {
            return false;
        }

        // Delete selection first if any
        if (hasSelection()) {
            deleteSelection();
        }

        text.insert(cursorPosition, c);
        cursorPosition++;
        resetCursorBlink();

        if (onTextChanged != null) {
            onTextChanged.accept(getText());
        }

        return true;
    }

    private void resetCursorBlink() {
        cursorVisible = true;
        lastBlinkTime = System.currentTimeMillis();
    }

    @Override
    public void setTextColor(float[] color) {
        this.textColor = color;
    }

    @Override
    public void setBackgroundColor(float[] color) {
        this.backgroundColor = color;
    }

    @Override
    public void setOnChange(Runnable onChange) {
        this.onTextChanged = text -> onChange.run();
    }

    @Override
    public void clearFocus() {
        this.focused = false;
    }
}
