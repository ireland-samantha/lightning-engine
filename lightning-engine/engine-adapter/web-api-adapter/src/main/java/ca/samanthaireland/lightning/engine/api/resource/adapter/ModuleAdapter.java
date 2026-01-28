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

import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.ModuleResponseDto;
import ca.samanthaireland.lightning.engine.api.resource.adapter.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

/**
 * REST API adapter for module operations.
 */
public interface ModuleAdapter {

    /**
     * Get all available modules.
     *
     * @return list of module responses
     */
    List<ModuleResponse> getAllModules() throws IOException;

    /**
     * Get a specific module by name.
     *
     * @param moduleName the module name
     * @return the module if found
     */
    Optional<ModuleResponse> getModule(String moduleName) throws IOException;

    /**
     * Upload a JAR module.
     *
     * @param fileName the JAR file name
     * @param jarData the JAR file bytes
     * @return list of all modules after upload
     */
    List<ModuleResponse> uploadModule(String fileName, byte[] jarData) throws IOException;

    /**
     * Uninstall a module.
     *
     * @param moduleName the module name
     * @return true if uninstalled
     */
    boolean uninstallModule(String moduleName) throws IOException;

    /**
     * Reload all modules from disk.
     *
     * @return list of all modules after reload
     */
    List<ModuleResponse> reloadModules() throws IOException;

    /**
     * Check if a module is installed.
     *
     * @param moduleName the module name
     * @return true if installed
     */
    default boolean hasModule(String moduleName) throws IOException {
        return getModule(moduleName).isPresent();
    }

    /**
     * Module response DTO.
     */
    record ModuleResponse(String name, String flagComponentName, int enabledMatchCount) {}

    /**
     * HTTP-based implementation.
     */
    class HttpModuleAdapter implements ModuleAdapter {
        private final HttpClient httpClient;
        private final String baseUrl;
        private final AdapterConfig config;

        public HttpModuleAdapter(String baseUrl) {
            this(baseUrl, AdapterConfig.defaults());
        }

        public HttpModuleAdapter(String baseUrl, AdapterConfig config) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.config = config;
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(config.getConnectTimeout())
                    .build();
        }

        public HttpModuleAdapter(String baseUrl, HttpClient httpClient) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.config = AdapterConfig.defaults();
            this.httpClient = httpClient;
        }

        private HttpRequest.Builder requestBuilder(String path) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path));
            if (config.hasAuthentication()) {
                builder.header("Authorization", "Bearer " + config.getBearerToken());
            }
            return builder;
        }

        @Override
        public List<ModuleResponse> getAllModules() throws IOException {
            HttpRequest request = requestBuilder("/api/modules")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<ModuleResponseDto> dtos = JsonMapper.fromJsonList(response.body(), ModuleResponseDto.class);
                    return dtos.stream()
                            .map(dto -> new ModuleResponse(dto.name(), dto.flagComponentName(), dto.enabledMatchCount()))
                            .toList();
                }
                throw new IOException("Get modules failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public Optional<ModuleResponse> getModule(String moduleName) throws IOException {
            HttpRequest request = requestBuilder("/api/modules/" + moduleName)
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    ModuleResponseDto dto = JsonMapper.fromJson(response.body(), ModuleResponseDto.class);
                    return Optional.of(new ModuleResponse(dto.name(), dto.flagComponentName(), dto.enabledMatchCount()));
                } else if (response.statusCode() == 404) {
                    return Optional.empty();
                }
                throw new IOException("Get module failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public List<ModuleResponse> uploadModule(String fileName, byte[] jarData) throws IOException {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                    "Content-Type: application/java-archive\r\n\r\n";

            byte[] prefix = header.getBytes();
            byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes();
            byte[] requestBody = new byte[prefix.length + jarData.length + suffix.length];
            System.arraycopy(prefix, 0, requestBody, 0, prefix.length);
            System.arraycopy(jarData, 0, requestBody, prefix.length, jarData.length);
            System.arraycopy(suffix, 0, requestBody, prefix.length + jarData.length, suffix.length);

            HttpRequest request = requestBuilder("/api/modules/upload")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    List<ModuleResponseDto> dtos = JsonMapper.fromJsonList(response.body(), ModuleResponseDto.class);
                    return dtos.stream()
                            .map(dto -> new ModuleResponse(dto.name(), dto.flagComponentName(), dto.enabledMatchCount()))
                            .toList();
                }
                throw new IOException("Upload module failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Upload interrupted", e);
            }
        }

        @Override
        public boolean uninstallModule(String moduleName) throws IOException {
            HttpRequest request = requestBuilder("/api/modules/" + moduleName)
                    .DELETE()
                    .build();

            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                return response.statusCode() == 204;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public List<ModuleResponse> reloadModules() throws IOException {
            HttpRequest request = requestBuilder("/api/modules/reload")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<ModuleResponseDto> dtos = JsonMapper.fromJsonList(response.body(), ModuleResponseDto.class);
                    return dtos.stream()
                            .map(dto -> new ModuleResponse(dto.name(), dto.flagComponentName(), dto.enabledMatchCount()))
                            .toList();
                }
                throw new IOException("Reload modules failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }
    }
}
