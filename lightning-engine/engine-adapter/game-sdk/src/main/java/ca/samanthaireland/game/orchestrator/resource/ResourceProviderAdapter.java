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


package ca.samanthaireland.game.orchestrator.resource;

import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;
import ca.samanthaireland.game.orchestrator.ResourceDownloadEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ResourceProvider implementation that delegates to container-scoped resource operations.
 * <p>This is the base implementation that performs direct server operations
 * without any caching. Use {@link CachingResourceProvider} to add caching.
 */
public class ResourceProviderAdapter implements ResourceProvider {

    private final ContainerAdapter.ContainerScope containerScope;
    private Consumer<ResourceDownloadEvent> downloadListener;

    /**
     * Create a new adapter wrapping a ContainerScope.
     *
     * @param containerScope the container scope for resource operations
     */
    public ResourceProviderAdapter(ContainerAdapter.ContainerScope containerScope) {
        this.containerScope = containerScope;
    }

    @Override
    public long uploadResource(String name, String type, byte[] data) throws IOException {
        return containerScope.uploadResource(name, type, data);
    }

    @Override
    public Optional<ResourceData> downloadResource(long resourceId) throws IOException {
        return containerScope.getResource(resourceId)
                .map(r -> new ResourceData(
                        r.resourceId(),
                        r.resourceName(),
                        r.resourceType(),
                        null // Container-scoped getResource doesn't return blob data
                ));
    }

    @Override
    public void deleteResource(long resourceId) throws IOException {
        containerScope.deleteResource(resourceId);
    }

    @Override
    public void ensureResource(long resourceId) {
        // No caching - this is a no-op for the base adapter
        // The CachingResourceProvider decorator handles this
    }

    @Override
    public Optional<Path> getLocalPath(long resourceId) {
        // No caching - always empty for base adapter
        return Optional.empty();
    }

    @Override
    public boolean isAvailableLocally(long resourceId) {
        // No caching - never available locally for base adapter
        return false;
    }

    @Override
    public void setDownloadListener(Consumer<ResourceDownloadEvent> listener) {
        this.downloadListener = listener;
    }

    /**
     * Get the current download listener.
     *
     * @return the download listener, or null if not set
     */
    protected Consumer<ResourceDownloadEvent> getDownloadListener() {
        return downloadListener;
    }

    @Override
    public void clearCache() {
        // No cache to clear for base adapter
    }

    @Override
    public Optional<Path> getCacheDirectory() {
        // No cache directory for base adapter
        return Optional.empty();
    }

    @Override
    public void close() {
        // No resources to release for base adapter
    }
}
