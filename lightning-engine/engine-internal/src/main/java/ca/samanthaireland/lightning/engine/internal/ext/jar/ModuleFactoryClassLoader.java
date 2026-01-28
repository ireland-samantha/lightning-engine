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


package ca.samanthaireland.lightning.engine.internal.ext.jar;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Generic JAR file loader for factory implementations.
 *
 * <p>This class scans JAR files for classes implementing a specific factory interface
 * and provides methods to instantiate them. It abstracts the common JAR loading logic
 * used by both modules and AIs.
 *
 * @param <FACTORY> the factory interface type to load
 */
@Slf4j
public class ModuleFactoryClassLoader<FACTORY> implements FactoryClassloader<FACTORY>, Closeable {

    private final Class<FACTORY> factoryInterface;
    private final String factoryTypeName;
    private final List<URLClassLoader> openClassLoaders = new CopyOnWriteArrayList<>();

    /**
     * Create a new JAR factory loader.
     *
     * @param factoryInterface the factory interface class to search for
     * @param factoryTypeName human-readable name for logging (e.g., "ModuleFactory", "AIFactory")
     */
    public ModuleFactoryClassLoader(Class<FACTORY> factoryInterface, String factoryTypeName) {
        this.factoryInterface = factoryInterface;
        this.factoryTypeName = factoryTypeName;
    }

    /**
     * Loads all implementations of <F> from a jarfile
     *
     * @param jarFile the JAR file to load
     * @return list of factory instances found in the JAR
     * @throws IOException if the JAR file cannot be read
     */
    @Override
    public List<FACTORY> loadFactoriesFromJar(File jarFile) throws IOException {
        List<FACTORY> factories = new ArrayList<>();

        if (!jarFile.exists() || !jarFile.isFile() || !jarFile.getName().endsWith(".jar")) {
            log.warn("Invalid JAR file: {}", jarFile);
            return factories;
        }

        URL jarUrl = jarFile.toURI().toURL();

        // We don't close the classloader immediately because the factory
        // instances may reference inner classes that need to be loaded later.
        // The classloader must remain open for the factory's lifetime.
        // Classloaders are tracked and closed when this loader is closed.
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
        openClassLoaders.add(classLoader);

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class") && !name.contains("$")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    Optional<FACTORY> factory = tryLoadFactory(classLoader, className);
                    factory.ifPresent(factories::add);
                }
            }
        }

        log.info("Loaded {} {} implementations from {}", factories.size(), factoryTypeName, jarFile.getName());
        return factories;
    }

    /**
     * Try to load a class and instantiate it as a factory.
     *
     * @param classLoader the class loader to use
     * @param className the fully qualified class name
     * @return Optional containing the factory if successful
     */
    private Optional<FACTORY> tryLoadFactory(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = classLoader.loadClass(className);

            if (factoryInterface.isAssignableFrom(clazz) && !clazz.isInterface()) {
                @SuppressWarnings("unchecked")
                FACTORY factory = (FACTORY) clazz.getDeclaredConstructor().newInstance();
                log.debug("Found {}: {}", factoryTypeName, className);
                return Optional.of(factory);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            log.trace("Class not found: {}", className);
        } catch (NoSuchMethodException e) {
            log.warn("{} {} does not have a no-arg constructor", factoryTypeName, className);
        } catch (Exception e) {
            log.warn("Failed to instantiate {} {}: {}", factoryTypeName, className, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Close all class loaders opened by this factory loader.
     *
     * <p>After calling this method, factory instances loaded by this loader
     * may no longer be able to load additional classes (e.g., inner classes).
     * Only call this when all factories are no longer needed.
     */
    @Override
    public void close() {
        for (URLClassLoader classLoader : openClassLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn("Failed to close class loader: {}", e.getMessage());
            }
        }
        openClassLoaders.clear();
        log.debug("Closed {} class loaders for {}", openClassLoaders.size(), factoryTypeName);
    }
}
