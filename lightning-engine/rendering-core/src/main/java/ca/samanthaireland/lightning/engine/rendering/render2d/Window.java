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

import java.util.List;

/**
 * Abstract interface for a GUI window.
 * This abstraction allows client code to work with windows without
 * depending on specific rendering backends (OpenGL, Vulkan, etc.).
 */
public interface Window {

    // ========== Component Management ==========

    /**
     * Add a GUI component to the window.
     */
    void addComponent(WindowComponent component);

    /**
     * Remove a GUI component from the window.
     */
    void removeComponent(WindowComponent component);

    /**
     * Clear all components from the window.
     */
    void clearComponents();

    /**
     * Get all components in the window.
     */
    List<WindowComponent> getComponents();

    // ========== Overlay Management ==========

    /**
     * Show an overlay component (rendered on top of everything else).
     * Overlays are rendered last in z-order and receive input events first.
     * Common uses: context menus, tooltips, dialogs.
     *
     * @param overlay the overlay component to show
     */
    default void showOverlay(WindowComponent overlay) {
        // Default implementation does nothing
    }

    /**
     * Hide an overlay component.
     *
     * @param overlay the overlay to hide
     */
    default void hideOverlay(WindowComponent overlay) {
        // Default implementation does nothing
    }

    /**
     * Hide all overlays.
     */
    default void hideAllOverlays() {
        // Default implementation does nothing
    }

    /**
     * Get the currently active overlay, if any.
     *
     * @return the active overlay, or null if none
     */
    default WindowComponent getActiveOverlay() {
        return null;
    }

    // ========== Sprite Management ==========

    /**
     * Add a sprite to be rendered.
     *
     * @param sprite the sprite to add
     */
    void addSprite(Sprite sprite);

    /**
     * Remove a sprite by ID.
     *
     * @param spriteId the sprite ID to remove
     */
    void removeSprite(int spriteId);

    /**
     * Remove a sprite.
     *
     * @param sprite the sprite to remove
     */
    void removeSprite(Sprite sprite);

    /**
     * Clear all sprites from the window.
     */
    void clearSprites();

    /**
     * Get all sprites in the window.
     */
    List<Sprite> getSprites();

    /**
     * Get a sprite by ID.
     *
     * @param spriteId the sprite ID
     * @return the sprite, or null if not found
     */
    Sprite getSprite(int spriteId);

    /**
     * Get the sprite renderer for advanced sprite operations.
     *
     * @return the sprite renderer, or null if not initialized
     */
    SpriteRenderer getSpriteRenderer();

    // ========== Input Handling ==========

    /**
     * Add keyboard controls handler.
     *
     * @param handler the key input handler
     */
    void addControls(KeyInputHandler handler);

    /**
     * Remove keyboard controls handler.
     *
     * @param handler the key input handler to remove
     */
    void removeControls(KeyInputHandler handler);

    // ========== Lifecycle ==========

    /**
     * Set a callback to be called every frame for updates.
     */
    void setOnUpdate(Runnable onUpdate);

    /**
     * Start the window's event loop. This is a blocking call.
     */
    void run();

    /**
     * Run the event loop for a specific number of frames, then return.
     * Initializes the window if not already initialized.
     * Useful for testing where you want to verify rendering works.
     *
     * @param frameCount the number of frames to run
     */
    void runFrames(int frameCount);

    /**
     * Stop the window and exit the event loop.
     */
    void stop();

    // ========== Properties ==========

    /**
     * Enable or disable debug mode.
     */
    void setDebugMode(boolean debugMode);

    /**
     * Get the window width.
     */
    int getWidth();

    /**
     * Get the window height.
     */
    int getHeight();

    /**
     * Get the window title.
     */
    String getTitle();

    // ========== Rendering ==========

    /**
     * Get the Renderer instance for this window.
     * Use this to pass to components for backend-agnostic rendering.
     *
     * @return the Renderer instance, or null if not initialized
     */
    default Renderer getRenderer() {
        return null;
    }
}
