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


package ca.samanthaireland.game.renderering;

import ca.samanthaireland.lightning.engine.rendering.render2d.Window;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowFactory;
import ca.samanthaireland.game.domain.ControlSystem;
import ca.samanthaireland.game.orchestrator.SpriteSnapshotMapper;

/**
 * Builder for creating GameRenderer instances.
 * Provides a fluent API for configuring game rendering.
 *
 * <p>Example usage:
 * <pre>{@code
 * GameRenderer renderer = GameRendererBuilder.create()
 *     .windowSize(800, 600)
 *     .title("My Game")
 *     .controlSystem(myControls)
 *     .spriteMapper(components -> convertToSprites(components))
 *     .build();
 *
 * renderer.start(() -> {
 *     // Game update logic
 * });
 * }</pre>
 */
public class GameRendererBuilder {

    private int width = 800;
    private int height = 600;
    private String title = "Game";
    private ControlSystem controlSystem;
    private SpriteSnapshotMapper spriteMapper;
    private Window window; // Optional pre-built window
    private WindowFactory windowFactory;

    private GameRendererBuilder() {
    }

    /**
     * Create a new GameRendererBuilder with default settings.
     */
    public static GameRendererBuilder create() {
        return new GameRendererBuilder();
    }

    /**
     * Set the window dimensions.
     *
     * @param width  window width in pixels
     * @param height window height in pixels
     */
    public GameRendererBuilder windowSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Set the window width.
     */
    public GameRendererBuilder width(int width) {
        this.width = width;
        return this;
    }

    /**
     * Set the window height.
     */
    public GameRendererBuilder height(int height) {
        this.height = height;
        return this;
    }

    /**
     * Set the window title.
     */
    public GameRendererBuilder title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the control system for input handling.
     */
    public GameRendererBuilder controlSystem(ControlSystem controlSystem) {
        this.controlSystem = controlSystem;
        return this;
    }

    /**
     * Set the sprite mapper for converting snapshots to sprites.
     */
    public GameRendererBuilder spriteMapper(SpriteSnapshotMapper spriteMapper) {
        this.spriteMapper = spriteMapper;
        return this;
    }

    /**
     * Use a pre-built window instead of creating one.
     * Useful for testing with HeadlessWindow.
     */
    public GameRendererBuilder window(Window window) {
        this.window = window;
        return this;
    }

    /**
     * Set a custom window factory.
     * This allows using different rendering backends.
     */
    public GameRendererBuilder windowFactory(WindowFactory factory) {
        this.windowFactory = factory;
        return this;
    }

    /**
     * Build the GameRenderer instance.
     */
    public GameRenderer build() {
        DefaultGameRenderer renderer;

        if (window != null) {
            // Use pre-built window
            renderer = new DefaultGameRenderer(window);
        } else {
            // Create new window
            renderer = new DefaultGameRenderer(width, height, title);
        }

        if (controlSystem != null) {
            renderer.setControlSystem(controlSystem);
        }

        if (spriteMapper != null) {
            renderer.setSpriteMapper(spriteMapper);
        }

        return renderer;
    }
}
