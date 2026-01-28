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

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionRegistry;
import ca.samanthaireland.lightning.engine.ext.module.CompoundModule;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.ext.module.ModuleIdentifier;
import ca.samanthaireland.lightning.engine.auth.module.ModuleAuthService;
import ca.samanthaireland.lightning.engine.auth.module.ModuleAuthToken;
import ca.samanthaireland.lightning.engine.auth.module.ModulePermissionClaimBuilder;
import ca.samanthaireland.lightning.engine.internal.core.store.ModuleScopedStore;
import ca.samanthaireland.lightning.engine.internal.ext.jar.FactoryClassloader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Manages modules located in a server directory.
 */
@Slf4j
public class OnDiskModuleManager implements ModuleManager {

    private final Path scanDirectory;
    private final FactoryClassloader<ModuleFactory> factoryClassloader;
    private final ModuleContext moduleContext;
    private final PermissionRegistry permissionRegistry;
    private final EntityComponentStore sharedStore;
    private final ModuleAuthService authService;

    private final Map<String, ModuleFactory> factoryCache = new ConcurrentHashMap<>();
    private final Map<String, EngineModule> moduleCache = new ConcurrentHashMap<>();
    private final Map<String, ModuleScopedContext> moduleContextCache = new ConcurrentHashMap<>();

    private boolean scanned = false;

    /**
     * Create an OnDiskModuleManager with a custom directory and factory classloader.
     *
     * @param scanDirectory the directory to scan for JAR files
     * @param factoryClassloader the classloader to use for loading module factories from JARs
     * @param moduleContext the module context for dependency injection
     * @param permissionRegistry the registry for component permissions
     * @param sharedStore the shared entity component store for all modules
     */
    public OnDiskModuleManager(
            Path scanDirectory,
            FactoryClassloader<ModuleFactory> factoryClassloader,
            ModuleContext moduleContext,
            PermissionRegistry permissionRegistry,
            EntityComponentStore sharedStore) {
        this.scanDirectory = scanDirectory;
        this.factoryClassloader = factoryClassloader;
        this.moduleContext = moduleContext;
        this.permissionRegistry = permissionRegistry;
        this.sharedStore = sharedStore;
        this.authService = new ModuleAuthService();
        log.info("OnDiskModuleManager initialized with JWT authentication");
    }

    /**
     * Create an OnDiskModuleManager for a container with a custom parent classloader.
     *
     * <p>This constructor is designed for container isolation, where each container
     * has its own classloader for module JAR loading.
     *
     * @param moduleContext the module context for dependency injection
     * @param permissionRegistry the registry for component permissions
     * @param scanDirectory the directory path to scan for JAR files
     * @param parentClassLoader the parent classloader for module class loading (typically a ContainerClassLoader)
     */
    public OnDiskModuleManager(
            ModuleContext moduleContext,
            PermissionRegistry permissionRegistry,
            String scanDirectory,
            ClassLoader parentClassLoader) {
        this.scanDirectory = Path.of(scanDirectory);
        this.factoryClassloader = new ca.samanthaireland.lightning.engine.internal.ext.jar.ModuleFactoryClassLoader<>(
                ModuleFactory.class, "ModuleFactory") {
            @Override
            public List<ModuleFactory> loadFactoriesFromJar(File jarFile) throws IOException {
                // Override to use the container's parent classloader
                return loadFactoriesFromJarWithParent(jarFile, parentClassLoader);
            }
        };
        this.moduleContext = moduleContext;
        this.permissionRegistry = permissionRegistry;
        this.sharedStore = moduleContext.getEntityComponentStore();
        this.authService = new ModuleAuthService();
        log.info("OnDiskModuleManager initialized for container with custom classloader");
    }

