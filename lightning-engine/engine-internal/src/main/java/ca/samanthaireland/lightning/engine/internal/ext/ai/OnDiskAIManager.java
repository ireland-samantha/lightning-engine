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


package ca.samanthaireland.lightning.engine.internal.ext.ai;

import ca.samanthaireland.lightning.engine.core.command.CommandExecutor;
import ca.samanthaireland.lightning.engine.core.resources.ResourceManager;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.game.domain.AI;
import ca.samanthaireland.game.backend.installation.AIFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages AI located in a server directory.
 *
 * <p>Provides per-match isolation by creating new AI instances for each match.
 */
@Slf4j
public class OnDiskAIManager implements AIManager {

    private final Path scanDirectory;
    private final AIFactoryFileLoader factoryFileLoader;
    private final ModuleContext moduleContext;
    private final CommandExecutor commandExecutor;
    private final ResourceManager resourceManager;

    private final Map<String, AIFactory> factoryCache = new ConcurrentHashMap<>();
    private volatile boolean scanned = false;

    /**
     * Create an AI manager with a custom directory.
     *
     * @param scanDirectory the directory to scan for JAR files
     * @param factoryFileLoader the loader for JAR files
     * @param moduleContext the module context for dependency injection
     * @param commandExecutor the command executor for AI commands
     * @param resourceManager the resource manager for resource lookup
     */
    public OnDiskAIManager(Path scanDirectory,
                           AIFactoryFileLoader factoryFileLoader,
                           ModuleContext moduleContext,
                           CommandExecutor commandExecutor,
                           ResourceManager resourceManager) {
        this.scanDirectory = scanDirectory;
        this.factoryFileLoader = factoryFileLoader;
        this.moduleContext = moduleContext;
        this.commandExecutor = commandExecutor;
        this.resourceManager = resourceManager;
    }

    @Override
    public void reloadInstalled() throws IOException {
        log.info("Scanning for AI in: {}", scanDirectory.toAbsolutePath());

        if (!Files.exists(scanDirectory)) {
            log.warn("AI directory does not exist: {}", scanDirectory);
            Files.createDirectories(scanDirectory);
            scanned = true;
            return;
        }

        if (!Files.isDirectory(scanDirectory)) {
            log.error("AI path is not a directory: {}", scanDirectory);
            scanned = true;
            return;
        }

        try (Stream<Path> paths = Files.list(scanDirectory)) {
            paths.filter(path -> path.toString().endsWith(".jar"))
                    .forEach(this::loadJarFile);
        }

        scanned = true;
        log.info("AI scan complete. Found {} AI: {}", factoryCache.size(), factoryCache.keySet());
    }

    private void loadJarFile(Path jarPath) {
        try {
            File jarFile = jarPath.toFile();
            List<AIFactory> factories = factoryFileLoader.loadAIFactories(jarFile);

            for (AIFactory factory : factories) {
                registerFactory(factory, jarFile.getName());
            }
        } catch (IOException e) {
            log.error("Failed to load JAR file: {}", jarPath, e);
        }
    }

    private void registerFactory(AIFactory factory, String source) {
        String name = factory.getName();

        if (factoryCache.containsKey(name)) {
            log.warn("Duplicate AI name '{}' found. Overwriting with AI from {}",
                    name, source);
        }

        factoryCache.put(name, factory);
        log.info("Registered AI: {} from {}", name, source);
    }

    @Override
    public void installAI(Class<? extends AIFactory> aiFactory) {
        try {
            AIFactory factory = aiFactory.getDeclaredConstructor().newInstance();
            registerFactory(factory, "class " + aiFactory.getName());
        } catch (Exception e) {
            log.error("Failed to instantiate AI factory: {}", aiFactory.getName(), e);
        }
    }

    @Override
    public void installAI(Path jarFile) throws IOException {
        if (!Files.exists(jarFile)) {
            throw new IOException("JAR file does not exist: " + jarFile);
        }

        if (!jarFile.toString().endsWith(".jar")) {
            throw new IOException("File is not a JAR file: " + jarFile);
        }

        if (!Files.exists(scanDirectory)) {
            Files.createDirectories(scanDirectory);
        }

        Path targetPath = scanDirectory.resolve(jarFile.getFileName());
        Files.copy(jarFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied JAR file to scan directory: {} -> {}", jarFile, targetPath);

        reset();
        reloadInstalled();
    }

    private void ensureScanned() {
        if (!scanned) {
            try {
                reloadInstalled();
            } catch (IOException e) {
                log.error("Failed to scan AI directory", e);
            }
        }
    }

    @Override
    public AIFactory getFactory(String aiName) {
        ensureScanned();
        return factoryCache.get(aiName);
    }

    @Override
    public AI createForMatch(String aiName, long matchId) {
        ensureScanned();

        AIFactory factory = factoryCache.get(aiName);
        if (factory == null) {
            log.warn("No AI found for: {}", aiName);
            return null;
        }

        DefaultAIContext context = new DefaultAIContext(matchId);
        context.addDependency(EntityComponentStore.class, moduleContext.getEntityComponentStore());
        context.setCommandExecutor(commandExecutor);
        context.setResourceManager(resourceManager);

        try {
            return factory.create(context);
        } catch (Exception e) {
            log.error("Failed to create AI '{}' for match {}: {}", aiName, matchId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<String> getAvailableAIs() {
        ensureScanned();
        return new ArrayList<>(factoryCache.keySet());
    }

    @Override
    public boolean hasAI(String aiName) {
        ensureScanned();
        return factoryCache.containsKey(aiName);
    }

    @Override
    public boolean uninstallAI(String aiName) {
        ensureScanned();

        AIFactory removed = factoryCache.remove(aiName);

        if (removed != null) {
            log.info("Uninstalled AI: {}", aiName);
            return true;
        }

        log.warn("AI not found for uninstall: {}", aiName);
        return false;
    }

    @Override
    public void reset() {
        factoryCache.clear();
        scanned = false;
        log.info("AI manager reset");
    }

    /**
     * Get the configured scan directory path.
     *
     * @return the scan directory path
     */
    public Path getScanDirectory() {
        return scanDirectory;
    }
}
