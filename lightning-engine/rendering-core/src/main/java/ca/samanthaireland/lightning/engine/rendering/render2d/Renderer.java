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
 * Abstract interface for 2D GUI rendering.
 * This abstraction allows components to render without depending on a specific
 * rendering backend (NanoVG, pure OpenGL, Vulkan, etc.).
 *
 * <p>Implementations include:
 * <ul>
 *   <li>{@code NanoVGRenderer} - Uses NanoVG for vector graphics rendering</li>
 *   <li>{@code PureGLRenderer} - Uses direct OpenGL calls with custom shaders</li>
 * </ul>
 */
public interface Renderer {

    // ========== Lifecycle ==========

    /**
     * Begin a new frame.
     * Call this at the start of each frame before any rendering calls.
     *
     * @param width      viewport width in pixels
     * @param height     viewport height in pixels
     * @param pixelRatio device pixel ratio (1.0 for standard, 2.0 for Retina)
     */
    void beginFrame(int width, int height, float pixelRatio);

    /**
     * End the current frame.
     * Call this after all rendering is complete.
     */
    void endFrame();

    /**
     * Dispose of all renderer resources.
     * Call this when the renderer is no longer needed.
     */
    void dispose();

    // ========== Rectangles ==========

    /**
     * Draw a filled rectangle.
     */
    void fillRect(float x, float y, float width, float height, float[] color);

    /**
     * Draw a filled rounded rectangle.
     */
    void fillRoundedRect(float x, float y, float width, float height, float radius, float[] color);

    /**
     * Draw a stroked rectangle (border only).
     */
    void strokeRect(float x, float y, float width, float height, float[] color, float strokeWidth);

    /**
     * Draw a stroked rounded rectangle (border only).
     */
    void strokeRoundedRect(float x, float y, float width, float height, float radius, float[] color, float strokeWidth);

    // ========== Text ==========

    /**
     * Set the current font and size for text rendering.
     *
     * @param fontId   font ID from loadFont(), or -1 for default
     * @param fontSize font size in pixels
     */
    void setFont(int fontId, float fontSize);

    /**
     * Draw text at the specified position.
     *
     * @param x     x position
     * @param y     y position
     * @param text  text to draw
     * @param color text color (RGBA float array)
     * @param align alignment flags (use constants from this interface)
     */
    void drawText(float x, float y, String text, float[] color, int align);

    /**
     * Measure the width of text with the current font settings.
     *
     * @param text text to measure
     * @return width in pixels
     */
    float measureText(String text);

    /**
     * Measure the bounds of text with the current font settings.
     *
     * @param text   text to measure
     * @param bounds output array [minX, minY, maxX, maxY], or null to skip
     * @return width of the text
     */
    float measureTextBounds(String text, float[] bounds);

    // ========== Font Management ==========

    /**
     * Load a font from a resource path.
     *
     * @param name font name identifier
     * @param path resource path to the font file
     * @return font ID, or -1 if loading failed
     */
    int loadFont(String name, String path);

    /**
     * Get the default font ID.
     *
     * @return default font ID, or -1 if no default font is set
     */
    int getDefaultFontId();

    // ========== Clipping ==========

    /**
     * Push a scissor clip region.
     * Only pixels within the region will be rendered.
     *
     * @param x      clip region x
     * @param y      clip region y
     * @param width  clip region width
     * @param height clip region height
     */
    void pushClip(float x, float y, float width, float height);

    /**
     * Intersect with current scissor clip region.
     * Creates a clip region that is the intersection of the current clip and the new region.
     */
    void intersectClip(float x, float y, float width, float height);

    /**
     * Reset (pop) the scissor clip region to the previous state.
     */
    void popClip();

    // ========== Transform ==========

    /**
     * Save the current transform state.
     * Use with {@link #restore()} to temporarily modify transforms.
     */
    void save();

    /**
     * Restore the previously saved transform state.
     */
    void restore();

    /**
     * Translate the coordinate system.
     */
    void translate(float x, float y);

    /**
     * Scale the coordinate system.
     */
    void scale(float sx, float sy);

    /**
     * Rotate the coordinate system.
     *
     * @param angle angle in radians
     */
    void rotate(float angle);

    /**
     * Reset the transform to identity.
     */
    void resetTransform();

    // ========== Lines ==========

    /**
     * Draw a line from (x1, y1) to (x2, y2).
     */
    void drawLine(float x1, float y1, float x2, float y2, float[] color, float strokeWidth);

    // ========== Triangles (for icons) ==========

    /**
     * Draw a filled triangle.
     */
    void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, float[] color);

    // ========== Alignment Constants ==========

    /** Align text horizontally to the left. */
    int ALIGN_LEFT = 1 << 0;
    /** Align text horizontally to center. */
    int ALIGN_CENTER = 1 << 1;
    /** Align text horizontally to the right. */
    int ALIGN_RIGHT = 1 << 2;

    /** Align text vertically to the top. */
    int ALIGN_TOP = 1 << 3;
    /** Align text vertically to the middle. */
    int ALIGN_MIDDLE = 1 << 4;
    /** Align text vertically to the bottom. */
    int ALIGN_BOTTOM = 1 << 5;
    /** Align text vertically to the baseline. */
    int ALIGN_BASELINE = 1 << 6;
}
