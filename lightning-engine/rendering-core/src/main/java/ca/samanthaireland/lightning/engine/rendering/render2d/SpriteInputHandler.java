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

/**
 * Interface for handling input events on sprites.
 * Implement this interface to make sprites interactive.
 *
 * <p>Example usage:
 * <pre>{@code
 * sprite.setInputHandler(new SpriteInputHandler() {
 *     @Override
 *     public boolean onMouseClick(Sprite sprite, int button, int action) {
 *         if (button == 0 && action == 1) { // Left click press
 *             System.out.println("Sprite clicked: " + sprite.getId());
 *             return true; // Event consumed
 *         }
 *         return false;
 *     }
 * });
 * }</pre>
 */
public interface SpriteInputHandler {

    /**
     * Called when a mouse button is clicked on the sprite.
     *
     * @param sprite the sprite that was clicked
     * @param button the mouse button (0=left, 1=right, 2=middle)
     * @param action 1=press, 0=release
     * @return true if the event was consumed, false to propagate
     */
    default boolean onMouseClick(Sprite sprite, int button, int action) {
        return false;
    }

    /**
     * Called when the mouse cursor enters the sprite's bounds.
     *
     * @param sprite the sprite being entered
     */
    default void onMouseEnter(Sprite sprite) {
    }

    /**
     * Called when the mouse cursor exits the sprite's bounds.
     *
     * @param sprite the sprite being exited
     */
    default void onMouseExit(Sprite sprite) {
    }

    /**
     * Called when the mouse moves while over the sprite.
     *
     * @param sprite  the sprite the mouse is over
     * @param mouseX  current mouse x position
     * @param mouseY  current mouse y position
     * @return true if the event was consumed, false to propagate
     */
    default boolean onMouseMove(Sprite sprite, int mouseX, int mouseY) {
        return false;
    }

    /**
     * Called when the mouse scrolls while over the sprite.
     *
     * @param sprite  the sprite the mouse is over
     * @param scrollX horizontal scroll amount
     * @param scrollY vertical scroll amount
     * @return true if the event was consumed, false to propagate
     */
    default boolean onMouseScroll(Sprite sprite, double scrollX, double scrollY) {
        return false;
    }

    /**
     * Called when a key is pressed while the sprite has focus.
     * Note: Sprites must be focusable to receive keyboard events.
     *
     * @param sprite the focused sprite
     * @param key    the key code (GLFW key constants)
     * @param action 1=press, 0=release, 2=repeat
     * @param mods   modifier keys bitmask
     * @return true if the event was consumed, false to propagate
     */
    default boolean onKeyPress(Sprite sprite, int key, int action, int mods) {
        return false;
    }

    /**
     * Called when a character is typed while the sprite has focus.
     * Note: Sprites must be focusable to receive keyboard events.
     *
     * @param sprite    the focused sprite
     * @param codepoint the unicode codepoint
     * @return true if the event was consumed, false to propagate
     */
    default boolean onCharInput(Sprite sprite, int codepoint) {
        return false;
    }
}
