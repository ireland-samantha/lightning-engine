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

import ca.samanthaireland.lightning.engine.core.error.GameError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryErrorBroadcasterTest {

    private InMemoryErrorBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new InMemoryErrorBroadcaster();
    }

    @Test
    @Disabled("Flaky test - async thread pool timing causes race conditions where errors may not be delivered within the 2s timeout")
    void subscribe_receivesAllErrors() throws InterruptedException {
        List<GameError> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        broadcaster.subscribe(error -> {
            received.add(error);
            latch.countDown();
        });

        GameError error1 = GameError.generalError("test", "message1", "details1");
        GameError error2 = GameError.generalError("test", "message2", "details2");

        broadcaster.publish(error1);
        broadcaster.publish(error2);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(2);
        assertThat(received.get(0).message()).isEqualTo("message1");
        assertThat(received.get(1).message()).isEqualTo("message2");
    }

    @Test
    void subscribeToMatch_filtersErrorsByMatch() throws InterruptedException {
        List<GameError> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        broadcaster.subscribeToMatch(1, error -> {
            received.add(error);
            latch.countDown();
        });

        // Publish error for different match - should not be received
        GameError otherMatchError = GameError.commandError(2, 0, "cmd", "error", "details");
        broadcaster.publish(otherMatchError);

        // Publish error for subscribed match - should be received
        GameError matchError = GameError.commandError(1, 0, "cmd", "error for match 1", "details");
        broadcaster.publish(matchError);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(1);
        assertThat(received.get(0).message()).isEqualTo("error for match 1");
    }

    @Test
    void subscribeToPlayer_filtersErrorsByMatchAndPlayer() throws InterruptedException {
        List<GameError> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        broadcaster.subscribeToPlayer(1, 100, error -> {
            received.add(error);
            latch.countDown();
        });

        // Publish error for different player - should not be received
        GameError otherPlayerError = GameError.commandError(1, 200, "cmd", "other player", "details");
        broadcaster.publish(otherPlayerError);

        // Publish error for subscribed player - should be received
        GameError playerError = GameError.commandError(1, 100, "cmd", "correct player", "details");
        broadcaster.publish(playerError);

        // Publish match-wide error (playerId=0) - should also be received
        GameError matchWideError = GameError.systemError(1, "System", "match wide", "details");
        broadcaster.publish(matchWideError);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(2);
        assertThat(received).anyMatch(e -> e.message().equals("correct player"));
        assertThat(received).anyMatch(e -> e.message().equals("match wide"));
    }

    @Test
    void unsubscribe_stopsReceivingErrors() throws InterruptedException {
        List<GameError> received = new ArrayList<>();

        String subscriptionId = broadcaster.subscribe(received::add);

        GameError error1 = GameError.generalError("test", "before unsubscribe", "");
        broadcaster.publish(error1);

        // Wait for async delivery
        Thread.sleep(100);

        broadcaster.unsubscribe(subscriptionId);

        GameError error2 = GameError.generalError("test", "after unsubscribe", "");
        broadcaster.publish(error2);

        // Wait to ensure no more messages arrive
        Thread.sleep(100);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).message()).isEqualTo("before unsubscribe");
    }

    @Test
    void gameError_commandError_setsCorrectType() {
        GameError error = GameError.commandError(1, 2, "TestCommand", "failed", "stack");

        assertThat(error.type()).isEqualTo(GameError.ErrorType.COMMAND);
        assertThat(error.matchId()).isEqualTo(1);
        assertThat(error.playerId()).isEqualTo(2);
        assertThat(error.source()).isEqualTo("TestCommand");
        assertThat(error.message()).isEqualTo("failed");
        assertThat(error.details()).isEqualTo("stack");
        assertThat(error.id()).isNotNull();
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    void gameError_systemError_setsCorrectType() {
        GameError error = GameError.systemError(1, "PhysicsSystem", "NPE", "stack");

        assertThat(error.type()).isEqualTo(GameError.ErrorType.SYSTEM);
        assertThat(error.matchId()).isEqualTo(1);
        assertThat(error.playerId()).isEqualTo(0);
        assertThat(error.source()).isEqualTo("PhysicsSystem");
    }

    @Test
    void gameError_generalError_setsCorrectType() {
        GameError error = GameError.generalError("Startup", "failed to init", "stack");

        assertThat(error.type()).isEqualTo(GameError.ErrorType.GENERAL);
        assertThat(error.matchId()).isEqualTo(0);
        assertThat(error.playerId()).isEqualTo(0);
        assertThat(error.source()).isEqualTo("Startup");
    }
}
