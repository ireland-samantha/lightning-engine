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


package ca.samanthaireland.lightning.engine.rendering.testing;

import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for complex interaction sequences.
 * Mirrors Selenium's Actions class.
 *
 * <p>Example usage:
 * <pre>{@code
 * driver.actions()
 *     .moveToElement(button)
 *     .click()
 *     .pause(Duration.ofMillis(100))
 *     .sendKeys("hello")
 *     .perform();
 * }</pre>
 */
public class Actions {

    private final GuiDriver driver;
    private final List<Runnable> actionQueue = new ArrayList<>();
    private int currentX = 0;
    private int currentY = 0;

    /**
     * Create a new Actions builder.
     * @param driver the driver to use for actions
     */
    public Actions(GuiDriver driver) {
        this.driver = driver;
    }

    // ========== Mouse Movement ==========

    /**
     * Move to the center of an element.
     * @param element the target element
     * @return this builder for chaining
     */
    public Actions moveToElement(GuiElement element) {
        actionQueue.add(() -> {
            currentX = element.getX() + element.getWidth() / 2;
            currentY = element.getY() + element.getHeight() / 2;
            driver.moveMouse(currentX, currentY);
        });
        return this;
    }

    /**
     * Move to a specific position on an element.
     * @param element the target element
     * @param offsetX x offset from element's top-left
     * @param offsetY y offset from element's top-left
     * @return this builder for chaining
     */
    public Actions moveToElement(GuiElement element, int offsetX, int offsetY) {
        actionQueue.add(() -> {
            currentX = element.getX() + offsetX;
            currentY = element.getY() + offsetY;
            driver.moveMouse(currentX, currentY);
        });
        return this;
    }

    /**
     * Move by an offset from current position.
     * @param dx x offset
     * @param dy y offset
     * @return this builder for chaining
     */
    public Actions moveByOffset(int dx, int dy) {
        actionQueue.add(() -> {
            currentX += dx;
            currentY += dy;
            driver.moveMouse(currentX, currentY);
        });
        return this;
    }

    // ========== Mouse Clicks ==========

    /**
     * Click at current position.
     * @return this builder for chaining
     */
    public Actions click() {
        actionQueue.add(() -> driver.click(currentX, currentY));
        return this;
    }

    /**
     * Click on an element.
     * @param element the target element
     * @return this builder for chaining
     */
    public Actions click(GuiElement element) {
        return moveToElement(element).click();
    }

    /**
     * Double-click at current position.
     * @return this builder for chaining
     */
    public Actions doubleClick() {
        actionQueue.add(() -> {
            driver.click(currentX, currentY);
            driver.click(currentX, currentY);
        });
        return this;
    }

    /**
     * Double-click on an element.
     * @param element the target element
     * @return this builder for chaining
     */
    public Actions doubleClick(GuiElement element) {
        return moveToElement(element).doubleClick();
    }

    /**
     * Right-click at current position.
     * @return this builder for chaining
     */
    public Actions rightClick() {
        actionQueue.add(() -> {
            List<WindowComponent> components = driver.getWindow().getComponents();
            for (int i = components.size() - 1; i >= 0; i--) {
                var component = components.get(i);
                if (component.onMouseClick(currentX, currentY, 1, 1)) {
                    component.onMouseClick(currentX, currentY, 1, 0);
                    return;
                }
            }
        });
        return this;
    }

    /**
     * Click and hold at current position.
     * @return this builder for chaining
     */
    public Actions clickAndHold() {
        actionQueue.add(() -> {
            List<WindowComponent> components = driver.getWindow().getComponents();
            for (int i = components.size() - 1; i >= 0; i--) {
                if (components.get(i).onMouseClick(currentX, currentY, 0, 1)) {
                    return;
                }
            }
        });
        return this;
    }

    /**
     * Click and hold on an element.
     * @param element the target element
     * @return this builder for chaining
     */
    public Actions clickAndHold(GuiElement element) {
        return moveToElement(element).clickAndHold();
    }

    /**
     * Release the mouse button.
     * @return this builder for chaining
     */
    public Actions release() {
        actionQueue.add(() -> {
            List<WindowComponent> components = driver.getWindow().getComponents();
            for (int i = components.size() - 1; i >= 0; i--) {
                if (components.get(i).onMouseClick(currentX, currentY, 0, 0)) {
                    return;
                }
            }
        });
        return this;
    }

    /**
     * Drag from source to target element.
     * @param source the source element
     * @param target the target element
     * @return this builder for chaining
     */
    public Actions dragAndDrop(GuiElement source, GuiElement target) {
        return clickAndHold(source)
            .moveToElement(target)
            .release();
    }

    // ========== Keyboard ==========

    /**
     * Press and hold a key.
     * @param keyCode the key code (from KeyCodes)
     * @return this builder for chaining
     */
    public Actions keyDown(int keyCode) {
        actionQueue.add(() -> {
            List<WindowComponent> components = driver.getWindow().getComponents();
            for (int i = components.size() - 1; i >= 0; i--) {
                if (components.get(i).onKeyPress(keyCode, KeyCodes.ACTION_PRESS)) {
                    return;
                }
            }
        });
        return this;
    }

    /**
     * Release a key.
     * @param keyCode the key code (from KeyCodes)
     * @return this builder for chaining
     */
    public Actions keyUp(int keyCode) {
        actionQueue.add(() -> {
            List<WindowComponent> components = driver.getWindow().getComponents();
            for (int i = components.size() - 1; i >= 0; i--) {
                if (components.get(i).onKeyPress(keyCode, KeyCodes.ACTION_RELEASE)) {
                    return;
                }
            }
        });
        return this;
    }

    /**
     * Type a string of characters.
     * @param text the text to type
     * @return this builder for chaining
     */
    public Actions sendKeys(String text) {
        actionQueue.add(() -> driver.type(text));
        return this;
    }

    /**
     * Press and release a key.
     * @param keyCode the key code (from KeyCodes)
     * @return this builder for chaining
     */
    public Actions pressKey(int keyCode) {
        return keyDown(keyCode).keyUp(keyCode);
    }

    // ========== Timing ==========

    /**
     * Pause for a duration.
     * @param duration the pause duration
     * @return this builder for chaining
     */
    public Actions pause(Duration duration) {
        actionQueue.add(() -> {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return this;
    }

    /**
     * Pause for milliseconds.
     * @param millis the pause in milliseconds
     * @return this builder for chaining
     */
    public Actions pause(long millis) {
        return pause(Duration.ofMillis(millis));
    }

    // ========== Scroll ==========

    /**
     * Scroll at current position.
     * @param deltaX horizontal scroll amount
     * @param deltaY vertical scroll amount
     * @return this builder for chaining
     */
    public Actions scroll(double deltaX, double deltaY) {
        actionQueue.add(() -> driver.scroll(currentX, currentY, deltaX, deltaY));
        return this;
    }

    /**
     * Scroll on an element.
     * @param element the target element
     * @param deltaX horizontal scroll amount
     * @param deltaY vertical scroll amount
     * @return this builder for chaining
     */
    public Actions scroll(GuiElement element, double deltaX, double deltaY) {
        return moveToElement(element).scroll(deltaX, deltaY);
    }

    // ========== Execution ==========

    /**
     * Execute all queued actions.
     */
    public void perform() {
        for (Runnable action : actionQueue) {
            action.run();
        }
        actionQueue.clear();
    }

    /**
     * Clear all queued actions without executing.
     * @return this builder for chaining
     */
    public Actions reset() {
        actionQueue.clear();
        return this;
    }
}
