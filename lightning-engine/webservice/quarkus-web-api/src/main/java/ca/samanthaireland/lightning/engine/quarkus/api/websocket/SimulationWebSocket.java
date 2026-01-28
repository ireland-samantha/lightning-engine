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


package ca.samanthaireland.lightning.engine.quarkus.api.websocket;

import ca.samanthaireland.lightning.engine.quarkus.api.dto.TickResponse;
import ca.samanthaireland.lightning.engine.core.GameSimulation;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import jakarta.inject.Inject;

/**
 * WebSocket endpoint for simulation tick operations.
 *
 * <p>Clients connect to /ws/simulation and can:
 * - Send "tick" to advance the simulation
 * - Send "status" to get the current tick
 */
@WebSocket(path = "/ws/simulation")
public class SimulationWebSocket {

    @Inject
    GameSimulation gameSimulation;

    @OnOpen
    public TickResponse onOpen() {
        return new TickResponse(gameSimulation.getCurrentTick());
    }

    @OnTextMessage
    public TickResponse onMessage(String message) {
        return switch (message.trim().toLowerCase()) {
            case "tick" -> new TickResponse(gameSimulation.advanceTick());
            case "status" -> new TickResponse(gameSimulation.getCurrentTick());
            default -> new TickResponse(gameSimulation.getCurrentTick());
        };
    }
}
