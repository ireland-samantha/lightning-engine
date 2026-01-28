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


package ca.samanthaireland.lightning.engine.quarkus.api.config;

import ca.samanthaireland.auth.AuthBootstrap;
import ca.samanthaireland.auth.AuthService;
import ca.samanthaireland.auth.InMemoryRoleRepository;
import ca.samanthaireland.auth.InMemoryUserRepository;
import ca.samanthaireland.auth.PasswordService;
import ca.samanthaireland.auth.RoleRepository;
import ca.samanthaireland.auth.RoleService;
import ca.samanthaireland.auth.UserRepository;
import ca.samanthaireland.auth.UserService;
import ca.samanthaireland.lightning.engine.core.GameSimulation;
import ca.samanthaireland.lightning.engine.core.command.CommandExecutor;
import ca.samanthaireland.lightning.engine.core.command.CommandQueue;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.match.PlayerService;
import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionRepository;
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionService;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotRestoreService;
import ca.samanthaireland.lightning.engine.core.store.ComponentRegistry;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.core.store.PermissionRegistry;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.internal.GameLoop;
import ca.samanthaireland.lightning.engine.internal.TickListener;
import ca.samanthaireland.lightning.engine.internal.core.command.CommandQueueExecutor;
import ca.samanthaireland.lightning.engine.internal.core.command.CommandResolver;
import ca.samanthaireland.lightning.engine.internal.core.command.InMemoryCommandQueueManager;
import ca.samanthaireland.lightning.engine.internal.core.command.ModuleCommandResolver;
import ca.samanthaireland.lightning.engine.internal.core.command.CommandExecutorFromResolver;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryGameSimulation;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryMatchRepository;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryMatchService;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryPlayerRepository;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryPlayerService;
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaCompressionService;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotHistory;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.DeltaCompressionServiceImpl;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.InMemorySnapshotHistory;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotProvider;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotProviderImpl;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import ca.samanthaireland.lightning.engine.internal.core.store.LockingEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.SimplePermissionRegistry;
import ca.samanthaireland.lightning.engine.internal.core.resource.OnDiskResourceManager;
import ca.samanthaireland.lightning.engine.internal.ext.ai.AIFactoryFileLoader;
import ca.samanthaireland.lightning.engine.internal.ext.ai.AIManager;
import ca.samanthaireland.lightning.engine.internal.ext.ai.AITickService;
import ca.samanthaireland.lightning.engine.internal.ext.ai.OnDiskAIManager;
import ca.samanthaireland.lightning.engine.internal.ext.jar.ModuleFactoryClassLoader;
import ca.samanthaireland.lightning.engine.internal.ext.module.DefaultInjector;
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;
import ca.samanthaireland.lightning.engine.internal.ext.module.OnDiskModuleManager;
import ca.samanthaireland.lightning.engine.internal.container.InMemoryContainerManager;
import ca.samanthaireland.lightning.engine.internal.core.session.DefaultPlayerSessionService;
import ca.samanthaireland.lightning.engine.internal.core.session.InMemoryPlayerSessionRepository;
import ca.samanthaireland.lightning.engine.core.error.ErrorBroadcaster;
import ca.samanthaireland.lightning.engine.internal.core.error.InMemoryErrorBroadcaster;
import ca.samanthaireland.lightning.engine.internal.core.store.SimpleComponentRegistry;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.DefaultSnapshotRestoreService;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.MongoSnapshotHistoryRepository;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.MongoSnapshotPersistenceService;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.NoOpSnapshotPersistenceService;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotHistoryRepository;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotHistoryService;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotPersistenceConfig;
import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;

/**
 * CDI configuration for simulation beans.
 */
@ApplicationScoped
public class SimulationConfig {

    @ConfigProperty(name = "ecs.max-entities", defaultValue = "1000000")
    int maxEntities;

    @ConfigProperty(name = "ecs.max-components", defaultValue = "100")
    int maxComponents;

    @ConfigProperty(name = "gameloop.max-commands-per-tick", defaultValue = "10000")
    int maxCommandsPerTick;

    @ConfigProperty(name = "storage.modules-path", defaultValue = "modules")
    String modulesPath;

    @ConfigProperty(name = "storage.ai-path", defaultValue = "ai")
    String aiPath;

    @ConfigProperty(name = "storage.resources-path", defaultValue = "resources")
    String resourcesPath;

    @ConfigProperty(name = "auth.jwt.secret", defaultValue = "")
    String jwtSecret;

    @ConfigProperty(name = "admin.initial.password", defaultValue = "")
    String adminInitialPassword;

    // ---------- Core infrastructure ----------

    @Produces
    @ApplicationScoped
    public ModuleContext moduleDependencyInjector() {
        return new DefaultInjector();
    }

    @Produces
    @ApplicationScoped
    public EntityComponentStore entityComponentStore(ModuleContext context) {
        ArrayEntityComponentStore store =
                new ArrayEntityComponentStore(new EcsProperties(maxEntities, maxComponents));
        EntityComponentStore wrapped = LockingEntityComponentStore.wrap(store);
        // Register with the injector so modules can access it
        context.addClass(EntityComponentStore.class, wrapped);
        return wrapped;
    }

