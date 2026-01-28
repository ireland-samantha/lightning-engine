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

import ca.samanthaireland.lightning.engine.rendering.render2d.AbstractWindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.Image;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;

/**
 * Mock Image implementation for headless testing.
 */
public class MockImage extends AbstractWindowComponent implements Image {

    private String loadedFilePath;
    private String loadedResourcePath;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private boolean loaded = false;
    private boolean maintainAspectRatio = true;

    public MockImage(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public boolean loadFromFile(String filePath) {
        this.loadedFilePath = filePath;
        this.loaded = true;
        // Simulate loading - in headless mode we just track the path
        return true;
    }

    @Override
    public boolean loadFromResource(String resourcePath) {
        this.loadedResourcePath = resourcePath;
        this.loaded = true;
        return true;
    }

    @Override
    public int getImageWidth() {
        return imageWidth;
    }

    @Override
    public int getImageHeight() {
        return imageHeight;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void dispose() {
        loaded = false;
        loadedFilePath = null;
        loadedResourcePath = null;
        imageWidth = 0;
        imageHeight = 0;
    }

    @Override
    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
    }

    @Override
    public boolean isMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    @Override
    public void render(Renderer renderer) {
        // No rendering in headless mode
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        return visible && contains(mx, my);
    }

    // Test helper methods

    /**
     * Get the file path that was loaded (for testing).
     */
    public String getLoadedFilePath() {
        return loadedFilePath;
    }

    /**
     * Get the resource path that was loaded (for testing).
     */
    public String getLoadedResourcePath() {
        return loadedResourcePath;
    }

    /**
     * Set the simulated image dimensions (for testing).
     */
    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }
}
