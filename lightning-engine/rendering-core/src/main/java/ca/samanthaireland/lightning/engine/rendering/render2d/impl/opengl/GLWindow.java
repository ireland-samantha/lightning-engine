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

import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.KeyInputHandler;
import ca.samanthaireland.lightning.engine.rendering.render2d.Panel;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.Sprite;
import ca.samanthaireland.lightning.engine.rendering.render2d.SpriteInputHandler;
import ca.samanthaireland.lightning.engine.rendering.render2d.SpriteRenderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.Window;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11C.*;

/**
 * A window with NanoVG-based GUI rendering capabilities and sprite support.
 * This is the OpenGL/GLFW implementation of the Window interface.
 */
@Slf4j
public class GLWindow implements Window {

    private final int windowWidth;
    private final int windowHeight;
    private final String title;
    private final List<WindowComponent> components = new ArrayList<>();
    private final List<WindowComponent> overlays = new ArrayList<>();
    private final List<Sprite> sprites = new CopyOnWriteArrayList<>();
    private final List<KeyInputHandler> keyHandlers = new ArrayList<>();

    private long windowHandle;
    private long nvgContext;
    private int defaultFontId = -1;
    private GLFontLoader fontLoader;
    private GLSpriteRenderer spriteRenderer;
    private NanoVGRenderer renderer;

    private boolean running = true;
    private Runnable onUpdate;
    private boolean debugMode = false;

    // Sprite input tracking
    private Sprite hoveredSprite = null;
    private Sprite focusedSprite = null;

    // Framebuffer dimensions (may differ from window on HiDPI displays)
    private int framebufferWidth;
    private int framebufferHeight;
    private float pixelRatio = 1.0f;

