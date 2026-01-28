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
import ca.samanthaireland.lightning.engine.rendering.render2d.Button;
import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;

/**
 * Mock Button implementation for headless testing.
 */
public class MockButton extends AbstractWindowComponent implements Button {

    private String text;
    private float fontSize = 14.0f;
    private float[] textColor;
    private float[] backgroundColor;
    private float[] hoverColor;
    private float[] pressedColor;
    private float[] borderColor;
    private float cornerRadius = 4.0f;
    private Runnable onClick;
    private boolean hovered;
    private boolean pressed;

    public MockButton(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
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
    public void setHoverColor(float[] color) {
        this.hoverColor = color;
    }

    @Override
    public void setPressedColor(float[] color) {
        this.pressedColor = color;
    }

    @Override
    public void setBorderColor(float[] color) {
        this.borderColor = color;
    }

    @Override
    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
    }

    @Override
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
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

        boolean wasPressed = pressed;

        if (contains(mx, my)) {
            if (InputConstants.isLeftButton(button)) {
                if (InputConstants.isPress(action)) {
                    pressed = true;
                    return true;
                } else if (InputConstants.isRelease(action)) {
                    if (pressed && onClick != null) {
                        onClick.run();
                    }
                    pressed = false;
                    return true;
                }
            }
        } else {
            pressed = false;
        }

        return wasPressed != pressed;
    }

    @Override
    public boolean onMouseMove(int mx, int my) {
        if (!visible) {
            return false;
        }

        boolean wasHovered = hovered;
        hovered = contains(mx, my);

        if (!hovered) {
            pressed = false;
        }

        return wasHovered != hovered;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public float getFontSize() {
        return fontSize;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }
}
