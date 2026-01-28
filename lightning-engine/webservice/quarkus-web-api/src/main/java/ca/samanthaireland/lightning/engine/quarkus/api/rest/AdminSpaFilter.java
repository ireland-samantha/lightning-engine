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

package ca.samanthaireland.lightning.engine.quarkus.api.rest;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * SPA fallback filter for the admin dashboard.
 * <p>
 * Handles client-side routing by serving /admin/dashboard/index.html for any
 * request to /admin/** that doesn't match a static file or API endpoint.
 * This allows the React router to handle paths like /admin/dashboard, /admin/users, etc.
 */
@ApplicationScoped
public class AdminSpaFilter {

    private static final String ADMIN_PREFIX = "/admin";
    private static final String ADMIN_INDEX_PATH = "/admin/dashboard/index.html";

    /**
     * Route filter that intercepts admin requests and rewrites non-static paths
     * to the SPA index.html. The filter runs early (priority 100) to rewrite
     * the path before static resource handling.
     */
    @RouteFilter(100)
    void spaFallback(RoutingContext rc) {
        String path = rc.normalizedPath();

        if (shouldRewriteToIndex(path)) {
            rc.reroute(ADMIN_INDEX_PATH);
        } else {
            rc.next();
        }
    }

    private boolean shouldRewriteToIndex(String path) {
        if (!path.startsWith(ADMIN_PREFIX)) {
            return false;
        }

        // Don't rewrite if it's already the index.html
        if (path.equals(ADMIN_INDEX_PATH)) {
            return false;
        }

        // Don't rewrite requests for static assets (files with extensions)
        if (hasFileExtension(path)) {
            return false;
        }

        // Don't rewrite API requests
        if (path.startsWith("/admin/api")) {
            return false;
        }

        return true;
    }

    private boolean hasFileExtension(String path) {
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');

        // Has extension if dot comes after last slash and isn't at the end
        return lastDot > lastSlash && lastDot < path.length() - 1;
    }
}
