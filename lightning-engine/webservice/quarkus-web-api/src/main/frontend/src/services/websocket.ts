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

/**
 * WebSocket client for real-time snapshot streaming.
 */

/**
 * Component data within a snapshot module.
 */
export interface ComponentDataResponse {
  name: string;
  values: number[];
}

/**
 * Module data within a snapshot, including version information.
 */
export interface ModuleDataResponse {
  name: string;
  version: string;
  components: ComponentDataResponse[];
}

/**
 * Snapshot data with module-based structure.
 */
export interface SnapshotData {
  matchId: number;
  tick: number;
  modules: ModuleDataResponse[];
  error?: string;
}

export type SnapshotListener = (snapshot: SnapshotData) => void;

export class WebSocketClient {
  private ws: WebSocket | null = null;
  private serverUrl: string;
  private containerId: number;
  private matchId: number;
  private listeners: Set<SnapshotListener> = new Set();
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 5;
  private reconnectDelay: number = 1000;
  private reconnectTimer: number | null = null;
  private connected: boolean = false;

  constructor(serverUrl: string, containerId: number, matchId: number) {
    this.serverUrl = serverUrl.replace(/^http/, "ws");
    this.containerId = containerId;
    this.matchId = matchId;
  }

  connect(): void {
    if (this.ws && this.connected) {
      console.log("WebSocket already connected");
      return;
    }

    // Get JWT token from localStorage for authentication
    const token = localStorage.getItem("authToken");
    const tokenParam = token ? `?token=${encodeURIComponent(token)}` : "";
    const wsUrl = `${this.serverUrl}/ws/containers/${this.containerId}/matches/${this.matchId}/snapshot${tokenParam}`;
    console.log(`Connecting to WebSocket: ${wsUrl}`);

    try {
      this.ws = new WebSocket(wsUrl);
      this.setupEventHandlers();
    } catch (error) {
      console.error("Failed to create WebSocket:", error);
      this.scheduleReconnect();
    }
  }

  private setupEventHandlers(): void {
    if (!this.ws) return;

    this.ws.onopen = () => {
      console.log("WebSocket connected");
      this.connected = true;
      this.reconnectAttempts = 0;
    };

    this.ws.onmessage = (event) => {
      try {
        const snapshot = JSON.parse(event.data) as SnapshotData;
        this.notifyListeners(snapshot);
      } catch (error) {
        console.error("Failed to parse snapshot:", error);
      }
    };

    this.ws.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    this.ws.onclose = (event) => {
      console.log(`WebSocket closed: ${event.code} ${event.reason}`);
      this.connected = false;
      this.ws = null;
      this.scheduleReconnect();
    };
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log("Max reconnect attempts reached");
      return;
    }

    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts);
    console.log(
      `Scheduling reconnect in ${delay}ms (attempt ${this.reconnectAttempts + 1})`,
    );

    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectAttempts++;
      this.connect();
    }, delay);
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.connected = false;
    console.log("WebSocket disconnected");
  }

  isConnected(): boolean {
    return this.connected;
  }

  requestSnapshot(): void {
    if (this.ws && this.connected) {
      this.ws.send(JSON.stringify({ type: "request_snapshot" }));
    }
  }

  addListener(listener: SnapshotListener): void {
    this.listeners.add(listener);
  }

  removeListener(listener: SnapshotListener): void {
    this.listeners.delete(listener);
  }

  private notifyListeners(snapshot: SnapshotData): void {
    for (const listener of this.listeners) {
      try {
        listener(snapshot);
      } catch (error) {
        console.error("Error in snapshot listener:", error);
      }
    }
  }

  getMatchId(): number {
    return this.matchId;
  }

  getContainerId(): number {
    return this.containerId;
  }

  setMatch(containerId: number, matchId: number): void {
    if (this.containerId !== containerId || this.matchId !== matchId) {
      this.containerId = containerId;
      this.matchId = matchId;
      if (this.connected) {
        this.disconnect();
        this.connect();
      }
    }
  }
}
