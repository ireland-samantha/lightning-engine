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
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.TextField;
import ca.samanthaireland.lightning.engine.rendering.testing.KeyCodes;

import java.util.function.Consumer;

/**
 * Mock TextField implementation for headless testing.
 */
public class MockTextField extends AbstractWindowComponent implements TextField {

    private StringBuilder text = new StringBuilder();
    private String placeholder = "";
    private int cursorPosition = 0;
    private boolean focused = false;
    private float fontSize = 14.0f;
    private float[] textColor;
    private float[] backgroundColor;

    private Consumer<String> onTextChanged;
    private Consumer<String> onSubmit;
    private Runnable onChange;

    public MockTextField(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public String getText() {
        return text.toString();
    }

    @Override
    public void setText(String text) {
        this.text = new StringBuilder(text != null ? text : "");
        this.cursorPosition = this.text.length();
        if (onTextChanged != null) {
            onTextChanged.accept(getText());
        }
        if (onChange != null) {
            onChange.run();
        }
    }

    @Override
    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
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
        this.onChange = onChange;
    }

    public void setOnTextChanged(Consumer<String> onTextChanged) {
        this.onTextChanged = onTextChanged;
    }

    public void setOnSubmit(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit;
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
        if (!visible) {
            return false;
        }

        if (InputConstants.isLeftButton(button) && InputConstants.isPress(action)) {
            boolean wasFocused = focused;
            focused = contains(mx, my);

            if (focused && !wasFocused) {
                cursorPosition = text.length();
            }

            return focused || wasFocused;
        }
        return false;
    }

    @Override
    public boolean onKeyPress(int key, int action) {
        return onKeyPress(key, action, 0);
    }

    @Override
    public boolean onKeyPress(int key, int action, int mods) {
        if (!focused || InputConstants.isRelease(action)) {
            return false;
        }

        // Handle Ctrl/Cmd modifiers for shortcuts
        boolean isCtrlOrCmd = (mods & (KeyCodes.MOD_CONTROL | KeyCodes.MOD_SUPER)) != 0;
        if (isCtrlOrCmd && key == KeyCodes.KEY_A) {
            // Select all - for mock, just move cursor to end
            cursorPosition = text.length();
            return true;
        }

        switch (key) {
            case KeyCodes.KEY_BACKSPACE:
                if (cursorPosition > 0) {
                    text.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                    notifyChange();
                }
                return true;

            case KeyCodes.KEY_DELETE:
                if (cursorPosition < text.length()) {
                    text.deleteCharAt(cursorPosition);
                    notifyChange();
                }
                return true;

            case KeyCodes.KEY_LEFT:
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                return true;

            case KeyCodes.KEY_RIGHT:
                if (cursorPosition < text.length()) {
                    cursorPosition++;
                }
                return true;

            case KeyCodes.KEY_HOME:
                cursorPosition = 0;
                return true;

            case KeyCodes.KEY_END:
                cursorPosition = text.length();
                return true;

            case KeyCodes.KEY_ENTER:
                if (onSubmit != null) {
                    onSubmit.accept(getText());
                }
                return true;

            default:
                return false;
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

        text.insert(cursorPosition, c);
        cursorPosition++;
        notifyChange();

        return true;
    }

    private void notifyChange() {
        if (onTextChanged != null) {
            onTextChanged.accept(getText());
        }
        if (onChange != null) {
            onChange.run();
        }
    }
}