    @Produces
    @ApplicationScoped
    public PermissionRegistry permissionRegistry() {
        return new SimplePermissionRegistry();
    }

    // ---------- Container management ----------

    @Produces
    @ApplicationScoped
    public ContainerManager containerManager() {
        // Creates manager with default container initialized automatically
        return new InMemoryContainerManager(
                modulesPath, maxEntities, maxComponents, maxCommandsPerTick);
    }

    @Produces
    @ApplicationScoped
    public ModuleManager moduleManager(ModuleContext context, PermissionRegistry permissionRegistry,
                                        EntityComponentStore store) {
        // Pass store directly to OnDiskModuleManager to ensure it's available during module initialization
        OnDiskModuleManager manager =
                new OnDiskModuleManager(Path.of(modulesPath),
                        new ModuleFactoryClassLoader<>(ModuleFactory.class, "ModuleFactory"),
                        context,
                        permissionRegistry,
                        store);
        // Register with the injector so modules can access it via ModuleResolver
        context.addClass(ModuleResolver.class, manager);
        return manager;
    }

    @Produces
    @ApplicationScoped
    public AIManager aiManager(ModuleContext context,
                               CommandExecutor commandExecutor,
                               OnDiskResourceManager resourceManager) {
        OnDiskAIManager manager =
                new OnDiskAIManager(Path.of(aiPath),
                        new AIFactoryFileLoader(),
                        context,
                        commandExecutor,
                        resourceManager);
        return manager;
    }

    @Produces
    @ApplicationScoped
    public AITickService aiTickService(AIManager aiManager,
                                        MatchService matchService) {
        return new AITickService(aiManager, matchService);
    }

    // ---------- Command queue ----------

    /**
     * Produce the concrete implementation ONCE.
     * CDI will expose it as all implemented interfaces.
     */
    @Produces
    @ApplicationScoped
    public InMemoryCommandQueueManager commandQueueManager() {
        return new InMemoryCommandQueueManager();
    }

    // ---------- Resolvers ----------

    @Produces
    @ApplicationScoped
    public CommandResolver commandResolver(ModuleManager moduleManager) {
        return new ModuleCommandResolver(moduleManager);
    }

    @Produces
    @ApplicationScoped
    public CommandExecutor commandExecutor(CommandResolver commandResolver) {
        return new CommandExecutorFromResolver(commandResolver);
    }

    // ---------- Snapshot ----------

    @Produces
    @ApplicationScoped
    public SnapshotProvider snapshotProvider(EntityComponentStore store,
                                             ModuleManager moduleManager) {
        return new SnapshotProviderImpl(store, moduleManager);
    }

    @Produces
    @ApplicationScoped
    public SnapshotHistory snapshotHistory() {
        return new InMemorySnapshotHistory();
    }

    @Produces
    @ApplicationScoped
    public DeltaCompressionService deltaCompressionService() {
        return new DeltaCompressionServiceImpl();
    }

    // ---------- Error broadcasting ----------

    @Produces
    @ApplicationScoped
    public ErrorBroadcaster errorBroadcaster() {
        return new InMemoryErrorBroadcaster();
    }

    // ---------- Match / Player services ----------

    @Produces
    @ApplicationScoped
    public MatchService matchService(ModuleResolver resolver, ModuleContext context) {
        InMemoryMatchService service =
                new InMemoryMatchService(new InMemoryMatchRepository(), resolver);
        // Register with the injector so modules can access it
        context.addClass(MatchService.class, service);
        return service;
    }

    @Produces
    @ApplicationScoped
    public PlayerService playerService() {
        return new InMemoryPlayerService(new InMemoryPlayerRepository());
    }

    // ---------- Session management ----------

    @Produces
    @ApplicationScoped
    public PlayerSessionRepository playerSessionRepository() {
        return new InMemoryPlayerSessionRepository();
    }

    @Produces
    @ApplicationScoped
    public PlayerSessionService playerSessionService(PlayerSessionRepository sessionRepository,
                                                      PlayerService playerService,
                                                      MatchService matchService) {
        return new DefaultPlayerSessionService(sessionRepository, playerService, matchService);
    }

    // ---------- Component registry ----------

    @Produces
    @ApplicationScoped
    public ComponentRegistry componentRegistry(ModuleManager moduleManager) {
        SimpleComponentRegistry registry = new SimpleComponentRegistry();
        // Register components from all loaded modules
        moduleManager.resolveAllModules().forEach(module ->
                module.createComponents().forEach(registry::register));
        return registry;
    }

    // ---------- Snapshot restore ----------

    @Produces
    @ApplicationScoped
    public SnapshotRestoreService snapshotRestoreService(SnapshotHistoryRepository historyRepository,
                                                          EntityComponentStore store,
                                                          MatchService matchService,
                                                          ComponentRegistry componentRegistry) {
        return new DefaultSnapshotRestoreService(
                historyRepository, store, matchService, componentRegistry);
    }

