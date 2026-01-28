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
import ca.samanthaireland.lightning.engine.rendering.render2d.Label;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;

/**
 * Mock Label implementation for headless testing.
 */
public class MockLabel extends AbstractWindowComponent implements Label {

    private String text;
    private float fontSize = 16.0f;
    private float[] textColor;
    private int alignment;

    public MockLabel(int x, int y, String text) {
        super(x, y, 0, 16);
        this.text = text;
    }

    public MockLabel(int x, int y, String text, float fontSize) {
        super(x, y, 0, (int) fontSize);
        this.text = text;
        this.fontSize = fontSize;
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
    public float getFontSize() {
        return fontSize;
    }

    @Override
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        this.height = (int) fontSize;
    }

    @Override
    public float[] getTextColor() {
        return textColor;
    }

    @Override
    public void setTextColor(float[] color) {
        this.textColor = color;
    }

    @Override
    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public int getAlignment() {
        return alignment;
    }

    @Override
    public void render(Renderer renderer) {
        // No rendering in headless mode
    }
}
