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

import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

/**
 * NanoVG-based implementation of the {@link Renderer} interface.
 * This is the default renderer that wraps NanoVG for vector graphics rendering.
 */
@Slf4j
public class NanoVGRenderer implements Renderer {

    private long nvg;
    private int defaultFontId = -1;
    private final GLFontLoader fontLoader;
    private boolean initialized = false;

    // Reusable color instance to avoid allocations
    private NVGColor reusableColor;

    /**
     * Create a new NanoVG renderer.
     */
    public NanoVGRenderer() {
        this.fontLoader = null; // Will be created on init
    }

    /**
     * Create a NanoVG renderer with an existing NVG context.
     * Used for testing or when sharing contexts.
     *
     * @param nvgContext the existing NanoVG context
     * @param defaultFontId the default font ID, or -1 if none
     */
    public NanoVGRenderer(long nvgContext, int defaultFontId) {
        this.nvg = nvgContext;
        this.defaultFontId = defaultFontId;
        this.fontLoader = null;
        this.reusableColor = NVGColor.malloc();
        this.initialized = true;
    }

    /**
     * Initialize the renderer.
     * Call this after OpenGL context is created.
     *
     * @return true if initialization succeeded
     */
    public boolean init() {
        if (initialized) {
            return true;
        }

        nvg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (nvg == 0) {
            log.error("Failed to create NanoVG context");
            return false;
        }

        reusableColor = NVGColor.malloc();

        // Load default font
        GLFontLoader loader = new GLFontLoader(nvg);
        defaultFontId = loader.loadDefaultFont();
        if (defaultFontId >= 0) {
            nvgFontFaceId(nvg, defaultFontId);
        }

        initialized = true;
        log.debug("NanoVGRenderer initialized, defaultFontId={}", defaultFontId);
        return true;
    }

    /**
     * Get the NanoVG context handle.
     * For use by components that need direct NanoVG access.
     */
    public long getNvg() {
        return nvg;
    }

    // ========== Lifecycle ==========

    @Override
    public void beginFrame(int width, int height, float pixelRatio) {
        if (!initialized) {
            init();
        }
        nvgBeginFrame(nvg, width, height, pixelRatio);
        GLContext.begin(nvg, defaultFontId, null);
    }

    @Override
    public void endFrame() {
        GLContext.end();
        nvgEndFrame(nvg);
    }

    @Override
    public void dispose() {
        if (reusableColor != null) {
            reusableColor.free();
            reusableColor = null;
        }
        if (nvg != 0) {
            nvgDelete(nvg);
            nvg = 0;
        }
        initialized = false;
    }

    // ========== Rectangles ==========

    @Override
    public void fillRect(float x, float y, float width, float height, float[] color) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, width, height);
        nvgFillColor(nvg, GLColour.rgba(color, reusableColor));
        nvgFill(nvg);
    }

    @Override
    public void fillRoundedRect(float x, float y, float width, float height, float radius, float[] color) {
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, width, height, radius);
        nvgFillColor(nvg, GLColour.rgba(color, reusableColor));
        nvgFill(nvg);
    }

    @Override
    public void strokeRect(float x, float y, float width, float height, float[] color, float strokeWidth) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, width, height);
        nvgStrokeColor(nvg, GLColour.rgba(color, reusableColor));
        nvgStrokeWidth(nvg, strokeWidth);
        nvgStroke(nvg);
    }

    @Override
    public void strokeRoundedRect(float x, float y, float width, float height, float radius, float[] color, float strokeWidth) {
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, width, height, radius);
        nvgStrokeColor(nvg, GLColour.rgba(color, reusableColor));
        nvgStrokeWidth(nvg, strokeWidth);
        nvgStroke(nvg);
    }

    // ========== Text ==========

    @Override
    public void setFont(int fontId, float fontSize) {
        int effectiveFontId = fontId >= 0 ? fontId : defaultFontId;
        if (effectiveFontId >= 0) {
            nvgFontFaceId(nvg, effectiveFontId);
        }
        nvgFontSize(nvg, fontSize);
    }

    @Override
    public void drawText(float x, float y, String text, float[] color, int align) {
        nvgTextAlign(nvg, translateAlign(align));
        nvgFillColor(nvg, GLColour.rgba(color, reusableColor));
        nvgText(nvg, x, y, text);
    }

    @Override
    public float measureText(String text) {
        return nvgTextBounds(nvg, 0, 0, text, (float[]) null);
    }

    @Override
    public float measureTextBounds(String text, float[] bounds) {
        return nvgTextBounds(nvg, 0, 0, text, bounds);
    }

    // ========== Font Management ==========

    @Override
    public int loadFont(String name, String path) {
        return nvgCreateFont(nvg, name, path);
    }

    @Override
    public int getDefaultFontId() {
        return defaultFontId;
    }

    // ========== Clipping ==========

    @Override
    public void pushClip(float x, float y, float width, float height) {
        nvgSave(nvg);
        nvgScissor(nvg, x, y, width, height);
    }

    @Override
    public void intersectClip(float x, float y, float width, float height) {
        nvgIntersectScissor(nvg, x, y, width, height);
    }

    @Override
    public void popClip() {
        nvgRestore(nvg);
    }

    // ========== Transform ==========

    @Override
    public void save() {
        nvgSave(nvg);
    }

    @Override
    public void restore() {
        nvgRestore(nvg);
    }

    @Override
    public void translate(float x, float y) {
        nvgTranslate(nvg, x, y);
    }

    @Override
    public void scale(float sx, float sy) {
        nvgScale(nvg, sx, sy);
    }

    @Override
    public void rotate(float angle) {
        nvgRotate(nvg, angle);
    }

    @Override
    public void resetTransform() {
        nvgResetTransform(nvg);
    }

    // ========== Lines ==========

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float[] color, float strokeWidth) {
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x1, y1);
        nvgLineTo(nvg, x2, y2);
        nvgStrokeColor(nvg, GLColour.rgba(color, reusableColor));
        nvgStrokeWidth(nvg, strokeWidth);
        nvgStroke(nvg);
    }

    // ========== Triangles ==========

    @Override
    public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, float[] color) {
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x1, y1);
        nvgLineTo(nvg, x2, y2);
        nvgLineTo(nvg, x3, y3);
        nvgClosePath(nvg);
        nvgFillColor(nvg, GLColour.rgba(color, reusableColor));
        nvgFill(nvg);
    }

    // ========== Internal ==========

    /**
     * Translate our alignment constants to NanoVG alignment flags.
     */
    private int translateAlign(int align) {
        int nvgAlign = 0;

        // Horizontal alignment
        if ((align & ALIGN_LEFT) != 0) nvgAlign |= NVG_ALIGN_LEFT;
        else if ((align & ALIGN_CENTER) != 0) nvgAlign |= NVG_ALIGN_CENTER;
        else if ((align & ALIGN_RIGHT) != 0) nvgAlign |= NVG_ALIGN_RIGHT;

        // Vertical alignment
        if ((align & ALIGN_TOP) != 0) nvgAlign |= NVG_ALIGN_TOP;
        else if ((align & ALIGN_MIDDLE) != 0) nvgAlign |= NVG_ALIGN_MIDDLE;
        else if ((align & ALIGN_BOTTOM) != 0) nvgAlign |= NVG_ALIGN_BOTTOM;
        else if ((align & ALIGN_BASELINE) != 0) nvgAlign |= NVG_ALIGN_BASELINE;

        return nvgAlign;
    }
}
