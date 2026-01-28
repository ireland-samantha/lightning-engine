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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A 2D sprite with position, size, and optional input handling.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sprite {

    private int id;
    private String texturePath;
    private int x;
    private int y;
    private int sizeX;
    private int sizeY;

    /**
     * Optional input handler for this sprite.
     * When set, the sprite can receive mouse and keyboard events.
     */
    @Builder.Default
    private SpriteInputHandler inputHandler = null;

    /**
     * Whether this sprite can receive keyboard focus.
     * Only focusable sprites can receive keyboard events.
     */
    @Builder.Default
    private boolean focusable = false;

    /**
     * Z-index for rendering and input event ordering.
     * Higher values are rendered on top and receive events first.
     */
    @Builder.Default
    private int zIndex = 0;

    /**
     * Check if a point is within this sprite's bounds.
     *
     * @param px x coordinate to check
     * @param py y coordinate to check
     * @return true if the point is inside the sprite
     */
    public boolean contains(int px, int py) {
        return px >= x && px < x + sizeX && py >= y && py < y + sizeY;
    }

    /**
     * Check if this sprite has an input handler.
     */
    public boolean hasInputHandler() {
        return inputHandler != null;
    }
}
