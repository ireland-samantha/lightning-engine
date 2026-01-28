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

package ca.samanthaireland.lightning.controlplane.provider.metrics;

import ca.samanthaireland.lightning.controlplane.cluster.model.ClusterStatus;
import ca.samanthaireland.lightning.controlplane.cluster.service.ClusterService;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.match.service.MatchRoutingService;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that registers cluster metrics with Micrometer for Prometheus scraping.
 */
@ApplicationScoped
@Startup
public class ClusterMetricsService {
    private static final Logger log = LoggerFactory.getLogger(ClusterMetricsService.class);

    private static final String METRIC_PREFIX = "lightning_cluster_";

    private final ClusterService clusterService;
    private final SchedulerService schedulerService;
    private final MatchRoutingService matchRoutingService;

    @Inject
    public ClusterMetricsService(
            MeterRegistry registry,
            ClusterService clusterService,
            SchedulerService schedulerService,
            MatchRoutingService matchRoutingService
    ) {
        this.clusterService = clusterService;
        this.schedulerService = schedulerService;
        this.matchRoutingService = matchRoutingService;

        registerMetrics(registry);
        log.info("Cluster metrics registered with Micrometer");
    }

    private void registerMetrics(MeterRegistry registry) {
        // Node metrics
        Gauge.builder(METRIC_PREFIX + "nodes_total", this, s -> s.getClusterStatus().totalNodes())
                .description("Total number of registered nodes")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "nodes_healthy", this, s -> s.getClusterStatus().healthyNodes())
                .description("Number of healthy nodes")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "nodes_draining", this, s -> s.getClusterStatus().drainingNodes())
                .description("Number of draining nodes")
                .register(registry);

        // Capacity metrics
        Gauge.builder(METRIC_PREFIX + "capacity_total", this, s -> s.getClusterStatus().totalCapacity())
                .description("Total container capacity across all nodes")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "capacity_available", this, s -> s.getClusterStatus().availableCapacity())
                .description("Available container capacity across healthy nodes")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "saturation", this, s -> s.schedulerService.getClusterSaturation())
                .description("Cluster saturation ratio (0.0 to 1.0)")
                .register(registry);

        // Container and match metrics
        Gauge.builder(METRIC_PREFIX + "containers_total", this, s -> s.getClusterStatus().totalContainers())
                .description("Total number of containers across all nodes")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "matches_total", this, s -> s.getClusterStatus().totalMatches())
                .description("Total number of matches across all nodes (from node metrics)")
                .register(registry);

        // Match registry metrics
        Gauge.builder(METRIC_PREFIX + "matches_registered", this, s -> s.matchRoutingService.findAll().size())
                .description("Total matches in the control plane registry")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "matches_running", this,
                        s -> s.matchRoutingService.findByStatus(MatchStatus.RUNNING).size())
                .description("Number of running matches")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "matches_finished", this,
                        s -> s.matchRoutingService.findByStatus(MatchStatus.FINISHED).size())
                .description("Number of finished matches")
                .register(registry);

        // Player count
        Gauge.builder(METRIC_PREFIX + "players_total", this, s -> s.getTotalPlayerCount())
                .description("Total number of players across all matches")
                .register(registry);
    }

    private ClusterStatus getClusterStatus() {
        return clusterService.getClusterStatus();
    }

    private int getTotalPlayerCount() {
        return matchRoutingService.findAll().stream()
                .mapToInt(m -> m.playerCount())
                .sum();
    }
}