    public GLWindow(int width, int height, String title) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.title = title;
    }

    // ========== Component Management ==========

    @Override
    public void addComponent(WindowComponent component) {
        components.add(component);
    }

    @Override
    public void removeComponent(WindowComponent component) {
        components.remove(component);
    }

    @Override
    public void clearComponents() {
        components.clear();
    }

    @Override
    public List<WindowComponent> getComponents() {
        return components;
    }

    // ========== Overlay Management ==========

    @Override
    public void showOverlay(WindowComponent overlay) {
        if (!overlays.contains(overlay)) {
            overlays.add(overlay);
            overlay.setVisible(true);
            log.debug("Showing overlay: {}", overlay.getClass().getSimpleName());
        }
    }

    @Override
    public void hideOverlay(WindowComponent overlay) {
        overlays.remove(overlay);
        overlay.setVisible(false);
        log.debug("Hiding overlay: {}", overlay.getClass().getSimpleName());
    }

    @Override
    public void hideAllOverlays() {
        for (WindowComponent overlay : overlays) {
            overlay.setVisible(false);
        }
        overlays.clear();
    }

    @Override
    public WindowComponent getActiveOverlay() {
        return overlays.isEmpty() ? null : overlays.get(overlays.size() - 1);
    }

    // ========== Sprite Management ==========

    @Override
    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
    }

    @Override
    public void removeSprite(int spriteId) {
        sprites.removeIf(s -> s.getId() == spriteId);
    }

    @Override
    public void removeSprite(Sprite sprite) {
        sprites.remove(sprite);
    }

    @Override
    public void clearSprites() {
        sprites.clear();
    }

    @Override
    public List<Sprite> getSprites() {
        return new ArrayList<>(sprites);
    }

    @Override
    public Sprite getSprite(int spriteId) {
        return sprites.stream()
                .filter(s -> s.getId() == spriteId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public SpriteRenderer getSpriteRenderer() {
        return spriteRenderer;
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

    public int getDefaultFontId() {
        return defaultFontId;
    }

    public long getNvgContext() {
        return nvgContext;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public int getWidth() {
        return windowWidth;
    }

    @Override
    public int getHeight() {
        return windowHeight;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void run() {
        init();
        loop();
        cleanup();
    }

    @Override
    public void runFrames(int frameCount) {
        if (windowHandle == 0) {
            init();
        }
        loopFrames(frameCount);
    }

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        windowHandle = glfwCreateWindow(windowWidth, windowHeight, title, 0, 0);
        if (windowHandle == 0) {
            throw new RuntimeException("Failed to create window");
        }

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();
        glfwSwapInterval(1);

        // Ensure window is visible and focused (especially important on macOS)
        glfwShowWindow(windowHandle);
        glfwFocusWindow(windowHandle);

        // Get framebuffer size (may differ from window size on HiDPI displays)
        int[] fbWidth = new int[1];
        int[] fbHeight = new int[1];
        glfwGetFramebufferSize(windowHandle, fbWidth, fbHeight);
        framebufferWidth = fbWidth[0];
        framebufferHeight = fbHeight[0];
        pixelRatio = (float) framebufferWidth / windowWidth;

        if (debugMode) {
            log.debug("Window size: {}x{}", windowWidth, windowHeight);
            log.debug("Framebuffer size: {}x{}", framebufferWidth, framebufferHeight);
            log.debug("Pixel ratio: {}", pixelRatio);
        }

        // Initialize NanoVG
        nvgContext = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (nvgContext == 0) {
            throw new RuntimeException("Failed to create NanoVG context");
        }

        // Load default font using FontLoader
        fontLoader = new GLFontLoader(nvgContext);
        defaultFontId = fontLoader.loadDefaultFont();
        if (defaultFontId >= 0) {
            nvgFontFaceId(nvgContext, defaultFontId);
        }

        // Initialize the Renderer abstraction (wraps NanoVG)
        renderer = new NanoVGRenderer();
        renderer.init();

        // Initialize sprite renderer
        spriteRenderer = new GLSpriteRenderer(windowWidth, windowHeight);

        // Setup input callbacks
        setupInputCallbacks();
    }

    private void setupInputCallbacks() {
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            glfwGetCursorPos(window, xpos, ypos);
            int mx = (int) xpos[0];
            int my = (int) ypos[0];

            if (debugMode) {
                log.debug("Mouse click: x={}, y={}, button={}, action={}",
                    mx, my, button, InputConstants.isPress(action) ? "PRESS" : "RELEASE");
            }

            // On mouse press, clear focus from all components first
            // This ensures text fields lose focus when clicking elsewhere
            if (InputConstants.isPress(action) && InputConstants.isLeftButton(button)) {
                clearAllFocus();
            }

            // Check overlays first (they're on top)
            for (int i = overlays.size() - 1; i >= 0; i--) {
                WindowComponent overlay = overlays.get(i);
                if (overlay.onMouseClick(mx, my, button, action)) {
                    if (debugMode) {
                        log.debug("  -> Handled by overlay {}", overlay.getClass().getSimpleName());
                    }
                    return;
                }
            }

            // If clicking and there are overlays showing, hide them if click was outside
            if (InputConstants.isPress(action) && !overlays.isEmpty()) {
                hideAllOverlays();
            }

            // Propagate to components in reverse order (top-most first)
            boolean handled = false;
            for (int i = components.size() - 1; i >= 0; i--) {
                WindowComponent comp = components.get(i);
                if (debugMode) {
                    log.debug("  Checking component {}: {} at ({},{}) size ({}x{}) contains={}",
                        i, comp.getClass().getSimpleName(), comp.getX(), comp.getY(),
                        comp.getWidth(), comp.getHeight(), comp.contains(mx, my));
                }
                if (comp.onMouseClick(mx, my, button, action)) {
                    if (debugMode) {
                        log.debug("  -> Handled by {}", comp.getClass().getSimpleName());
                    }
                    handled = true;
                    break;
                }
            }

            // If no component handled it, check sprites (they're behind GUI)
            if (!handled) {
                handled = dispatchMouseClickToSprites(mx, my, button, action);
            }

            // Update focused sprite
            if (InputConstants.isPress(action) && InputConstants.isLeftButton(button) && !handled) {
                // Click on empty area - clear sprite focus
                focusedSprite = null;
            }
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            int mx = (int) xpos;
            int my = (int) ypos;

            // Update overlays first
            for (int i = overlays.size() - 1; i >= 0; i--) {
                overlays.get(i).onMouseMove(mx, my);
            }

            for (int i = components.size() - 1; i >= 0; i--) {
                components.get(i).onMouseMove(mx, my);
            }

            // Handle sprite hover tracking
            updateSpriteHover(mx, my);
        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            glfwGetCursorPos(window, xpos, ypos);
            int mx = (int) xpos[0];
            int my = (int) ypos[0];

            boolean handled = false;
            for (int i = components.size() - 1; i >= 0; i--) {
                if (components.get(i).onMouseScroll(mx, my, xoffset, yoffset)) {
                    handled = true;
                    break;
                }
            }

            // If no component handled it, try sprites
            if (!handled) {
                dispatchMouseScrollToSprites(mx, my, xoffset, yoffset);
            }
        });

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
                return;
            }

            // Handle arrow keys for registered handlers
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                KeyInputHandler.KeyType keyType = null;
                if (key == GLFW_KEY_UP) keyType = KeyInputHandler.KeyType.UP;
                else if (key == GLFW_KEY_DOWN) keyType = KeyInputHandler.KeyType.DOWN;
                else if (key == GLFW_KEY_LEFT) keyType = KeyInputHandler.KeyType.LEFT;
                else if (key == GLFW_KEY_RIGHT) keyType = KeyInputHandler.KeyType.RIGHT;

                if (keyType != null) {
                    for (KeyInputHandler handler : keyHandlers) {
                        handler.onArrowKeyPress(keyType);
                    }
                }
            }

            // Propagate to components (with mods for copy/paste support)
            boolean handled = false;
            for (int i = components.size() - 1; i >= 0; i--) {
                if (components.get(i).onKeyPress(key, action, mods)) {
                    handled = true;
                    break;
                }
            }

            // If no component handled it and a sprite has focus, dispatch to it
            if (!handled && focusedSprite != null && focusedSprite.hasInputHandler()) {
                focusedSprite.getInputHandler().onKeyPress(focusedSprite, key, action, mods);
            }
        });

        glfwSetCharCallback(windowHandle, (window, codepoint) -> {
            boolean handled = false;
            for (int i = components.size() - 1; i >= 0; i--) {
                if (components.get(i).onCharInput(codepoint)) {
                    handled = true;
                    break;
                }
            }

            // If no component handled it and a sprite has focus, dispatch to it
            if (!handled && focusedSprite != null && focusedSprite.hasInputHandler()) {
                focusedSprite.getInputHandler().onCharInput(focusedSprite, codepoint);
            }
        });
    }

    // ========== Sprite Input Dispatch ==========

    /**
     * Dispatch mouse click to sprites in reverse z-order (highest z-index first).
     *
     * @return true if a sprite consumed the event
     */
    private boolean dispatchMouseClickToSprites(int mx, int my, int button, int action) {
        // Sort sprites by z-index (highest first)
        List<Sprite> sortedSprites = new ArrayList<>(sprites);
        sortedSprites.sort((a, b) -> Integer.compare(b.getZIndex(), a.getZIndex()));

        for (Sprite sprite : sortedSprites) {
            if (sprite.hasInputHandler() && sprite.contains(mx, my)) {
                SpriteInputHandler handler = sprite.getInputHandler();
                if (handler.onMouseClick(sprite, button, action)) {
                    // Handle focus on click
                    if (InputConstants.isPress(action) && InputConstants.isLeftButton(button) && sprite.isFocusable()) {
                        focusedSprite = sprite;
                    }
                    if (debugMode) {
                        log.debug("  -> Handled by sprite {}", sprite.getId());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Dispatch mouse scroll to sprites.
     *
     * @return true if a sprite consumed the event
     */
    private boolean dispatchMouseScrollToSprites(int mx, int my, double scrollX, double scrollY) {
        // Sort sprites by z-index (highest first)
        List<Sprite> sortedSprites = new ArrayList<>(sprites);
        sortedSprites.sort((a, b) -> Integer.compare(b.getZIndex(), a.getZIndex()));

        for (Sprite sprite : sortedSprites) {
            if (sprite.hasInputHandler() && sprite.contains(mx, my)) {
                if (sprite.getInputHandler().onMouseScroll(sprite, scrollX, scrollY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Update sprite hover state and dispatch enter/exit events.
     */
    private void updateSpriteHover(int mx, int my) {
        // Find the sprite under the cursor (highest z-index first)
        Sprite newHovered = null;
        List<Sprite> sortedSprites = new ArrayList<>(sprites);
        sortedSprites.sort((a, b) -> Integer.compare(b.getZIndex(), a.getZIndex()));

        for (Sprite sprite : sortedSprites) {
            if (sprite.hasInputHandler() && sprite.contains(mx, my)) {
                newHovered = sprite;
                break;
            }
        }

        // Dispatch enter/exit events
        if (newHovered != hoveredSprite) {
            if (hoveredSprite != null && hoveredSprite.hasInputHandler()) {
                hoveredSprite.getInputHandler().onMouseExit(hoveredSprite);
            }
            if (newHovered != null && newHovered.hasInputHandler()) {
                newHovered.getInputHandler().onMouseEnter(newHovered);
            }
            hoveredSprite = newHovered;
        }

        // Dispatch move event to hovered sprite
        if (hoveredSprite != null && hoveredSprite.hasInputHandler()) {
            hoveredSprite.getInputHandler().onMouseMove(hoveredSprite, mx, my);
        }
    }

    private void loop() {
        loopWhile(() -> !glfwWindowShouldClose(windowHandle) && running);
    }

    /**
     * Run the event loop for a specific number of frames.
     * Similar to loop() but exits after frameCount frames instead of running until stopped.
     */
    private void loopFrames(int frameCount) {
        int[] frame = {0};
        loopWhile(() -> frame[0]++ < frameCount && !glfwWindowShouldClose(windowHandle) && running);
    }

    /**
     * Unified render loop implementation.
     * Continues rendering while the condition returns true.
     *
     * @param condition the condition to check before each frame
     */
    private void loopWhile(BooleanSupplier condition) {
        while (condition.getAsBoolean()) {
            renderFrame();
        }
    }

    /**
     * Render a single frame.
     * This method handles all rendering for one frame: update callback, sprites, components, and overlays.
     */
    private void renderFrame() {
        // Call update callback
        if (onUpdate != null) {
            onUpdate.run();
        }

        // Clear screen - use framebuffer size for viewport
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glClearColor(
            GLColour.BACKGROUND[0],
            GLColour.BACKGROUND[1],
            GLColour.BACKGROUND[2],
            GLColour.BACKGROUND[3]
        );
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        // Render sprites first (behind GUI components)
        renderSprites();

        // Render NanoVG GUI
        renderGui();

        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    /**
     * Render all sprites.
     */
    private void renderSprites() {
        if (!sprites.isEmpty() && spriteRenderer != null) {
            spriteRenderer.begin();
            for (Sprite sprite : sprites) {
                spriteRenderer.draw(sprite);
            }
            spriteRenderer.end();
        }
    }

    /**
     * Render all GUI components and overlays.
     *
     * <p>Uses the Renderer interface for components that support it,
     * falling back to the legacy NanoVG render method for backward compatibility.
     */
    @SuppressWarnings("deprecation")
    private void renderGui() {
        // Begin NanoVG frame with correct pixel ratio
        nvgBeginFrame(nvgContext, windowWidth, windowHeight, pixelRatio);

        // Set up GUI context for this frame
        GLContext.begin(nvgContext, defaultFontId, this);

        // Set default font before rendering each frame
        if (defaultFontId >= 0) {
            nvgFontFaceId(nvgContext, defaultFontId);
            nvgFontSize(nvgContext, 14.0f);
        }

        // Render all components using Renderer interface
        for (WindowComponent component : components) {
            // Reset font before each component in case it was changed
            if (defaultFontId >= 0) {
                nvgFontFaceId(nvgContext, defaultFontId);
            }

            component.render(renderer);
        }

        // Render overlays last (on top of everything)
        for (WindowComponent overlay : overlays) {
            if (defaultFontId >= 0) {
                nvgFontFaceId(nvgContext, defaultFontId);
            }

            overlay.render(renderer);
        }

        // End GUI context
        GLContext.end();

        // End NanoVG frame
        nvgEndFrame(nvgContext);
    }

    private void cleanup() {
        if (spriteRenderer != null) {
            spriteRenderer.dispose();
        }
        if (renderer != null) {
            renderer.dispose();
        }
        nvgDelete(nvgContext);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }

    /**
     * Get the Renderer instance for this window.
     * Use this to pass to components for backend-agnostic rendering.
     *
     * @return the Renderer instance
     */
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Clear focus from all components recursively.
     * This is called before propagating mouse clicks to ensure
     * text fields lose focus when clicking elsewhere.
     */
    private void clearAllFocus() {
        for (WindowComponent component : components) {
            clearFocusRecursively(component);
        }
        for (WindowComponent overlay : overlays) {
            clearFocusRecursively(overlay);
        }
    }

    private void clearFocusRecursively(WindowComponent component) {
        component.clearFocus();
        // If component is a panel, clear focus on its children
        if (component instanceof Panel panel) {
            for (WindowComponent child : panel.getChildren()) {
                clearFocusRecursively(child);
            }
        }
    }

    /**
     * Load a custom font from a resource path and return its ID.
     *
     * @param name the font name to register
     * @param path the resource path to the font file
     * @return the font ID, or -1 if the font could not be loaded
     */
    public int loadFont(String name, String path) {
        if (fontLoader == null) {
            log.warn("Cannot load font '{}': FontLoader not initialized", name);
            return -1;
        }
        return fontLoader.loadFontFromResource(name, path);
    }
}
