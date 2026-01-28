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
 * An image component that displays a texture/image file.
 *
 * <p>Unlike sprites which are rendered using the 3D sprite renderer,
 * Image components are rendered as part of the NanoVG UI layer,
 * making them suitable for use in panels and other UI elements.
 */
public interface Image extends WindowComponent {

    /**
     * Load an image from a file path.
     *
     * @param filePath the path to the image file
     * @return true if the image was loaded successfully
     */
    boolean loadFromFile(String filePath);

    /**
     * Load an image from a classpath resource.
     *
     * @param resourcePath the classpath resource path
     * @return true if the image was loaded successfully
     */
    boolean loadFromResource(String resourcePath);

    /**
     * Get the original width of the loaded image.
     *
     * @return the image width in pixels, or 0 if no image is loaded
     */
    int getImageWidth();

    /**
     * Get the original height of the loaded image.
     *
     * @return the image height in pixels, or 0 if no image is loaded
     */
    int getImageHeight();

    /**
     * Check if an image is currently loaded.
     *
     * @return true if an image is loaded
     */
    boolean isLoaded();

    /**
     * Dispose of the image resources.
     * Should be called when the image is no longer needed.
     */
    void dispose();

    /**
     * Set whether to maintain aspect ratio when rendering.
     *
     * @param maintainAspectRatio true to maintain aspect ratio
     */
    void setMaintainAspectRatio(boolean maintainAspectRatio);

    /**
     * Get whether aspect ratio is being maintained.
     *
     * @return true if aspect ratio is maintained
     */
    boolean isMaintainAspectRatio();
}
