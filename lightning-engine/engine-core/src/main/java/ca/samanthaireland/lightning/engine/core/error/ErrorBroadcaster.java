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


package ca.samanthaireland.lightning.engine.core.error;

import java.util.function.Consumer;

/**
 * Service for broadcasting game errors to interested listeners.
 *
 * <p>Components that generate errors (command executors, systems, etc.) publish
 * errors to this service, and interested parties (WebSocket endpoints, etc.)
 * subscribe to receive them.
 */
public interface ErrorBroadcaster {

    /**
     * Publish an error to all subscribers.
     *
     * @param error the error to publish
     */
    void publish(GameError error);

    /**
     * Subscribe to receive errors.
     *
     * @param listener the listener to receive errors
     * @return a subscription ID for unsubscribing
     */
    String subscribe(Consumer<GameError> listener);

    /**
     * Subscribe to receive errors for a specific match.
     *
     * @param matchId the match ID to filter by
     * @param listener the listener to receive errors
     * @return a subscription ID for unsubscribing
     */
    String subscribeToMatch(long matchId, Consumer<GameError> listener);

    /**
     * Subscribe to receive errors for a specific player in a match.
     *
     * @param matchId the match ID to filter by
     * @param playerId the player ID to filter by
     * @param listener the listener to receive errors
     * @return a subscription ID for unsubscribing
     */
    String subscribeToPlayer(long matchId, long playerId, Consumer<GameError> listener);

    /**
     * Unsubscribe from receiving errors.
     *
     * @param subscriptionId the subscription ID returned from subscribe
     */
    void unsubscribe(String subscriptionId);
}
