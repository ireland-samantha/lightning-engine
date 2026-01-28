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


package ca.samanthaireland.lightning.engine.quarkus.api;

import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.ai.TickCounterAIFactory;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.ext.modules.*;
import ca.samanthaireland.lightning.engine.internal.ext.ai.AIManager;
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;

@ApplicationScoped
@Startup
public class Bootstrap {
    @Inject
    EntityComponentStore entityComponentStore;

    @Inject
    ModuleContext moduleDependencyInjector;

    @Inject
    ModuleManager moduleManager;

    @Inject
    AIManager aiManager;

    @Inject
    ModuleResolver moduleResolver;

    @Inject
    MatchService matchService;

    void onStart(@Observes StartupEvent event) {
        // Install core engine modules
        moduleManager.installModule(EntityModuleFactory.class);
        moduleManager.installModule(GridMapModuleFactory.class);
        moduleManager.installModule(RigidBodyModuleFactory.class);
//        moduleManager.installModule(BoxColliderModuleFactory.class);
        moduleManager.installModule(RenderingModuleFactory.class);

//        moduleManager.installModule(HealthModuleFactory.class);
//        moduleManager.installModule(ProjectileModuleFactory.class);
//        moduleManager.installModule(ItemsModuleFactory.class);

        // Install sample AI
        aiManager.installAI(TickCounterAIFactory.class);

        try {
            moduleManager.reloadInstalled();
            aiManager.reloadInstalled();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        moduleDependencyInjector.addClass(MatchService.class, matchService);
        moduleDependencyInjector.addClass(ModuleManager.class, moduleManager);
        moduleDependencyInjector.addClass(ModuleResolver.class, moduleResolver);
        moduleDependencyInjector.addClass(EntityComponentStore.class, entityComponentStore);
    }
}
