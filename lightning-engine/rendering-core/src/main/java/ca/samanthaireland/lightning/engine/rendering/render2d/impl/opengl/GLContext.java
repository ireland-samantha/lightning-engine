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

import ca.samanthaireland.lightning.engine.rendering.render2d.Window;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;

/**
 * Thread-local context for GUI rendering.
 * Provides access to shared resources like fonts during the render pass.
 */
public final class GLContext {

    private static final ThreadLocal<GLContext> CURRENT = new ThreadLocal<>();

    private int defaultFontId = -1;
    private long nvgContext;
    private Window window;

    private GLContext() {
    }

    /**
     * Get the current GUI context for this thread.
     * Returns null if no context is active.
     */
    public static GLContext current() {
        return CURRENT.get();
    }

    /**
     * Get the default font ID.
     * Returns -1 if no font is available.
     */
    public static int getDefaultFontId() {
        GLContext ctx = CURRENT.get();
        return ctx != null ? ctx.defaultFontId : -1;
    }

    /**
     * Get the NanoVG context handle.
     */
    public static long getNvgContext() {
        GLContext ctx = CURRENT.get();
        return ctx != null ? ctx.nvgContext : 0;
    }

    /**
     * Get the current window.
     */
    public static Window getWindow() {
        GLContext ctx = CURRENT.get();
        return ctx != null ? ctx.window : null;
    }

    /**
     * Show an overlay component on top of everything else.
     */
    public static void showOverlay(WindowComponent overlay) {
        GLContext ctx = CURRENT.get();
        if (ctx != null && ctx.window != null) {
            ctx.window.showOverlay(overlay);
        }
    }

    /**
     * Hide an overlay component.
     */
    public static void hideOverlay(WindowComponent overlay) {
        GLContext ctx = CURRENT.get();
        if (ctx != null && ctx.window != null) {
            ctx.window.hideOverlay(overlay);
        }
    }

    /**
     * Begin a render context. Call this at the start of the render loop.
     */
    public static void begin(long nvgContext, int defaultFontId) {
        begin(nvgContext, defaultFontId, null);
    }

    /**
     * Begin a render context with window reference.
     */
    public static void begin(long nvgContext, int defaultFontId, Window window) {
        GLContext ctx = CURRENT.get();
        if (ctx == null) {
            ctx = new GLContext();
            CURRENT.set(ctx);
        }
        ctx.nvgContext = nvgContext;
        ctx.defaultFontId = defaultFontId;
        ctx.window = window;
    }

    /**
     * End the render context.
     * Note: We keep the window reference so that input handlers can access it
     * for overlay management (context menus, dialogs, etc.) between frames.
     */
    public static void end() {
        // Keep the context around for reuse, just clear the nvg context
        GLContext ctx = CURRENT.get();
        if (ctx != null) {
            ctx.nvgContext = 0;
            // Keep window reference for input event handling between frames
        }
    }
}
