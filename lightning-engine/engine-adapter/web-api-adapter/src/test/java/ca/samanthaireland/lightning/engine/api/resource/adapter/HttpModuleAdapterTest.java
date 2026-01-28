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


package ca.samanthaireland.lightning.engine.api.resource.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class HttpModuleAdapterTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> stringResponse;

    @Mock
    private HttpResponse<Void> voidResponse;

    private ModuleAdapter.HttpModuleAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new ModuleAdapter.HttpModuleAdapter("http://localhost:8080", httpClient);
    }

    @Test
    void getAllModules_success() throws Exception {
        when(stringResponse.statusCode()).thenReturn(200);
        when(stringResponse.body()).thenReturn("[{\"name\":\"chess\",\"flagComponentName\":\"ChessFlag\",\"enabledMatchCount\":2}]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        List<ModuleAdapter.ModuleResponse> result = adapter.getAllModules();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("chess");
        assertThat(result.get(0).flagComponentName()).isEqualTo("ChessFlag");
        assertThat(result.get(0).enabledMatchCount()).isEqualTo(2);
    }

    @Test
    void getAllModules_empty() throws Exception {
        when(stringResponse.statusCode()).thenReturn(200);
        when(stringResponse.body()).thenReturn("[]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        List<ModuleAdapter.ModuleResponse> result = adapter.getAllModules();

        assertThat(result).isEmpty();
    }

    @Test
    void getModule_found() throws Exception {
        when(stringResponse.statusCode()).thenReturn(200);
        when(stringResponse.body()).thenReturn("{\"name\":\"chess\",\"flagComponentName\":\"ChessFlag\",\"enabledMatchCount\":5}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        Optional<ModuleAdapter.ModuleResponse> result = adapter.getModule("chess");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("chess");
        assertThat(result.get().enabledMatchCount()).isEqualTo(5);
    }

    @Test
    void getModule_notFound() throws Exception {
        when(stringResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        Optional<ModuleAdapter.ModuleResponse> result = adapter.getModule("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void uploadModule_success() throws Exception {
        when(stringResponse.statusCode()).thenReturn(201);
        when(stringResponse.body()).thenReturn("[{\"name\":\"newmodule\",\"flagComponentName\":null,\"enabledMatchCount\":0}]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        byte[] jarData = "fake jar content".getBytes();
        List<ModuleAdapter.ModuleResponse> result = adapter.uploadModule("test.jar", jarData);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("newmodule");
    }

    @Test
    void uploadModule_failure() throws Exception {
        when(stringResponse.statusCode()).thenReturn(400);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        assertThatThrownBy(() -> adapter.uploadModule("invalid.txt", new byte[0]))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("status: 400");
    }

    @Test
    void uninstallModule_success() throws Exception {
        when(voidResponse.statusCode()).thenReturn(204);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(voidResponse);

        boolean result = adapter.uninstallModule("chess");

        assertThat(result).isTrue();
    }

    @Test
    void uninstallModule_notFound() throws Exception {
        when(voidResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(voidResponse);

        boolean result = adapter.uninstallModule("nonexistent");

        assertThat(result).isFalse();
    }

    @Test
    void reloadModules_success() throws Exception {
        when(stringResponse.statusCode()).thenReturn(200);
        when(stringResponse.body()).thenReturn("[]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        List<ModuleAdapter.ModuleResponse> result = adapter.reloadModules();

        assertThat(result).isEmpty();
    }

    @Test
    void interrupted_throwsIOException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Interrupted"));

        assertThatThrownBy(() -> adapter.getAllModules())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("interrupted");
    }
}
