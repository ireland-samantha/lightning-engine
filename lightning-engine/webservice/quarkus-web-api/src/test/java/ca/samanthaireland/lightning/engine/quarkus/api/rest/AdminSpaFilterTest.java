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

import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminSpaFilterTest {

    private AdminSpaFilter filter;
    private RoutingContext routingContext;

    @BeforeEach
    void setUp() {
        filter = new AdminSpaFilter();
        routingContext = mock(RoutingContext.class);
    }

    @Nested
    class ShouldRerouteToIndex {

        @Test
        void adminDashboardPath() {
            when(routingContext.normalizedPath()).thenReturn("/admin/dashboard");

            filter.spaFallback(routingContext);

            verify(routingContext).reroute("/admin/dashboard/index.html");
            verify(routingContext, never()).next();
        }

        @Test
        void adminUsersPath() {
            when(routingContext.normalizedPath()).thenReturn("/admin/users");

            filter.spaFallback(routingContext);

            verify(routingContext).reroute("/admin/dashboard/index.html");
        }

        @Test
        void adminRootPath() {
            when(routingContext.normalizedPath()).thenReturn("/admin");

            filter.spaFallback(routingContext);

            verify(routingContext).reroute("/admin/dashboard/index.html");
        }

        @Test
        void adminNestedPath() {
            when(routingContext.normalizedPath()).thenReturn("/admin/settings/profile");

            filter.spaFallback(routingContext);

            verify(routingContext).reroute("/admin/dashboard/index.html");
        }
    }

    @Nested
    class ShouldNotReroute {

        @Test
        void nonAdminPath() {
            when(routingContext.normalizedPath()).thenReturn("/api/health");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void rootPath() {
            when(routingContext.normalizedPath()).thenReturn("/");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void staticJsFile() {
            when(routingContext.normalizedPath()).thenReturn("/admin/dashboard/assets/index-abc123.js");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void staticCssFile() {
            when(routingContext.normalizedPath()).thenReturn("/admin/dashboard/assets/style.css");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void staticHtmlFile() {
            when(routingContext.normalizedPath()).thenReturn("/admin/dashboard/some-page.html");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void indexHtmlDirectly() {
            when(routingContext.normalizedPath()).thenReturn("/admin/dashboard/index.html");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void adminApiEndpoint() {
            when(routingContext.normalizedPath()).thenReturn("/admin/api/users");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void staticPngImage() {
            when(routingContext.normalizedPath()).thenReturn("/admin/assets/logo.png");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }

        @Test
        void staticSvgImage() {
            when(routingContext.normalizedPath()).thenReturn("/admin/icons/menu.svg");

            filter.spaFallback(routingContext);

            verify(routingContext, never()).reroute(anyString());
            verify(routingContext).next();
        }
    }
}
