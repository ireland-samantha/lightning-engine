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


package ca.samanthaireland.lightning.engine.quarkus.api.persistence;

import ca.samanthaireland.lightning.engine.internal.TickListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-operation implementation of snapshot persistence.
 *
 * <p>This service is used when snapshot persistence is disabled.
 * It simply ignores all tick notifications without performing any action.
 */
public class NoOpSnapshotPersistenceService implements TickListener {
    private static final Logger log = LoggerFactory.getLogger(NoOpSnapshotPersistenceService.class);

    /**
     * Create a new no-op snapshot persistence service.
     */
    public NoOpSnapshotPersistenceService() {
        log.info("Snapshot persistence is disabled (NoOp mode)");
    }

    @Override
    public void onTickComplete(long tick) {
        // No-op: do nothing
    }
}
