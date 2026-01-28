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


package ca.samanthaireland.lightning.engine.quarkus.api.startup;

import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotRestoreService;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotRestoreService.RestoreResult;
import ca.samanthaireland.lightning.engine.quarkus.api.config.RestoreConfig;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotPersistenceConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Startup hook for automatic match restoration.
 *
 * <p>When both snapshot persistence and auto-restore are enabled,
 * this hook will restore all matches from their latest snapshots
 * during server startup.
 */
@ApplicationScoped
public class MatchRestoreStartup {
    private static final Logger log = LoggerFactory.getLogger(MatchRestoreStartup.class);

    @Inject
    SnapshotRestoreService restoreService;

    @Inject
    RestoreConfig restoreConfig;

    @Inject
    SnapshotPersistenceConfig persistenceConfig;

    void onStart(@Observes StartupEvent ev) {
        if (!persistenceConfig.enabled()) {
            log.debug("Snapshot persistence is disabled, skipping auto-restore");
            return;
        }

        if (!restoreConfig.autoRestoreOnStartup()) {
            log.info("Auto-restore on startup is disabled");
            return;
        }

        log.info("Auto-restoring matches from persisted snapshots...");

        try {
            List<RestoreResult> results = restoreService.restoreAllMatches();

            long successCount = results.stream()
                .filter(RestoreResult::success)
                .count();
            long failedCount = results.stream()
                .filter(r -> !r.success())
                .count();

            if (successCount > 0 || failedCount > 0) {
                log.info("Auto-restore completed: {} matches restored, {} failed",
                    successCount, failedCount);

                // Log failures for debugging
                results.stream()
                    .filter(r -> !r.success())
                    .forEach(r -> log.warn("Failed to restore match {}: {}",
                        r.matchId(), r.message()));
            } else {
                log.info("No matches found to restore");
            }

        } catch (Exception e) {
            log.error("Error during auto-restore: {}", e.getMessage(), e);
        }
    }
}
