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

import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLWindowFactory;

/**
 * Builder for creating Window instances.
 * Provides a fluent API for configuring window parameters.
 *
 * <p>The builder is decoupled from specific Window implementations through
 * the {@link WindowFactory} interface. By default, it uses the OpenGL-based
 * factory, but a custom factory can be set via {@link #factory(WindowFactory)}.
 */
public class WindowBuilder {

    private int width = 800;
    private int height = 600;
    private String title = "Window";
    private boolean debugMode = false;
    private WindowFactory windowFactory = GLWindowFactory.getInstance();

    /**
     * Create a new WindowBuilder with default settings.
     */
    public static WindowBuilder create() {
        return new WindowBuilder();
    }

    /**
     * Set the window dimensions.
     */
    public WindowBuilder size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Set the window width.
     */
    public WindowBuilder width(int width) {
        this.width = width;
        return this;
    }

    /**
     * Set the window height.
     */
    public WindowBuilder height(int height) {
        this.height = height;
        return this;
    }

    /**
     * Set the window title.
     */
    public WindowBuilder title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Enable or disable debug mode.
     */
    public WindowBuilder debugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    /**
     * Set a custom WindowFactory to use for creating the Window.
     * This allows using different rendering backends (OpenGL, Vulkan, etc.).
     *
     * @param factory the WindowFactory to use
     * @return this builder
     */
    public WindowBuilder factory(WindowFactory factory) {
        this.windowFactory = factory;
        return this;
    }

    /**
     * Build and return the Window instance.
     * Uses the configured WindowFactory (defaults to GLWindowFactory).
     */
    public Window build() {
        Window window = windowFactory.create(width, height, title);
        window.setDebugMode(debugMode);
        return window;
    }
}
