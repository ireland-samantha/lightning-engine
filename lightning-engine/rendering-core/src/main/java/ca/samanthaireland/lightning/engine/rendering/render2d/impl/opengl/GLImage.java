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

import ca.samanthaireland.lightning.engine.rendering.render2d.AbstractWindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.Image;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGPaint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * NanoVG-based image component for displaying textures in the UI.
 * OpenGL implementation of {@link Image}.
 */
@Slf4j
public class GLImage extends AbstractWindowComponent implements Image {

    private int imageHandle = -1;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private boolean maintainAspectRatio = true;
    private float[] borderColor = GLColour.BORDER;

    // Store the NVG context used to create the image (needed for cleanup)
    private long createdInNvgContext = 0;

    public GLImage(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public boolean loadFromFile(String filePath) {
        long nvg = GLContext.getNvgContext();
        if (nvg == 0) {
            log.warn("Cannot load image - no NVG context available. Will retry during render.");
            // Store path for deferred loading
            return deferredLoadFromFile(filePath);
        }
        return doLoadFromFile(nvg, filePath);
    }

    private boolean deferredLoadFromFile(String filePath) {
        // Store for later loading during render
        this.deferredFilePath = filePath;
        return true;
    }

    private String deferredFilePath = null;

    private boolean doLoadFromFile(long nvg, String filePath) {
        try {
            // Dispose previous image if any
            disposeImage(nvg);

            // Load image bytes from file
            byte[] bytes = Files.readAllBytes(Path.of(filePath));
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            // Create NanoVG image
            imageHandle = nvgCreateImageMem(nvg, NVG_IMAGE_GENERATE_MIPMAPS, buffer);
            if (imageHandle <= 0) {
                log.error("Failed to create NanoVG image from file: {}", filePath);
                return false;
            }

            // Get image dimensions
            int[] w = new int[1];
            int[] h = new int[1];
            nvgImageSize(nvg, imageHandle, w, h);
            imageWidth = w[0];
            imageHeight = h[0];
            createdInNvgContext = nvg;

            log.debug("Loaded image from file: {} ({}x{})", filePath, imageWidth, imageHeight);
            return true;
        } catch (IOException e) {
            log.error("Failed to load image from file: {}", filePath, e);
            return false;
        }
    }

    @Override
    public boolean loadFromResource(String resourcePath) {
        long nvg = GLContext.getNvgContext();
        if (nvg == 0) {
            log.warn("Cannot load image - no NVG context available. Will retry during render.");
            return deferredLoadFromResource(resourcePath);
        }
        return doLoadFromResource(nvg, resourcePath);
    }

    private String deferredResourcePath = null;

    private boolean deferredLoadFromResource(String resourcePath) {
        this.deferredResourcePath = resourcePath;
        return true;
    }

    private boolean doLoadFromResource(long nvg, String resourcePath) {
        try {
            // Dispose previous image if any
            disposeImage(nvg);

            // Load image bytes from classpath
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    log.error("Image resource not found: {}", resourcePath);
                    return false;
                }

                ByteBuffer buffer = ioResourceToByteBuffer(in);

                // Create NanoVG image
                imageHandle = nvgCreateImageMem(nvg, NVG_IMAGE_GENERATE_MIPMAPS, buffer);
                if (imageHandle <= 0) {
                    log.error("Failed to create NanoVG image from resource: {}", resourcePath);
                    return false;
                }

                // Get image dimensions
                int[] w = new int[1];
                int[] h = new int[1];
                nvgImageSize(nvg, imageHandle, w, h);
                imageWidth = w[0];
                imageHeight = h[0];
                createdInNvgContext = nvg;

                log.debug("Loaded image from resource: {} ({}x{})", resourcePath, imageWidth, imageHeight);
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to load image from resource: {}", resourcePath, e);
            return false;
        }
    }

