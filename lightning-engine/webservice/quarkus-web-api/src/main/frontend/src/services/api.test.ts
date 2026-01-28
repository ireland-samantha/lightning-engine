/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { server } from "../test/mocks/server";
import {
    apiClient, buildPlayerDeltaSnapshotWebSocketUrl, buildPlayerSnapshotWebSocketUrl, buildSnapshotWebSocketUrl
} from "./api";

describe("ApiClient", () => {
  beforeEach(() => {
    apiClient.clearAuthToken();
    server.resetHandlers();
  });

  describe("getMatches", () => {
    it("fetches matches successfully", async () => {
      server.use(
        http.get("/api/matches", () => {
          return HttpResponse.json([{ id: 1 }, { id: 2 }]);
        }),
      );

      const matches = await apiClient.getMatches();
      expect(matches).toEqual([{ id: 1 }, { id: 2 }]);
    });

    it("throws on API error", async () => {
      server.use(
        http.get("/api/matches", () => {
          return new HttpResponse("Internal Server Error", { status: 500 });
        }),
      );

      await expect(apiClient.getMatches()).rejects.toThrow("API error: 500");
    });
  });

  describe("authentication", () => {
    it("sets auth token after login", async () => {
      await apiClient.login("admin", "admin");
      expect(apiClient.isAuthenticated()).toBe(true);
      expect(apiClient.getAuthToken()).toBe("mock-jwt-token");
    });

    it("clears auth token", async () => {
      apiClient.setAuthToken("some-token");
      apiClient.clearAuthToken();
      expect(apiClient.isAuthenticated()).toBe(false);
      expect(apiClient.getAuthToken()).toBeNull();
    });

    it("reports authentication status", () => {
      expect(apiClient.isAuthenticated()).toBe(false);
      apiClient.setAuthToken("token");
      expect(apiClient.isAuthenticated()).toBe(true);
    });
  });

  describe("simulation", () => {
    it("sends tick request", async () => {
      server.use(
        http.post("/api/simulation/tick", () => {
          return HttpResponse.json({ tick: 42 });
        }),
      );

      const tick = await apiClient.tick();
      expect(tick).toBe(42);
    });

    it("sends play request with interval", async () => {
      server.use(
        http.post("/api/simulation/play", () => {
          return new HttpResponse(null, { status: 204 });
        }),
      );

      await expect(apiClient.play(16)).resolves.not.toThrow();
    });

    it("sends stop request", async () => {
      server.use(
        http.post("/api/simulation/stop", () => {
          return new HttpResponse(null, { status: 204 });
        }),
      );

      await expect(apiClient.stop()).resolves.not.toThrow();
    });
  });

  describe("history", () => {
    it("fetches history summary", async () => {
      const mockSummary = {
        totalSnapshots: 100,
        matchCount: 2,
        matchIds: [1, 2],
      };

      server.use(
        http.get("/api/history", () => {
          return HttpResponse.json(mockSummary);
        }),
      );

      const summary = await apiClient.getHistorySummary();
      expect(summary).toEqual(mockSummary);
    });

    it("fetches delta between ticks", async () => {
      const mockDelta = {
        matchId: 1,
        fromTick: 10,
        toTick: 20,
        addedEntities: [5],
        removedEntities: [2],
      };

      server.use(
        http.get("/api/snapshots/delta/1", () => {
          return HttpResponse.json(mockDelta);
        }),
      );

      const delta = await apiClient.getDelta(1, 10, 20);
      expect(delta).toEqual(mockDelta);
    });
  });

  describe("player-match", () => {
    it("joins player to match and returns WebSocket endpoints", async () => {
      const mockResponse = {
        playerId: 1,
        matchId: 1,
        snapshotWebSocketUrl: "ws://localhost/ws/matches/1/players/1/snapshot",
        deltaSnapshotWebSocketUrl:
          "ws://localhost/ws/matches/1/players/1/snapshot/delta",
        errorWebSocketUrl: "ws://localhost/ws/matches/1/players/1/errors",
        restSnapshotUrl: "/api/snapshots/match/1/player/1",
      };

      server.use(
        http.post("/api/player-matches", () => {
          return HttpResponse.json(mockResponse);
        }),
      );

      const response = await apiClient.joinPlayerToMatch(1, 1);
      expect(response.snapshotWebSocketUrl).toBe(
        mockResponse.snapshotWebSocketUrl,
      );
      expect(response.deltaSnapshotWebSocketUrl).toBe(
        mockResponse.deltaSnapshotWebSocketUrl,
      );
      expect(response.errorWebSocketUrl).toBe(mockResponse.errorWebSocketUrl);
    });
  });

  describe("snapshots", () => {
    it("fetches player-scoped snapshot", async () => {
      const mockSnapshot = {
        matchId: 1,
        tick: 42,
        modules: [
          {
            name: "EntityModule",
            version: "1.0",
            components: [{ name: "ENTITY_ID", values: [1, 2] }],
          },
        ],
      };

      server.use(
        http.get("/api/snapshots/match/1/player/1", () => {
          return HttpResponse.json(mockSnapshot);
        }),
      );

      const snapshot = await apiClient.getPlayerSnapshot(1, 1);
      expect(snapshot.matchId).toBe(1);
      expect(snapshot.tick).toBe(42);
    });
  });
});

describe("WebSocket URL builders", () => {
  it("builds snapshot WebSocket URL", () => {
    const url = buildSnapshotWebSocketUrl(1, 2);
    expect(url).toContain("/ws/containers/1/matches/2/snapshot");
  });

  it("builds player snapshot WebSocket URL", () => {
    const url = buildPlayerSnapshotWebSocketUrl(1, 2, 3);
    expect(url).toContain("/ws/containers/1/matches/2/players/3/snapshot");
  });

  it("builds player delta snapshot WebSocket URL", () => {
    const url = buildPlayerDeltaSnapshotWebSocketUrl(1, 2, 3);
    expect(url).toContain("/ws/containers/1/matches/2/players/3/delta");
  });
});
