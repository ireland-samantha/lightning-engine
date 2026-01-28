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

import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.KeyInputHandler;
import ca.samanthaireland.lightning.engine.rendering.render2d.Sprite;
import ca.samanthaireland.lightning.engine.rendering.render2d.SpriteRenderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.Window;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;
import ca.samanthaireland.lightning.engine.rendering.testing.ComponentRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A headless Window implementation for testing without OpenGL.
 *
 * <p>This window does not render anything - it simply maintains component state
 * and allows event simulation for testing purposes.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * HeadlessWindow window = new HeadlessWindow(800, 600);
 * window.addComponent(myPanel);
 *
 * GuiDriver driver = GuiDriver.connect(window);
 * driver.findElement(By.text("Save")).click();
 * }</pre>
 */
public class HeadlessWindow implements Window {

    private final int width;
    private final int height;
    private final String title;

    private final List<WindowComponent> components = new ArrayList<>();
    private final List<Sprite> sprites = new ArrayList<>();
    private final Map<Integer, Sprite> spritesById = new HashMap<>();
    private final List<KeyInputHandler> keyHandlers = new ArrayList<>();
    private final ComponentRegistry registry = new ComponentRegistry();

    private Runnable onUpdate;
    private boolean running = false;
    private boolean debugMode = false;

    /**
     * Create a headless window with default size.
     */
    public HeadlessWindow() {
        this(800, 600, "Headless Window");
    }

    /**
     * Create a headless window with specified size.
     * @param width window width
     * @param height window height
     */
    public HeadlessWindow(int width, int height) {
        this(width, height, "Headless Window");
    }

    /**
     * Create a headless window with specified size and title.
     * @param width window width
     * @param height window height
     * @param title window title
     */
    public HeadlessWindow(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    // ========== Component Management ==========

    @Override
    public void addComponent(WindowComponent component) {
        components.add(component);
        registry.registerTree(component);
    }

    @Override
    public void removeComponent(WindowComponent component) {
        components.remove(component);
        registry.unregister(component);
    }

    @Override
    public void clearComponents() {
        components.clear();
        registry.clear();
    }

    @Override
    public List<WindowComponent> getComponents() {
        return new ArrayList<>(components);
    }

    // ========== Sprite Management ==========

    @Override
    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
        spritesById.put(sprite.getId(), sprite);
    }

    @Override
    public void removeSprite(int spriteId) {
        Sprite sprite = spritesById.remove(spriteId);
        if (sprite != null) {
            sprites.remove(sprite);
        }
    }

    @Override
    public void removeSprite(Sprite sprite) {
        sprites.remove(sprite);
        spritesById.remove(sprite.getId());
    }

    @Override
    public void clearSprites() {
        sprites.clear();
        spritesById.clear();
    }

    @Override
    public List<Sprite> getSprites() {
        return new ArrayList<>(sprites);
    }

    @Override
    public Sprite getSprite(int spriteId) {
        return spritesById.get(spriteId);
    }

    @Override
    public SpriteRenderer getSpriteRenderer() {
        return null; // No sprite renderer in headless mode
    }

    // ========== Input Handling ==========

    @Override
    public void addControls(KeyInputHandler handler) {
        keyHandlers.add(handler);
    }

    @Override
    public void removeControls(KeyInputHandler handler) {
        keyHandlers.remove(handler);
    }

    // ========== Lifecycle ==========

    @Override
    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    @Override
    public void run() {
        running = true;
        // In headless mode, we don't actually run a loop
        // Tests will call update() manually if needed
    }

    @Override
    public void runFrames(int frameCount) {
        running = true;
        // In headless mode, just call update() for each frame
        for (int i = 0; i < frameCount && running; i++) {
            update();
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    // ========== Properties ==========

    @Override
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getTitle() {
        return title;
    }

    // ========== Test Support Methods ==========

    /**
     * Check if the window is running.
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Check if debug mode is enabled.
     * @return true if debug mode
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Get the component registry.
     * @return the registry
     */
    public ComponentRegistry getRegistry() {
        return registry;
    }

    /**
     * Trigger an update (calls the onUpdate callback).
     */
    public void update() {
        if (onUpdate != null) {
            onUpdate.run();
        }
    }

    /**
     * Simulate a mouse click at the given coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void simulateClick(int x, int y) {
        simulateMouseClick(x, y, InputConstants.BUTTON_LEFT, InputConstants.ACTION_PRESS);
        simulateMouseClick(x, y, InputConstants.BUTTON_LEFT, InputConstants.ACTION_RELEASE);
    }

    /**
     * Simulate a mouse click event.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param button the mouse button (use InputConstants.BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @param action the action (use InputConstants.ACTION_PRESS, ACTION_RELEASE)
     */
    public void simulateMouseClick(int x, int y, int button, int action) {
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onMouseClick(x, y, button, action)) {
                return;
            }
        }
    }

    /**
     * Simulate mouse movement.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void simulateMouseMove(int x, int y) {
        for (int i = components.size() - 1; i >= 0; i--) {
            components.get(i).onMouseMove(x, y);
        }
    }

    /**
     * Simulate mouse scroll.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param scrollX horizontal scroll amount
     * @param scrollY vertical scroll amount
     */
    public void simulateMouseScroll(int x, int y, double scrollX, double scrollY) {
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onMouseScroll(x, y, scrollX, scrollY)) {
                return;
            }
        }
    }

    /**
     * Simulate a key press.
     * @param keyCode the key code
     */
    public void simulateKeyPress(int keyCode) {
        simulateKey(keyCode, InputConstants.ACTION_PRESS);
        simulateKey(keyCode, InputConstants.ACTION_RELEASE);
    }

    /**
     * Simulate a key event.
     * @param keyCode the key code
     * @param action the action (use InputConstants.ACTION_PRESS, ACTION_RELEASE, ACTION_REPEAT)
     */
    public void simulateKey(int keyCode, int action) {
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onKeyPress(keyCode, action)) {
                return;
            }
        }

        // Also notify key handlers for arrow keys
        if (InputConstants.isPress(action)) {
            KeyInputHandler.KeyType keyType = mapToKeyType(keyCode);
            if (keyType != null) {
                for (KeyInputHandler handler : keyHandlers) {
                    handler.onArrowKeyPress(keyType);
                }
            }
        }
    }

    /**
     * Simulate character input.
     * @param codepoint the unicode codepoint
     */
    public void simulateCharInput(int codepoint) {
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onCharInput(codepoint)) {
                return;
            }
        }
    }

    /**
     * Simulate typing a string.
     * @param text the text to type
     */
    public void simulateTyping(String text) {
        for (char c : text.toCharArray()) {
            simulateCharInput(c);
        }
    }

    private KeyInputHandler.KeyType mapToKeyType(int keyCode) {
        return switch (keyCode) {
            case 265 -> KeyInputHandler.KeyType.UP;
            case 264 -> KeyInputHandler.KeyType.DOWN;
            case 263 -> KeyInputHandler.KeyType.LEFT;
            case 262 -> KeyInputHandler.KeyType.RIGHT;
            default -> null;
        };
    }
}
