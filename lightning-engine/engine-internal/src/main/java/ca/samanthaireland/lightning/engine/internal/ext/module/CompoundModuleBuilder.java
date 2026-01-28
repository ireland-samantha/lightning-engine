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

package ca.samanthaireland.lightning.engine.internal.ext.module;

import ca.samanthaireland.lightning.engine.ext.module.CompoundModule;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleIdentifier;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fluent builder for creating compound modules.
 *
 * <p>Example usage:
 * <pre>{@code
 * CompoundModule physics = CompoundModuleBuilder.create("PhysicsModule")
 *     .version(0, 2)
 *     .requireModule("GridMapModule", 0, 2)
 *     .requireModule("RigidBodyModule", 0, 1)
 *     .build(moduleResolver);
 * }</pre>
 */
public class CompoundModuleBuilder {

    private final String name;
    private ModuleVersion version = ModuleVersion.of(1, 0);
    private final List<ModuleIdentifier> componentRequirements = new ArrayList<>();

    private CompoundModuleBuilder(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be null or blank");
        }
        this.name = name;
    }

    /**
     * Start building a compound module with the given name.
     *
     * @param name the compound module name
     * @return a new builder
     */
    public static CompoundModuleBuilder create(String name) {
        return new CompoundModuleBuilder(name);
    }

    /**
     * Set the version of the compound module.
     *
     * @param major the major version
     * @param minor the minor version
     * @return this builder
     */
    public CompoundModuleBuilder version(int major, int minor) {
        this.version = ModuleVersion.of(major, minor);
        return this;
    }

    /**
     * Set the version of the compound module.
     *
     * @param major the major version
     * @param minor the minor version
     * @param patch the patch version
     * @return this builder
     */
    public CompoundModuleBuilder version(int major, int minor, int patch) {
        this.version = ModuleVersion.of(major, minor, patch);
        return this;
    }

    /**
     * Set the version of the compound module.
     *
     * @param version the module version
     * @return this builder
     */
    public CompoundModuleBuilder version(ModuleVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        this.version = version;
        return this;
    }

    /**
     * Add a required component module.
     *
     * @param moduleName the name of the required module
     * @param major the minimum required major version
     * @param minor the minimum required minor version
     * @return this builder
     */
    public CompoundModuleBuilder requireModule(String moduleName, int major, int minor) {
        return requireModule(ModuleIdentifier.of(moduleName, major, minor));
    }

    /**
     * Add a required component module.
     *
     * @param identifier the module identifier (name + required version)
     * @return this builder
     */
    public CompoundModuleBuilder requireModule(ModuleIdentifier identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Module identifier cannot be null");
        }
        componentRequirements.add(identifier);
        return this;
    }

    /**
     * Add a required component module by parsing a spec string.
     *
     * @param spec the module spec (e.g., "physics:0.2")
     * @return this builder
     */
    public CompoundModuleBuilder requireModule(String spec) {
        return requireModule(ModuleIdentifier.parse(spec));
    }

    /**
     * Build the compound module, resolving all component modules.
     *
     * @param resolver the module resolver to use for resolving components
     * @return the built compound module
     * @throws ModuleResolutionException if any component module cannot be resolved
     */
    public CompoundModule build(ModuleResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("Module resolver cannot be null");
        }
        if (componentRequirements.isEmpty()) {
            throw new IllegalStateException("Compound module must have at least one component");
        }

        List<EngineModule> resolvedComponents = new ArrayList<>();

        for (ModuleIdentifier required : componentRequirements) {
            EngineModule resolved = resolver.resolveModule(required.name());
            if (resolved == null) {
                throw new ModuleResolutionException(
                        "Failed to resolve component module: " + required.name());
            }

            ModuleVersion actualVersion = resolved.getVersion();
            if (!actualVersion.isCompatibleWith(required.version())) {
                throw new ModuleResolutionException(
                        "Version mismatch for module '" + required.name() +
                                "': required " + required.version() +
                                " but found " + actualVersion);
            }

            resolvedComponents.add(resolved);
        }

        return new BuiltCompoundModule(name, version, componentRequirements, resolvedComponents);
    }

    /**
     * Build the compound module with pre-resolved components.
     *
     * <p>Use this method when you have already resolved the component modules
     * and want to skip the resolution step.
     *
     * @param resolvedComponents the pre-resolved component modules
     * @return the built compound module
     */
    public CompoundModule buildWithComponents(List<EngineModule> resolvedComponents) {
        if (resolvedComponents == null) {
            throw new IllegalArgumentException("Resolved components cannot be null");
        }
        if (componentRequirements.isEmpty()) {
            throw new IllegalStateException("Compound module must have at least one component requirement");
        }
        if (resolvedComponents.size() != componentRequirements.size()) {
            throw new IllegalArgumentException(
                    "Number of resolved components (" + resolvedComponents.size() +
                            ") must match number of requirements (" + componentRequirements.size() + ")");
        }

        return new BuiltCompoundModule(name, version, componentRequirements, resolvedComponents);
    }

    /**
     * Internal implementation of CompoundModule created by the builder.
     */
    private static class BuiltCompoundModule extends AbstractCompoundModule {
        BuiltCompoundModule(
                String name,
                ModuleVersion version,
                List<ModuleIdentifier> componentIdentifiers,
                List<EngineModule> resolvedComponents) {
            super(name, version, componentIdentifiers, resolvedComponents);
        }
    }

    /**
     * Exception thrown when module resolution fails.
     */
    public static class ModuleResolutionException extends RuntimeException {
        public ModuleResolutionException(String message) {
            super(message);
        }

        public ModuleResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