    // ---------- Authentication ----------

    @Produces
    @ApplicationScoped
    public RoleRepository roleRepository() {
        return new InMemoryRoleRepository();
    }

    @Produces
    @ApplicationScoped
    public UserRepository userRepository() {
        return new InMemoryUserRepository();
    }

    @Produces
    @ApplicationScoped
    public PasswordService passwordService() {
        return new PasswordService();
    }

    @Produces
    @ApplicationScoped
    public RoleService roleService(RoleRepository roleRepository) {
        return new RoleService(roleRepository);
    }

    @Produces
    @ApplicationScoped
    public UserService userService(UserRepository userRepository,
                                    PasswordService passwordService,
                                    RoleRepository roleRepository) {
        return new UserService(userRepository, passwordService, roleRepository);
    }

    @Produces
    @ApplicationScoped
    public AuthService authService(UserRepository userRepository,
                                    PasswordService passwordService,
                                    RoleService roleService) {
        // Use SmallRye JWT for RSA-based token signing (compatible with SmallRye JWT verification)
        var tokenIssuer = new ca.samanthaireland.lightning.engine.quarkus.api.auth.SmallRyeJwtTokenIssuer(24);
        // The secret is not used for signing when using RSA, but required by AuthService
        // for its internal JWT verification. Generate a secure random secret if not configured.
        String effectiveSecret = jwtSecret;
        if (effectiveSecret == null || effectiveSecret.isBlank()) {
            byte[] randomBytes = new byte[32];
            new java.security.SecureRandom().nextBytes(randomBytes);
            effectiveSecret = java.util.Base64.getEncoder().encodeToString(randomBytes);
        }
        return new AuthService(userRepository, passwordService, roleService, effectiveSecret, 24, tokenIssuer);
    }

    @Produces
    @ApplicationScoped
    public AuthBootstrap authBootstrap(UserService userService,
                                        RoleService roleService,
                                        PasswordService passwordService,
                                        UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        AuthService authService) {
        // Set system property from Quarkus config so AuthBootstrap.resolveAdminPassword() can find it
        if (adminInitialPassword != null && !adminInitialPassword.isBlank()) {
            System.setProperty("admin.initial.password", adminInitialPassword);
        }
        AuthBootstrap bootstrap = new AuthBootstrap(
                userService, roleService, passwordService,
                userRepository, roleRepository, authService);
        bootstrap.initializeDefaults();
        return bootstrap;
    }

    // ---------- Resource management ----------

    @Produces
    @ApplicationScoped
    public OnDiskResourceManager resourceManager() {
        return new OnDiskResourceManager(Path.of(resourcesPath));
    }

    // ---------- Snapshot persistence ----------

    @Produces
    @ApplicationScoped
    public TickListener snapshotPersistenceService(SnapshotPersistenceConfig config,
                                                    SnapshotProvider snapshotProvider,
                                                    MatchService matchService,
                                                    MongoClient mongoClient) {
        if (config.enabled()) {
            return new MongoSnapshotPersistenceService(mongoClient, snapshotProvider, matchService, config);
        } else {
            return new NoOpSnapshotPersistenceService();
        }
    }

    @Produces
    @ApplicationScoped
    public SnapshotHistoryRepository snapshotHistoryRepository(SnapshotPersistenceConfig config,
                                                                MongoClient mongoClient) {
        return new MongoSnapshotHistoryRepository(mongoClient, config.database(), config.collection());
    }

    @Produces
    @ApplicationScoped
    public SnapshotHistoryService snapshotHistoryService(SnapshotHistoryRepository repository) {
        return new SnapshotHistoryService(repository);
    }

    // ---------- Game loop & simulation ----------

    @Produces
    @ApplicationScoped
    public GameLoop gameLoop(ModuleResolver resolver,
                             CommandQueueExecutor executor,
                             AITickService aiTickService,
                             TickListener snapshotPersistenceListener,
                             CommandQueue commandQueue,
                             ErrorBroadcaster errorBroadcaster) {
        GameLoop gameLoop = new GameLoop(resolver, executor, aiTickService, maxCommandsPerTick);
        // Register the snapshot persistence listener
        gameLoop.addTickListener(snapshotPersistenceListener);
        // Register the error collector listener
        gameLoop.addTickListener(new ca.samanthaireland.lightning.engine.quarkus.api.error.ErrorCollectorTickListener(
                commandQueue, errorBroadcaster));
        return gameLoop;
    }

    @Produces
    @ApplicationScoped
    public GameSimulation gameSimulation(
            MatchService matchService,
            PlayerService playerService,
            PlayerSessionService sessionService,
            ModuleManager moduleManager,
            CommandResolver commandResolver,
            CommandQueue commandQueue,
            SnapshotProvider snapshotProvider,
            GameLoop gameLoop) {

        return new InMemoryGameSimulation(
                matchService,
                playerService,
                sessionService,
                moduleManager,
                commandResolver,
                commandQueue,
                snapshotProvider,
                gameLoop);
    }
}
