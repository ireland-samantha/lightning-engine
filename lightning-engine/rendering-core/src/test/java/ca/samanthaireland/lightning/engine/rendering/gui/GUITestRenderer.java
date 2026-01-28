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


package ca.samanthaireland.lightning.engine.rendering.gui;

import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLColour;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.NanoVGRenderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Test renderer for UI components using off-screen framebuffer rendering.
 * Used for automated UI testing without displaying a visible window.
 */
public class GUITestRenderer implements AutoCloseable {

    private final int width;
    private final int height;
    private long windowHandle;
    private long nvgContext;
    private int defaultFontId = -1;
    private int framebuffer;
    private int colorTexture;
    private int depthStencilBuffer;
    private ByteBuffer pixelBuffer;
    private boolean initialized = false;

    /**
     * Retains font data buffer to prevent garbage collection.
     * NanoVG requires font data to remain valid when using nvgCreateFontMem with freeData=false.
     */
    private ByteBuffer fontDataBuffer;

    public GUITestRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Initialize the test renderer with an off-screen context.
     */
    public void init() {
        if (initialized) {
            return;
        }

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Create hidden window for OpenGL context
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Hidden window

        windowHandle = glfwCreateWindow(width, height, "Test Renderer", 0, 0);
        if (windowHandle == 0) {
            throw new RuntimeException("Failed to create test window");
        }

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();

        // Create framebuffer for off-screen rendering
        createFramebuffer();

        // Initialize NanoVG
        nvgContext = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (nvgContext == 0) {
            throw new RuntimeException("Failed to create NanoVG context");
        }

        // Load fonts
        loadFonts();

        // Allocate pixel buffer for reading back pixels
        pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);

        initialized = true;
    }

    private void createFramebuffer() {
        framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        // Create color texture
        colorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);

        // Create depth/stencil renderbuffer
        depthStencilBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthStencilBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthStencilBuffer);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete: " + status);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void loadFonts() {
        // Try bundled font
        try {
            ByteBuffer fontData = loadResourceToBuffer("fonts/default.ttf");
            if (fontData != null) {
                defaultFontId = nvgCreateFontMem(nvgContext, "default", fontData, false);
                if (defaultFontId >= 0) {
                    // Retain buffer to prevent garbage collection - NanoVG needs the data alive
                    fontDataBuffer = fontData;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Try system fonts as fallback
        if (defaultFontId < 0) {
            String[] systemFonts = {
                "/System/Library/Fonts/Geneva.ttf",
                "/System/Library/Fonts/Monaco.ttf",
                "C:\\Windows\\Fonts\\arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
            };
            for (String fontPath : systemFonts) {
                try {
                    defaultFontId = nvgCreateFont(nvgContext, "default", fontPath);
                    if (defaultFontId >= 0) {
                        break;
                    }
                } catch (Exception e) {
                    // Try next
                }
            }
        }
    }

    private ByteBuffer loadResourceToBuffer(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            ReadableByteChannel channel = Channels.newChannel(in);
            ByteBuffer buffer = BufferUtils.createByteBuffer(1024);
            while (true) {
                int bytes = channel.read(buffer);
                if (bytes == -1) break;
                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Render a list of components and return the pixel data.
     */
    public void render(List<WindowComponent> components) {
        if (!initialized) {
            throw new IllegalStateException("Renderer not initialized");
        }

        // Bind framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);

        // Clear
        glClearColor(GLColour.BACKGROUND[0], GLColour.BACKGROUND[1], GLColour.BACKGROUND[2], 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        // Begin NanoVG frame
        nvgBeginFrame(nvgContext, width, height, 1.0f);

        // Set default font
        if (defaultFontId >= 0) {
            nvgFontFaceId(nvgContext, defaultFontId);
            nvgFontSize(nvgContext, 14.0f);
        }

        // Create renderer wrapper for components
        NanoVGRenderer renderer = new NanoVGRenderer(nvgContext, defaultFontId);

        // Render components
        for (WindowComponent component : components) {
            if (defaultFontId >= 0) {
                nvgFontFaceId(nvgContext, defaultFontId);
            }
            component.render(renderer);
        }

        // End NanoVG frame
        nvgEndFrame(nvgContext);

        // Unbind framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Render a single component.
     */
    public void render(WindowComponent component) {
        render(List.of(component));
    }

    /**
     * Get the color at a specific pixel location.
     * Returns [r, g, b, a] values in range 0-255.
     */
    public int[] getPixelColor(int x, int y) {
        if (!initialized) {
            throw new IllegalStateException("Renderer not initialized");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glReadPixels(x, height - 1 - y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        int r = pixelBuffer.get(0) & 0xFF;
        int g = pixelBuffer.get(1) & 0xFF;
        int b = pixelBuffer.get(2) & 0xFF;
        int a = pixelBuffer.get(3) & 0xFF;

        return new int[]{r, g, b, a};
    }

    /**
     * Check if a pixel at the given location matches the expected color within tolerance.
     */
    public boolean pixelMatches(int x, int y, int[] expectedRGBA, int tolerance) {
        int[] actual = getPixelColor(x, y);
        return Math.abs(actual[0] - expectedRGBA[0]) <= tolerance &&
               Math.abs(actual[1] - expectedRGBA[1]) <= tolerance &&
               Math.abs(actual[2] - expectedRGBA[2]) <= tolerance &&
               Math.abs(actual[3] - expectedRGBA[3]) <= tolerance;
    }

    /**
     * Check if a region contains non-background color (text or graphics).
     */
    public boolean regionHasContent(int x, int y, int regionWidth, int regionHeight) {
        int[] bgColor = {
            (int)(GLColour.BACKGROUND[0] * 255),
            (int)(GLColour.BACKGROUND[1] * 255),
            (int)(GLColour.BACKGROUND[2] * 255),
            255
        };

        for (int py = y; py < y + regionHeight; py++) {
            for (int px = x; px < x + regionWidth; px++) {
                if (!pixelMatches(px, py, bgColor, 10)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a region contains pixels matching the expected color.
     */
    public boolean regionContainsColor(int x, int y, int regionWidth, int regionHeight, int[] expectedRGBA, int tolerance) {
        for (int py = y; py < y + regionHeight; py++) {
            for (int px = x; px < x + regionWidth; px++) {
                if (pixelMatches(px, py, expectedRGBA, tolerance)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the width of the render target.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of the render target.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Check if fonts were loaded successfully.
     */
    public boolean hasFonts() {
        return defaultFontId >= 0;
    }

    @Override
    public void close() {
        if (initialized) {
            if (nvgContext != 0) {
                nvgDelete(nvgContext);
            }
            if (framebuffer != 0) {
                glDeleteFramebuffers(framebuffer);
            }
            if (colorTexture != 0) {
                glDeleteTextures(colorTexture);
            }
            if (depthStencilBuffer != 0) {
                glDeleteRenderbuffers(depthStencilBuffer);
            }
            if (windowHandle != 0) {
                glfwDestroyWindow(windowHandle);
            }
            glfwTerminate();
            initialized = false;
        }
    }
}
