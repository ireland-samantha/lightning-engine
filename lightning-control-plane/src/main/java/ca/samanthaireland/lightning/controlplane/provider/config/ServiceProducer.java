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

package ca.samanthaireland.lightning.controlplane.provider.config;

import ca.samanthaireland.lightning.controlplane.autoscaler.service.AutoscalerService;
import ca.samanthaireland.lightning.controlplane.autoscaler.service.AutoscalerServiceImpl;
import ca.samanthaireland.lightning.controlplane.client.LightningNodeClient;
import ca.samanthaireland.lightning.controlplane.cluster.service.ClusterService;
import ca.samanthaireland.lightning.controlplane.cluster.service.ClusterServiceImpl;
import ca.samanthaireland.lightning.controlplane.config.AutoscalerConfiguration;
import ca.samanthaireland.lightning.controlplane.config.ControlPlaneConfiguration;
import ca.samanthaireland.lightning.controlplane.config.ModuleStorageConfiguration;
import ca.samanthaireland.lightning.controlplane.match.repository.MatchRegistry;
import ca.samanthaireland.lightning.controlplane.match.service.MatchRoutingService;
import ca.samanthaireland.lightning.controlplane.match.service.MatchRoutingServiceImpl;
import ca.samanthaireland.lightning.controlplane.module.repository.ModuleRepository;
import ca.samanthaireland.lightning.controlplane.module.service.ModuleRegistryService;
import ca.samanthaireland.lightning.controlplane.module.service.ModuleRegistryServiceImpl;
import ca.samanthaireland.lightning.controlplane.node.repository.NodeRepository;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryServiceImpl;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus CDI producer for core domain services.
 *
 * <p>This class bridges the framework-agnostic core domain services with
 * Quarkus CDI by producing singleton instances that can be injected into
 * REST resources and other framework components.
 *
 * <p>The producer pattern allows the core services to remain free of
 * framework annotations while still participating in CDI dependency injection.
 */
@ApplicationScoped
public class ServiceProducer {

    /**
     * Produces the core ControlPlaneConfiguration from Quarkus config.
     *
     * @param quarkusConfig the Quarkus configuration mapping
     * @return the core configuration interface
     */
    @Produces
    @Singleton
    public ControlPlaneConfiguration controlPlaneConfiguration(QuarkusControlPlaneConfig quarkusConfig) {
        return quarkusConfig;
    }

    /**
     * Produces the core AutoscalerConfiguration from Quarkus config.
     *
     * @param quarkusConfig the Quarkus configuration mapping
     * @return the core configuration interface
     */
    @Produces
    @Singleton
    public AutoscalerConfiguration autoscalerConfiguration(QuarkusAutoscalerConfig quarkusConfig) {
        return quarkusConfig;
    }

    /**
     * Produces the core ModuleStorageConfiguration from Quarkus config.
     *
     * @param quarkusConfig the Quarkus configuration mapping
     * @return the core configuration interface
     */
    @Produces
    @Singleton
    public ModuleStorageConfiguration moduleStorageConfiguration(QuarkusModuleStorageConfig quarkusConfig) {
        return quarkusConfig;
    }

    /**
     * Produces the NodeRegistryService.
     *
     * @param nodeRepository the node repository
     * @param config         the control plane configuration
     * @return the node registry service
     */
    @Produces
    @Singleton
    public NodeRegistryService nodeRegistryService(
            NodeRepository nodeRepository,
            ControlPlaneConfiguration config
    ) {
        return new NodeRegistryServiceImpl(nodeRepository, config);
    }

    /**
     * Produces the SchedulerService.
     *
     * @param nodeRegistryService the node registry service
     * @return the scheduler service
     */
    @Produces
    @Singleton
    public SchedulerService schedulerService(NodeRegistryService nodeRegistryService) {
        return new SchedulerServiceImpl(nodeRegistryService);
    }

    /**
     * Produces the ClusterService.
     *
     * @param nodeRegistryService the node registry service
     * @return the cluster service
     */
    @Produces
    @Singleton
    public ClusterService clusterService(NodeRegistryService nodeRegistryService) {
        return new ClusterServiceImpl(nodeRegistryService);
    }

    /**
     * Produces the MatchRoutingService.
     *
     * @param schedulerService the scheduler service
     * @param nodeClient       the lightning node client
     * @param matchRegistry    the match registry
     * @return the match routing service
     */
    @Produces
    @Singleton
    public MatchRoutingService matchRoutingService(
            SchedulerService schedulerService,
            LightningNodeClient nodeClient,
            MatchRegistry matchRegistry
    ) {
        return new MatchRoutingServiceImpl(schedulerService, nodeClient, matchRegistry);
    }

    /**
     * Produces the AutoscalerService.
     *
     * @param nodeRegistryService the node registry service
     * @param schedulerService    the scheduler service
     * @param config              the autoscaler configuration
     * @return the autoscaler service
     */
    @Produces
    @Singleton
    public AutoscalerService autoscalerService(
            NodeRegistryService nodeRegistryService,
            SchedulerService schedulerService,
            AutoscalerConfiguration config
    ) {
        return new AutoscalerServiceImpl(nodeRegistryService, schedulerService, config);
    }

    /**
     * Produces the ModuleRegistryService.
     *
     * @param moduleRepository    the module repository
     * @param nodeRegistryService the node registry service
     * @param config              the module storage configuration
     * @return the module registry service
     */
    @Produces
    @Singleton
    public ModuleRegistryService moduleRegistryService(
            ModuleRepository moduleRepository,
            NodeRegistryService nodeRegistryService,
            ModuleStorageConfiguration config
    ) {
        return new ModuleRegistryServiceImpl(moduleRepository, nodeRegistryService, config);
    }
}
