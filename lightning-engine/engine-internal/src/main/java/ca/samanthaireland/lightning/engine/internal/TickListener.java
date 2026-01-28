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


package ca.samanthaireland.lightning.engine.internal;

/**
 * Listener interface for receiving tick completion notifications from the game loop.
 *
 * <p>Implementations can use this to perform actions after each simulation tick,
 * such as persisting snapshots, sending updates to clients, or logging metrics.
 *
 * <p>Listeners are notified after all systems and game masters have executed for a tick.
 * Implementations should be mindful of performance as they run on the game loop thread.
 */
@FunctionalInterface
public interface TickListener {

    /**
     * Called after a tick has completed.
     *
     * <p>This method is invoked after:
     * <ol>
     *   <li>Commands have been executed</li>
     *   <li>Systems have been run</li>
     *   <li>Game masters have been executed</li>
     * </ol>
     *
     * @param tick the tick number that just completed
     */
    void onTickComplete(long tick);
}