    /**
     * Load module factories from a JAR using a custom parent classloader.
     */
    private static List<ModuleFactory> loadFactoriesFromJarWithParent(File jarFile, ClassLoader parentClassLoader) throws IOException {
        List<ModuleFactory> factories = new ArrayList<>();

        if (!jarFile.exists() || !jarFile.isFile() || !jarFile.getName().endsWith(".jar")) {
            log.warn("Invalid JAR file: {}", jarFile);
            return factories;
        }

        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, parentClassLoader);

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<java.util.jar.JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class") && !name.contains("$")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (ModuleFactory.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            @SuppressWarnings("unchecked")
                            ModuleFactory factory = (ModuleFactory) clazz.getDeclaredConstructor().newInstance();
                            factories.add(factory);
                            log.debug("Found ModuleFactory: {}", className);
                        }
                    } catch (Exception e) {
                        log.trace("Could not load class {}: {}", className, e.getMessage());
                    }
                }
            }
        }

        log.info("Loaded {} ModuleFactory implementations from {}", factories.size(), jarFile.getName());
        return factories;
    }

    /**
     * Scan the JAR directory for ModuleFactory implementations.
     *
     * <p>This method scans all JAR files in the configured directory and
     * registers any ModuleFactory implementations found.
     *
     * @throws IOException if the directory cannot be read
     */
    public void reloadInstalled() throws IOException {
        log.info("Scanning for modules in: {}", scanDirectory.toAbsolutePath());

        if (!Files.exists(scanDirectory)) {
            log.warn("JAR directory does not exist: {}", scanDirectory);
            Files.createDirectories(scanDirectory);
            scanned = true;
            return;
        }

        if (!Files.isDirectory(scanDirectory)) {
            log.error("JAR path is not a directory: {}", scanDirectory);
            scanned = true;
            return;
        }

        try (Stream<Path> paths = Files.list(scanDirectory)) {
            paths.filter(path -> path.toString().endsWith(".jar"))
                    .forEach(this::loadJarFile);
        }

        scanned = true;
        log.info("Module scan complete. Found {} modules: {}", factoryCache.size(), factoryCache.keySet());
    }

    /**
     * Load a single JAR file and register its ModuleFactories.
     *
     * @param jarPath the path to the JAR file
     */
    private void loadJarFile(Path jarPath) {
        try {
            File jarFile = jarPath.toFile();
            List<ModuleFactory> factories = factoryClassloader.loadFactoriesFromJar(jarFile);

            for (ModuleFactory factory : factories) {
                initializeAndCacheModule(factory, jarFile.getName());
            }
        } catch (IOException e) {
            log.error("Failed to load JAR file: {}", jarPath, e);
        }
    }

    /**
     * Initialize a module from its factory and cache both.
     *
     * <p>This method eagerly initializes modules during scanning/registration
     * rather than lazily on first access, preventing potential server stutters.
     *
     * <p>Each module receives its own {@link ModuleScopedContext} with a
     * {@link ModuleScopedStore} that enforces module-level permissions.
     *
     * @param factory the module factory
     * @param source description of where the factory came from (for logging)
     */
    /**
     * Name of the EntityModule, which receives superuser permissions via JWT.
     * EntityModule needs to attach FLAG components from other modules during spawn.
     */
    private static final String ENTITY_MODULE_NAME = "EntityModule";

    private void initializeAndCacheModule(ModuleFactory factory, String source) {
        try {
            ModuleScopedStore emptyStore = ModuleScopedStore.createEmpty(
                    sharedStore, permissionRegistry, "initializing");
            ModuleScopedContext moduleScopedContext = new ModuleScopedContext(moduleContext, emptyStore);

            // Create the module with its scoped context
            EngineModule module = factory.create(moduleScopedContext);
            String moduleName = module.getName();

            // Get the module's declared components
            List<BaseComponent> declaredComponents = new ArrayList<>();

            // Add the flag component
            BaseComponent flagComponent = module.createFlagComponent();
            if (flagComponent != null) {
                declaredComponents.add(flagComponent);
            }

            // Add all other declared components
            List<BaseComponent> moduleComponents = module.createComponents();
            if (moduleComponents != null) {
                declaredComponents.addAll(moduleComponents);
            }

            // Register PermissionComponents with their owner module name BEFORE building JWT claims
            // This allows other modules to see these components when they build their claims
            for (BaseComponent component : declaredComponents) {
                if (component instanceof PermissionComponent permissionComponent) {
                    permissionRegistry.registerComponent(permissionComponent, moduleName);
                    log.debug("Registered permission component '{}' with level {} for module {}",
                            permissionComponent.getName(),
                            permissionComponent.getPermissionLevel(),
                            moduleName);
                }
            }

            // Build JWT permission claims
            Map<String, ModuleAuthToken.ComponentPermission> componentPermissions =
                    ModulePermissionClaimBuilder.forModule(moduleName)
                            .withOwnComponents(declaredComponents)
                            .withAccessibleComponentsFrom(moduleCache.values())
                            .build();

            // Issue JWT token for this module
            // EntityModule gets superuser privileges to attach FLAG components during spawn
            boolean isSuperuser = ENTITY_MODULE_NAME.equals(moduleName);
            ModuleAuthToken authToken = isSuperuser
                    ? authService.issueSuperuserToken(moduleName, componentPermissions)
                    : authService.issueRegularToken(moduleName, componentPermissions);

            // Create the final scoped store with the JWT token
            ModuleScopedStore finalStore = ModuleScopedStore.create(
                    sharedStore, permissionRegistry, authToken);
            moduleScopedContext.setModuleScopedStore(finalStore);

            // Register module exports in the shared context
            List<ModuleExports> exports = module.getExports();
            if (exports != null) {
                for (ModuleExports export : exports) {
                    @SuppressWarnings("unchecked")
                    Class<ModuleExports> exportClass = (Class<ModuleExports>) export.getClass();
                    moduleContext.declareModuleExports(exportClass, export);
                    log.debug("Registered module export '{}' for module {}",
                            exportClass.getSimpleName(), moduleName);
                }
            }

            if (moduleCache.containsKey(moduleName)) {
                log.warn("Duplicate module name '{}' found. Overwriting with module from {}",
                        moduleName, source);
            }

            factoryCache.put(moduleName, factory);
            moduleCache.put(moduleName, module);
            moduleContextCache.put(moduleName, moduleScopedContext);
            log.info("Registered and initialized module: {} from {} with {} components",
                    moduleName, source, declaredComponents.size());

            // Refresh tokens for all existing modules so they can access the new module's components
            refreshExistingModuleTokens(moduleName);
        } catch (Exception e) {
            log.error("Failed to initialize module from factory: {}", e.getMessage(), e);
        }
    }

    /**
     * Refresh JWT tokens for all existing modules after a new module is installed.
     *
     * <p>When a new module is installed, existing modules may need access to its
     * components (if they have READ or WRITE permission level). This method
     * re-issues JWT tokens for all existing modules with updated permission claims.
     *
     * @param newModuleName the name of the newly installed module (to skip it)
     */
    private void refreshExistingModuleTokens(String newModuleName) {
        for (Map.Entry<String, ModuleScopedContext> entry : moduleContextCache.entrySet()) {
            String existingModuleName = entry.getKey();

            // Skip the newly installed module (its token is already up-to-date)
            if (existingModuleName.equals(newModuleName)) {
                continue;
            }

            ModuleScopedContext existingContext = entry.getValue();
            EngineModule existingModule = moduleCache.get(existingModuleName);

            if (existingModule == null || existingContext == null) {
                continue;
            }

            // Get the existing module's current token
            ModuleScopedStore existingStore = existingContext.getModuleScopedStore();
            ModuleAuthToken existingToken = existingStore.getAuthToken();

            // Get the existing module's components
            List<BaseComponent> declaredComponents = new ArrayList<>();
            BaseComponent flagComponent = existingModule.createFlagComponent();
            if (flagComponent != null) {
                declaredComponents.add(flagComponent);
            }
            List<BaseComponent> moduleComponents = existingModule.createComponents();
            if (moduleComponents != null) {
                declaredComponents.addAll(moduleComponents);
            }

            // Rebuild JWT permission claims with access to all currently loaded modules
            Map<String, ModuleAuthToken.ComponentPermission> componentPermissions =
                    ModulePermissionClaimBuilder.forModule(existingModuleName)
                            .withOwnComponents(declaredComponents)
                            .withAccessibleComponentsFrom(moduleCache.values())
                            .build();

            // Refresh the JWT token using the auth service (preserves superuser status)
            ModuleAuthToken newAuthToken = authService.refreshToken(existingToken, componentPermissions);

            // Update the module's scoped store with the new token
            ModuleScopedStore newStore = ModuleScopedStore.create(
                    sharedStore, permissionRegistry, newAuthToken);
            existingContext.setModuleScopedStore(newStore);

            log.debug("Refreshed JWT token for module {} with access to new module {}",
                    existingModuleName, newModuleName);
        }
    }

    @Override
    public void installModule(Class<? extends ModuleFactory> moduleFactoryClass) {
        try {
            ModuleFactory factory = moduleFactoryClass.getDeclaredConstructor().newInstance();
            initializeAndCacheModule(factory, "class " + moduleFactoryClass.getName());
        } catch (Exception e) {
            log.error("Failed to instantiate module factory: {}", moduleFactoryClass.getName(), e);
        }
    }

    /**
     * Install a module from a JAR file by copying it to the scan directory and rescanning.
     *
     * <p>This method copies the specified JAR file to the configured scan directory,
     * then triggers a full rescan to load the new module.
     *
     * @param jarFile the path to the JAR file to install
     * @throws IOException if the file cannot be copied or the scan fails
     */
    @Override
    public void installModule(Path jarFile) throws IOException {
        if (!Files.exists(jarFile)) {
            throw new IOException("JAR file does not exist: " + jarFile);
        }

        if (!jarFile.toString().endsWith(".jar")) {
            throw new IOException("File is not a JAR file: " + jarFile);
        }

        // Ensure scan directory exists
        if (!Files.exists(scanDirectory)) {
            Files.createDirectories(scanDirectory);
        }

        // Copy JAR to scan directory
        Path targetPath = scanDirectory.resolve(jarFile.getFileName());
        Files.copy(jarFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied JAR file to scan directory: {} -> {}", jarFile, targetPath);

        // Reset and rescan to pick up the new module
        reset();
        reloadInstalled();
    }

    /**
     * Ensure the directory has been scanned before resolving modules.
     */
    private void ensureScanned() {
        if (!scanned) {
            try {
                reloadInstalled();
            } catch (IOException e) {
                log.error("Failed to scan JAR directory", e);
            }
        }
    }

    @Override
    public EngineModule resolveModule(String moduleName) {
        ensureScanned();

        EngineModule module = moduleCache.get(moduleName);
        if (module == null) {
            log.warn("No module found for: {}", moduleName);
        }
        return module;
    }

    @Override
    public List<String> getAvailableModules() {
        ensureScanned();
        return new ArrayList<>(factoryCache.keySet());
    }

    @Override
    public List<EngineModule> resolveAllModules() {
        ensureScanned();
        return new ArrayList<>(moduleCache.values());
    }

    /**
     * Clear all caches and reset the scanned state.
     * Useful for hot-reloading modules.
     */
    public void reset() {
        factoryCache.clear();
        moduleCache.clear();
        moduleContextCache.clear();
        permissionRegistry.clear();
        scanned = false;
        log.info("Module resolver reset");
    }

    /**
     * Get the module-scoped context for a specific module.
     *
     * @param moduleName the name of the module
     * @return the module's scoped context, or null if not found
     */
    public ModuleScopedContext getModuleContext(String moduleName) {
        ensureScanned();
        return moduleContextCache.get(moduleName);
    }

    /**
     * Get the shared permission registry.
     *
     * @return the shared permission registry
     */
    public PermissionRegistry getPermissionRegistry() {
        return permissionRegistry;
    }

    /**
     * Get the configured JAR directory path.
     *
     * @return the JAR directory path
     */
    public Path getScanDirectory() {
        return scanDirectory;
    }

    /**
     * Check if a module with the given name is available.
     *
     * @param moduleName the module name to check
     * @return true if the module is available
     */
    public boolean hasModule(String moduleName) {
        ensureScanned();
        return factoryCache.containsKey(moduleName);
    }

    /**
     * Uninstall a module by name.
     *
     * <p>This removes the module from the cache. Note that this does not
     * delete the JAR file from disk - it will be reloaded on next restart
     * unless the file is manually deleted.
     *
     * @param moduleName the name of the module to uninstall
     * @return true if the module was found and uninstalled
     */
    @Override
    public boolean uninstallModule(String moduleName) {
        ensureScanned();

        ModuleFactory removed = factoryCache.remove(moduleName);
        moduleCache.remove(moduleName);

        if (removed != null) {
            log.info("Uninstalled module: {}", moduleName);
            return true;
        }

        log.warn("Module not found for uninstall: {}", moduleName);
        return false;
    }

    @Override
    public ModuleFactory getFactory(String moduleName) {
        ensureScanned();
        return factoryCache.get(moduleName);
    }

    @Override
    public void registerCompoundModule(CompoundModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Compound module cannot be null");
        }

        String moduleName = module.getName();

        // Verify all component modules are available
        for (var componentId : module.getComponentModules()) {
            if (!hasModule(componentId.name())) {
                throw new IllegalStateException(
                        "Component module not found: " + componentId.name() +
                                " (required by compound module " + moduleName + ")");
            }
            EngineModule component = resolveModule(componentId.name());
            if (!component.getVersion().isCompatibleWith(componentId.version())) {
                throw new IllegalStateException(
                        "Version mismatch for component " + componentId.name() +
                                ": required " + componentId.version() +
                                " but found " + component.getVersion());
            }
        }

        moduleCache.put(moduleName, module);
        log.info("Registered compound module: {} with {} components",
                moduleName, module.getComponentModules().size());
    }

    @Override
    public Optional<EngineModule> getModule(ModuleIdentifier identifier) {
        ensureScanned();
        return resolveModule(identifier);
    }
}
