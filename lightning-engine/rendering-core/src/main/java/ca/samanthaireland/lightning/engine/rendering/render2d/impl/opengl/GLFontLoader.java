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

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;
import static org.lwjgl.nanovg.NanoVG.nvgCreateFontMem;

/**
 * Handles font loading for NanoVG rendering.
 *
 * <p>Supports loading fonts from:
 * <ul>
 *   <li>Bundled resources (fonts/default.ttf)</li>
 *   <li>System font directories (OS-specific paths)</li>
 *   <li>Custom file paths</li>
 * </ul>
 *
 * <p>This class follows SRP by separating font loading concerns from window management.
 *
 * <p><b>Important:</b> Font data buffers are retained for the lifetime of this loader
 * because NanoVG requires the font data to remain in memory when using nvgCreateFontMem
 * with freeData=false.
 */
@Slf4j
public class GLFontLoader {

    private static final String BUNDLED_FONT_PATH = "fonts/default.ttf";
    private static final int INITIAL_BUFFER_SIZE = 1024;

    private final long nvgContext;

    /**
     * Keeps font data buffers alive for the lifetime of this loader.
     * NanoVG requires the font data to remain valid when freeData=false.
     */
    private final List<ByteBuffer> fontDataBuffers = new ArrayList<>();

    /**
     * Create a new font loader for the given NanoVG context.
     *
     * @param nvgContext the NanoVG context to load fonts into
     */
    public GLFontLoader(long nvgContext) {
        this.nvgContext = nvgContext;
    }

    /**
     * Load the default font, trying bundled font first, then system fonts.
     *
     * @return the font ID, or -1 if no font could be loaded
     */
    public int loadDefaultFont() {
        // Try bundled font first
        int fontId = loadBundledFont();
        if (fontId >= 0) {
            return fontId;
        }

        // Try system fonts as fallback
        fontId = loadSystemFont();
        if (fontId >= 0) {
            return fontId;
        }

        log.warn("No font could be loaded. Text will not be rendered.");
        return -1;
    }

    /**
     * Load the bundled default font from resources.
     *
     * @return the font ID, or -1 if the font could not be loaded
     */
    public int loadBundledFont() {
        try {
            ByteBuffer fontData = loadResourceToBuffer(BUNDLED_FONT_PATH);
            if (fontData != null) {
                int fontId = nvgCreateFontMem(nvgContext, "default", fontData, false);
                if (fontId >= 0) {
                    // Retain buffer to prevent garbage collection - NanoVG needs the data alive
                    fontDataBuffers.add(fontData);
                    log.info("Loaded bundled font (DejaVu Sans), buffer size: {} bytes", fontData.capacity());
                    return fontId;
                }
            }
        } catch (Exception e) {
            log.debug("Could not load bundled font: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * Try to load a system font, attempting multiple common paths.
     *
     * @return the font ID, or -1 if no system font could be loaded
     */
    public int loadSystemFont() {
        String[] systemFontPaths = getSystemFontPaths();
        for (String fontPath : systemFontPaths) {
            try {
                int fontId = nvgCreateFont(nvgContext, "default", fontPath);
                if (fontId >= 0) {
                    log.info("Loaded system font: {}", fontPath);
                    return fontId;
                }
            } catch (Exception e) {
                // Try next font
                log.trace("Could not load font {}: {}", fontPath, e.getMessage());
            }
        }
        return -1;
    }

    /**
     * Load a custom font from a resource path.
     *
     * @param name the font name to register
     * @param resourcePath the resource path to load from
     * @return the font ID, or -1 if the font could not be loaded
     */
    public int loadFontFromResource(String name, String resourcePath) {
        try {
            ByteBuffer fontData = loadResourceToBuffer(resourcePath);
            if (fontData != null) {
                int fontId = nvgCreateFontMem(nvgContext, name, fontData, false);
                if (fontId >= 0) {
                    // Retain buffer to prevent garbage collection - NanoVG needs the data alive
                    fontDataBuffers.add(fontData);
                    log.debug("Loaded font '{}' from resource: {}, buffer size: {} bytes",
                        name, resourcePath, fontData.capacity());
                    return fontId;
                }
            }
        } catch (Exception e) {
            log.warn("Could not load font '{}' from {}: {}", name, resourcePath, e.getMessage());
        }
        return -1;
    }

    /**
     * Load a font from a file path.
     *
     * @param name the font name to register
     * @param filePath the file path to load from
     * @return the font ID, or -1 if the font could not be loaded
     */
    public int loadFontFromFile(String name, String filePath) {
        try {
            int fontId = nvgCreateFont(nvgContext, name, filePath);
            if (fontId >= 0) {
                log.debug("Loaded font '{}' from file: {}", name, filePath);
                return fontId;
            }
        } catch (Exception e) {
            log.warn("Could not load font '{}' from {}: {}", name, filePath, e.getMessage());
        }
        return -1;
    }

    /**
     * Get the system font paths for the current operating system.
     *
     * @return array of system font paths to try
     */
    public String[] getSystemFontPaths() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return getMacOsFontPaths();
        } else if (os.contains("win")) {
            return getWindowsFontPaths();
        } else {
            return getLinuxFontPaths();
        }
    }

    private String[] getMacOsFontPaths() {
        return new String[]{
            "/System/Library/Fonts/Geneva.ttf",
            "/System/Library/Fonts/Monaco.ttf",
            "/System/Library/Fonts/NewYork.ttf",
            "/System/Library/Fonts/SFCompact.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/Library/Fonts/Arial Unicode.ttf"
        };
    }

    private String[] getWindowsFontPaths() {
        return new String[]{
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\segoeui.ttf",
            "C:\\Windows\\Fonts\\tahoma.ttf",
            "C:\\Windows\\Fonts\\calibri.ttf"
        };
    }

    private String[] getLinuxFontPaths() {
        return new String[]{
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/TTF/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/freefont/FreeSans.ttf",
            "/usr/share/fonts/noto/NotoSans-Regular.ttf"
        };
    }

    /**
     * Load a resource file into a direct ByteBuffer.
     *
     * @param path the resource path
     * @return the ByteBuffer containing the resource data, or null if not found
     */
    public ByteBuffer loadResourceToBuffer(String path) {
        if (path == null) {
            return null;
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            try (ReadableByteChannel channel = Channels.newChannel(in)) {
                ByteBuffer buffer = BufferUtils.createByteBuffer(INITIAL_BUFFER_SIZE);
                while (true) {
                    int bytes = channel.read(buffer);
                    if (bytes == -1) break;
                    if (buffer.remaining() == 0) {
                        buffer = expandBuffer(buffer);
                    }
                }
                buffer.flip();
                return buffer;
            }
        } catch (IOException e) {
            log.debug("Could not load resource {}: {}", path, e.getMessage());
            return null;
        }
    }

    private ByteBuffer expandBuffer(ByteBuffer buffer) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}
