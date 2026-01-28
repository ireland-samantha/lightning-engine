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
import ca.samanthaireland.lightning.engine.rendering.render2d.Button;
import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import lombok.extern.slf4j.Slf4j;

/**
 * A clickable button component.
 * OpenGL/NanoVG implementation of {@link Button}.
 */
@Slf4j
public class GLButton extends AbstractWindowComponent implements Button {

    private String text;
    private float fontSize;
    private float[] textColor;
    private float[] backgroundColor;
    private float[] hoverColor;
    private float[] pressedColor;
    private float[] borderColor;
    private float cornerRadius;
    private int fontId;

    private boolean hovered;
    private boolean pressed;
    private Runnable onClick;

    public GLButton(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
        this.fontSize = 14.0f;
        this.textColor = GLColour.TEXT_PRIMARY;
        this.backgroundColor = GLColour.BUTTON_BG;
        this.hoverColor = GLColour.BUTTON_HOVER;
        this.pressedColor = GLColour.BUTTON_PRESSED;
        this.borderColor = GLColour.BORDER;
        this.cornerRadius = 4.0f;
        this.fontId = -1;
        this.hovered = false;
        this.pressed = false;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setTextColor(float[] textColor) {
        this.textColor = textColor;
    }

    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setHoverColor(float[] hoverColor) {
        this.hoverColor = hoverColor;
    }

    public void setPressedColor(float[] pressedColor) {
        this.pressedColor = pressedColor;
    }

    public void setBorderColor(float[] borderColor) {
        this.borderColor = borderColor;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public void setFontId(int fontId) {
        this.fontId = fontId;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    @Override
    public void render(Renderer renderer) {
        if (!visible) {
            return;
        }

        // Determine background color based on state
        float[] bgColor = pressed ? pressedColor : (hovered ? hoverColor : backgroundColor);

        // Draw background (filled rounded rect)
        renderer.fillRoundedRect(x, y, width, height, cornerRadius, bgColor);

        // Draw border
        if (GuiConstants.BORDER_WIDTH > 0) {
            renderer.strokeRoundedRect(x, y, width, height, cornerRadius, borderColor, GuiConstants.BORDER_WIDTH);
        }

        // Draw text centered
        if (text != null && !text.isEmpty()) {
            renderer.setFont(fontId, fontSize);
            renderer.drawText(x + width / 2.0f, y + height / 2.0f, text, textColor,
                    Renderer.ALIGN_CENTER | Renderer.ALIGN_MIDDLE);
        }
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
}
