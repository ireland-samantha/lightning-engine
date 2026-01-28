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


package ca.samanthaireland.lightning.engine.core.container;

import java.nio.file.Path;
import java.util.List;

/**
 * Configuration for creating an execution container.
 *
 * @param name               Human-readable name for the container
 * @param maxEntities        Maximum number of entities the ECS store can hold
 * @param maxComponents      Maximum number of component types
 * @param maxCommandsPerTick Maximum commands to process per tick
 * @param maxMemoryMb        Maximum memory allocation in megabytes (0 = unlimited/JVM default)
 * @param moduleJarPaths     Specific JAR files to load (optional)
 * @param moduleScanDirectory Directory to scan for module JARs (optional)
 */
public record ContainerConfig(
        String name,
        int maxEntities,
        int maxComponents,
        int maxCommandsPerTick,
        long maxMemoryMb,
        List<String> moduleJarPaths,
        Path moduleScanDirectory
) {
    /**
     * Default configuration values.
     */
    public static final int DEFAULT_MAX_ENTITIES = 1_000_000;
    public static final int DEFAULT_MAX_COMPONENTS = 100;
    public static final int DEFAULT_MAX_COMMANDS_PER_TICK = 10_000;
    public static final long DEFAULT_MAX_MEMORY_MB = 0; // 0 = unlimited (use JVM heap)

    /**
     * Creates a default configuration with the given name.
     *
     * @param name the container name
     * @return a ContainerConfig with default values
     */
    public static ContainerConfig withDefaults(String name) {
        return new ContainerConfig(
                name,
                DEFAULT_MAX_ENTITIES,
                DEFAULT_MAX_COMPONENTS,
                DEFAULT_MAX_COMMANDS_PER_TICK,
                DEFAULT_MAX_MEMORY_MB,
                List.of(),
                null
        );
    }

    /**
     * Creates a builder for fluent configuration.
     *
     * @param name the container name
     * @return a new builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private int maxEntities = DEFAULT_MAX_ENTITIES;
        private int maxComponents = DEFAULT_MAX_COMPONENTS;
        private int maxCommandsPerTick = DEFAULT_MAX_COMMANDS_PER_TICK;
        private long maxMemoryMb = DEFAULT_MAX_MEMORY_MB;
        private List<String> moduleJarPaths = List.of();
        private Path moduleScanDirectory = null;

        private Builder(String name) {
            this.name = name;
        }

        public Builder maxEntities(int maxEntities) {
            this.maxEntities = maxEntities;
            return this;
        }

        public Builder maxComponents(int maxComponents) {
            this.maxComponents = maxComponents;
            return this;
        }

        public Builder maxCommandsPerTick(int maxCommandsPerTick) {
            this.maxCommandsPerTick = maxCommandsPerTick;
            return this;
        }

        /**
         * Sets the maximum memory allocation for this container in megabytes.
         * The value will be bounded by the JVM's available heap at runtime.
         * A value of 0 means unlimited (use JVM default).
         *
         * @param maxMemoryMb maximum memory in MB
         * @return this builder
         */
        public Builder maxMemoryMb(long maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
            return this;
        }

        public Builder moduleJarPaths(List<String> paths) {
            this.moduleJarPaths = paths;
            return this;
        }

        public Builder moduleScanDirectory(Path directory) {
            this.moduleScanDirectory = directory;
            return this;
        }

        public ContainerConfig build() {
            return new ContainerConfig(
                    name,
                    maxEntities,
                    maxComponents,
                    maxCommandsPerTick,
                    maxMemoryMb,
                    moduleJarPaths,
                    moduleScanDirectory
            );
        }
    }
}
