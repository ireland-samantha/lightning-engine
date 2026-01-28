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


package ca.samanthaireland.lightning.engine.internal.core.error;

import ca.samanthaireland.lightning.engine.core.error.ErrorBroadcaster;
import ca.samanthaireland.lightning.engine.core.error.GameError;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * In-memory implementation of {@link ErrorBroadcaster}.
 *
 * <p>Uses a thread pool to dispatch errors asynchronously to avoid blocking
 * the error publisher.
 */
@Slf4j
public class InMemoryErrorBroadcaster implements ErrorBroadcaster {

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ErrorBroadcaster-Worker");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void publish(GameError error) {
        log.debug("Publishing error: type={}, source={}, message={}",
                error.type(), error.source(), error.message());

        for (Subscription subscription : subscriptions.values()) {
            if (subscription.matches(error)) {
                executor.submit(() -> {
                    try {
                        subscription.listener().accept(error);
                    } catch (Exception e) {
                        log.error("Error dispatching to subscriber {}: {}",
                                subscription.id(), e.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public String subscribe(Consumer<GameError> listener) {
        String id = generateId();
        subscriptions.put(id, new Subscription(id, null, null, listener));
        log.debug("Subscription {} added (all errors)", id);
        return id;
    }

    @Override
    public String subscribeToMatch(long matchId, Consumer<GameError> listener) {
        String id = generateId();
        subscriptions.put(id, new Subscription(id, matchId, null, listener));
        log.debug("Subscription {} added for match {}", id, matchId);
        return id;
    }

    @Override
    public String subscribeToPlayer(long matchId, long playerId, Consumer<GameError> listener) {
        String id = generateId();
        subscriptions.put(id, new Subscription(id, matchId, playerId, listener));
        log.debug("Subscription {} added for match {} player {}", id, matchId, playerId);
        return id;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        Subscription removed = subscriptions.remove(subscriptionId);
        if (removed != null) {
            log.debug("Subscription {} removed", subscriptionId);
        }
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Shutdown the executor.
     */
    public void shutdown() {
        executor.shutdown();
        log.info("ErrorBroadcaster executor shutdown initiated");
    }

    /**
     * A subscription with optional match/player filtering.
     */
    private record Subscription(
            String id,
            Long matchId,
            Long playerId,
            Consumer<GameError> listener
    ) {
        boolean matches(GameError error) {
            // If no filter, match everything
            if (matchId == null) {
                return true;
            }

            // Match by matchId
            if (error.matchId() != 0 && error.matchId() != matchId) {
                return false;
            }

            // If no player filter, match all for this match
            if (playerId == null) {
                return true;
            }

            // Match by playerId (or include if error is match-wide)
            return error.playerId() == 0 || error.playerId() == playerId;
        }
    }
}