    private ByteBuffer ioResourceToByteBuffer(InputStream source) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(source);
        ByteBuffer buffer = BufferUtils.createByteBuffer(1024);
        while (true) {
            int bytes = rbc.read(buffer);
            if (bytes == -1)
                break;
            if (buffer.remaining() == 0)
                buffer = resizeBuffer(buffer, buffer.capacity() * 2);
        }
        buffer.flip();
        return buffer;
    }

    private ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
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
        return imageHandle > 0;
    }

    @Override
    public void dispose() {
        if (createdInNvgContext != 0) {
            disposeImage(createdInNvgContext);
        }
    }

    private void disposeImage(long nvg) {
        if (imageHandle > 0 && nvg != 0) {
            nvgDeleteImage(nvg, imageHandle);
            imageHandle = -1;
            imageWidth = 0;
            imageHeight = 0;
            createdInNvgContext = 0;
        }
    }

    @Override
    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
    }

    @Override
    public boolean isMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    /**
     * Set the border color.
     */
    public void setBorderColor(float[] borderColor) {
        this.borderColor = borderColor;
    }

    @Override
    public void render(Renderer renderer) {
        if (!visible) {
            log.trace("GLImage.render() - not visible, skipping");
            return;
        }

        // Image rendering requires NanoVG-specific features (image patterns)
        // Get the NanoVG context from the renderer
        long nvg = 0;
        if (renderer instanceof NanoVGRenderer nanoVGRenderer) {
            nvg = nanoVGRenderer.getNvg();
        }

        if (nvg == 0) {
            // Fallback: draw placeholder using Renderer interface
            renderer.strokeRoundedRect(x, y, width, height, 4, borderColor, 1.0f);
            renderer.fillRoundedRect(x, y, width, height, 4, GLColour.DARK_GRAY);
            renderer.setFont(-1, 12.0f);
            renderer.drawText(x + width / 2.0f, y + height / 2.0f, "No Image",
                    GLColour.TEXT_SECONDARY, Renderer.ALIGN_CENTER | Renderer.ALIGN_MIDDLE);
            return;
        }

        // Handle deferred loading from file (always process if path is set, replacing any existing image)
        if (deferredFilePath != null) {
            log.info("GLImage.render() - performing deferred load from file: {}", deferredFilePath);
            boolean success = doLoadFromFile(nvg, deferredFilePath);
            log.info("GLImage.render() - deferred load result: {}, imageHandle: {}", success, imageHandle);
            deferredFilePath = null;
        }

        // Handle deferred loading from resource (always process if path is set, replacing any existing image)
        if (deferredResourcePath != null) {
            log.info("GLImage.render() - performing deferred load from resource: {}", deferredResourcePath);
            boolean success = doLoadFromResource(nvg, deferredResourcePath);
            log.info("GLImage.render() - deferred load result: {}, imageHandle: {}", success, imageHandle);
            deferredResourcePath = null;
        }

        try (var color = org.lwjgl.nanovg.NVGColor.malloc();
             var paint = NVGPaint.malloc()) {

            log.debug("GLImage.render() - drawing at ({}, {}) size {}x{}, imageHandle: {}",
                x, y, width, height, imageHandle);

            // Draw border
            renderer.strokeRoundedRect(x, y, width, height, 4, borderColor, 1.0f);

            if (imageHandle > 0) {
                // Calculate rendering area maintaining aspect ratio if needed
                float drawX = x;
                float drawY = y;
                float drawWidth = width;
                float drawHeight = height;

                if (maintainAspectRatio && imageWidth > 0 && imageHeight > 0) {
                    float aspectRatio = (float) imageWidth / imageHeight;
                    float containerAspect = (float) width / height;

                    if (aspectRatio > containerAspect) {
                        // Image is wider - fit to width
                        drawHeight = width / aspectRatio;
                        drawY = y + (height - drawHeight) / 2;
                    } else {
                        // Image is taller - fit to height
                        drawWidth = height * aspectRatio;
                        drawX = x + (width - drawWidth) / 2;
                    }
                }

                // Create image pattern (NanoVG-specific)
                nvgImagePattern(nvg, drawX, drawY, drawWidth, drawHeight, 0, imageHandle, 1.0f, paint);

                // Draw image (NanoVG-specific)
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, drawX, drawY, drawWidth, drawHeight, 2);
                nvgFillPaint(nvg, paint);
                nvgFill(nvg);
            } else {
                // No image - draw placeholder
                renderer.fillRoundedRect(x, y, width, height, 4, GLColour.DARK_GRAY);
                renderer.setFont(-1, 12.0f);
                renderer.drawText(x + width / 2.0f, y + height / 2.0f, "No Image",
                        GLColour.TEXT_SECONDARY, Renderer.ALIGN_CENTER | Renderer.ALIGN_MIDDLE);
            }
        }
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        return visible && contains(mx, my);
    }
}
