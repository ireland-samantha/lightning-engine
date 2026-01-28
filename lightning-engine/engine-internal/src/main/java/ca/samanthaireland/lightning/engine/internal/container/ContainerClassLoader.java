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


package ca.samanthaireland.lightning.engine.internal.container;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * Custom classloader for container isolation.
 *
 * <p>Uses a hybrid delegation strategy:
 * <ul>
 *   <li><b>Parent-first</b> for engine core classes - Ensures shared APIs</li>
 *   <li><b>Child-first</b> for module classes - Enables isolation</li>
 * </ul>
 *
 * <p>This allows each container to have its own copy of module classes
 * while sharing the engine core APIs.
 */
@Slf4j
public class ContainerClassLoader extends URLClassLoader implements Closeable {

    /**
     * Packages that should always be loaded from the parent classloader.
     * These are shared APIs that must be consistent across all containers.
     */
    private static final Set<String> PARENT_FIRST_PACKAGES = Set.of(
            "ca.samanthaireland.lightning.engine.core",
            "ca.samanthaireland.lightning.engine.ext.module",
            "ca.samanthaireland.lightning.engine.ext.ai",
            "java.",
            "javax.",
            "jakarta.",
            "sun.",
            "jdk.",
            "org.slf4j",
            "ch.qos.logback",
            "lombok"
    );

    private final long containerId;
    private volatile boolean closed = false;

    /**
     * Creates a new container classloader.
     *
     * @param containerId the container ID (for logging)
     * @param urls        URLs to module JARs
     * @param parent      the parent classloader (typically the application classloader)
     */
    public ContainerClassLoader(long containerId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.containerId = containerId;
        log.debug("Created ContainerClassLoader for container {} with {} JAR URLs", containerId, urls.length);
    }

    /**
     * Creates a new empty container classloader.
     *
     * @param containerId the container ID
     * @param parent      the parent classloader
     */
    public ContainerClassLoader(long containerId, ClassLoader parent) {
        this(containerId, new URL[0], parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            if (closed) {
                throw new ClassNotFoundException("ClassLoader is closed: " + name);
            }

            // Check if already loaded
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            // Parent-first for engine APIs and JDK classes
            if (shouldLoadFromParent(name)) {
                log.trace("Container {}: Loading {} from parent (parent-first)", containerId, name);
                return getParent().loadClass(name);
            }

            // Child-first for module classes
            try {
                c = findClass(name);
                log.trace("Container {}: Loaded {} from container classloader (child-first)", containerId, name);
            } catch (ClassNotFoundException e) {
                // Fall back to parent if not found in child
                log.trace("Container {}: {} not found in container, falling back to parent", containerId, name);
                c = getParent().loadClass(name);
            }

            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    /**
     * Determines if a class should be loaded from the parent classloader.
     *
     * @param name the fully qualified class name
     * @return true if the class should be loaded from parent
     */
    private boolean shouldLoadFromParent(String name) {
        for (String prefix : PARENT_FIRST_PACKAGES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a JAR URL to this classloader.
     *
     * @param url the JAR URL to add
     */
    @Override
    public void addURL(URL url) {
        super.addURL(url);
        log.debug("Container {}: Added JAR URL: {}", containerId, url);
    }

    /**
     * Returns the container ID associated with this classloader.
     *
     * @return the container ID
     */
    public long getContainerId() {
        return containerId;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
        log.debug("Closed ContainerClassLoader for container {}", containerId);
    }

    /**
     * Checks if this classloader has been closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }
}
