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
 * Interface for sprite rendering.
 *
 * <p>A sprite renderer handles the rendering of 2D sprites with textures.
 * It manages the rendering pipeline including shaders, vertex buffers,
 * and transformation matrices.
 */
public interface SpriteRenderer extends AutoCloseable {

    /**
     * Draw a sprite at the specified position.
     *
     * @param sprite the sprite data to render
     */
    void draw(Sprite sprite);

    /**
     * Draw a sprite with a specific texture.
     *
     * @param texture the texture to use
     * @param x the x position
     * @param y the y position
     * @param width the width
     * @param height the height
     */
    void draw(Texture texture, float x, float y, float width, float height);

    /**
     * Draw a sprite with rotation.
     *
     * @param texture the texture to use
     * @param x the x position
     * @param y the y position
     * @param width the width
     * @param height the height
     * @param rotation the rotation in radians
     */
    void draw(Texture texture, float x, float y, float width, float height, float rotation);

    /**
     * Begin a batch of sprite rendering.
     * Call this before drawing multiple sprites for better performance.
     */
    void begin();

    /**
     * End a batch of sprite rendering.
     * Call this after drawing multiple sprites.
     */
    void end();

    /**
     * Dispose of renderer resources.
     */
    void dispose();

    @Override
    default void close() {
        dispose();
    }
}
