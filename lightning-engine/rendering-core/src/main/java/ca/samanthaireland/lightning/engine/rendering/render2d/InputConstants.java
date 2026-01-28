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
 * Constants for mouse and keyboard input events.
 *
 * <p>These values correspond to GLFW constants but are abstracted
 * to avoid direct GLFW dependency in component code.
 *
 * <p>Usage example:
 * <pre>{@code
 * if (button == InputConstants.BUTTON_LEFT && action == InputConstants.ACTION_PRESS) {
 *     // Handle left mouse button press
 * }
 * }</pre>
 */
public final class InputConstants {

    private InputConstants() {
        // Constants class
    }

    // ========== Mouse Buttons ==========

    /**
     * Left mouse button (primary click).
     */
    public static final int BUTTON_LEFT = 0;

    /**
     * Right mouse button (context menu).
     */
    public static final int BUTTON_RIGHT = 1;

    /**
     * Middle mouse button (scroll wheel click).
     */
    public static final int BUTTON_MIDDLE = 2;

    // ========== Actions ==========

    /**
     * Key/button was released.
     */
    public static final int ACTION_RELEASE = 0;

    /**
     * Key/button was pressed.
     */
    public static final int ACTION_PRESS = 1;

    /**
     * Key is being held down (repeat event).
     */
    public static final int ACTION_REPEAT = 2;

    // ========== Helper Methods ==========

    /**
     * Check if the action is a press event.
     *
     * @param action the action code
     * @return true if the action is a press
     */
    public static boolean isPress(int action) {
        return action == ACTION_PRESS;
    }

    /**
     * Check if the action is a release event.
     *
     * @param action the action code
     * @return true if the action is a release
     */
    public static boolean isRelease(int action) {
        return action == ACTION_RELEASE;
    }

    /**
     * Check if the button is the left mouse button.
     *
     * @param button the button code
     * @return true if the button is the left button
     */
    public static boolean isLeftButton(int button) {
        return button == BUTTON_LEFT;
    }

    /**
     * Check if the button is the right mouse button.
     *
     * @param button the button code
     * @return true if the button is the right button
     */
    public static boolean isRightButton(int button) {
        return button == BUTTON_RIGHT;
    }
}
