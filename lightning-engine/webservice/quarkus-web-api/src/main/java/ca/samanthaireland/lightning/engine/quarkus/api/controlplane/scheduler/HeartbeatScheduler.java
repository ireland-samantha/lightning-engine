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

package ca.samanthaireland.lightning.engine.quarkus.api.controlplane.scheduler;

import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.config.ControlPlaneClientConfig;
import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.service.NodeRegistrationService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler that handles node registration on startup and periodic heartbeats.
 */
@ApplicationScoped
public class HeartbeatScheduler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatScheduler.class);

    private final NodeRegistrationService nodeRegistrationService;
    private final ControlPlaneClientConfig config;
    private ScheduledExecutorService scheduler;

    @Inject
    public HeartbeatScheduler(
            NodeRegistrationService nodeRegistrationService,
            ControlPlaneClientConfig config
    ) {
        this.nodeRegistrationService = nodeRegistrationService;
        this.config = config;
    }

    /**
     * Called on application startup. Registers with control plane and starts heartbeat scheduler.
     */
    void onStart(@Observes StartupEvent event) {
        if (!config.isEnabled()) {
            log.info("Control plane integration disabled (control-plane.url not set)");
            return;
        }

        log.info("Control plane integration enabled, registering with {}", config.url().get());

        // Register immediately
        nodeRegistrationService.register();

        // Start heartbeat scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "control-plane-heartbeat");
            t.setDaemon(true);
            return t;
        });

        int intervalSeconds = config.heartbeatIntervalSeconds();
        scheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("Heartbeat scheduler started with interval {}s", intervalSeconds);
    }

    /**
     * Called on application shutdown. Deregisters from control plane.
     */
    void onStop(@Observes ShutdownEvent event) {
        if (!config.isEnabled()) {
            return;
        }

        log.info("Shutting down, deregistering from control plane");

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        nodeRegistrationService.deregister();
    }

    private void sendHeartbeat() {
        try {
            nodeRegistrationService.heartbeat();
        } catch (Exception e) {
            log.error("Error sending heartbeat: {}", e.getMessage());
        }
    }
}
