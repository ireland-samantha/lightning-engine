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
 * Complete REST API client for the Lightning Engine backend.
 * Covers all 82 endpoints across all resource categories.
 */

// ============================================================================
// Types
// ============================================================================

export interface LoginResponse {
  token: string;
  refreshToken?: string;
}

export interface UserData {
  id: number;
  username: string;
  email?: string;
  roles: string[];
  enabled: boolean;
  createdAt?: string;
  lastLogin?: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  roles?: string[];
}

export interface RoleData {
  id: number;
  name: string;
  description?: string;
  includedRoles?: string[];
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
  includedRoles?: string[];
}

export interface MatchData {
  id: number;
  name?: string;
  status?: string;
  tick?: number;
  modules?: string[];
  createdAt?: string;
}

export interface PlayerData {
  id: number;
  name?: string;
  externalId?: string;
  createdAt?: string;
}

export interface PlayerMatchData {
  playerId: number;
  matchId: number;
  joinedAt?: string;
  team?: number;
}

/**
 * Response from joining a match, includes WebSocket endpoints for player-scoped snapshots.
 */
export interface JoinMatchResponse {
  playerId: number;
  matchId: number;
  snapshotWebSocketUrl: string;
  deltaSnapshotWebSocketUrl: string;
  errorWebSocketUrl: string;
  restSnapshotUrl: string;
}

/**
 * Game error from the backend error stream.
 */
export interface GameError {
  id: string;
  timestamp: string;
  matchId: number;
  playerId: number;
  type: "COMMAND" | "SYSTEM" | "GENERAL";
  source: string;
  message: string;
  details: string;
}

export interface SessionData {
  id: number;
  playerId: number | null;
  matchId: number;
  token?: string;
  sessionToken?: string;
  status?: "CONNECTED" | "DISCONNECTED" | "ABANDONED";
  connectedAt?: string;
  disconnectedAt?: string;
}

export interface CommandData {
  name: string;
  description?: string;
  parameters?: CommandParameter[];
  module?: string;
}

export interface CommandParameter {
  name: string;
  type: string;
  required: boolean;
  description?: string;
}

export interface EnqueueCommandRequest {
  matchId: number;
  playerId?: number;
  commandName: string;
  parameters?: Record<string, unknown>;
}

export interface SimulationStatus {
  playing: boolean;
  currentTick: number;
  intervalMs?: number;
}

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

export interface DeltaSnapshotData {
  matchId: number;
  fromTick: number;
  toTick: number;
  changes?: Record<string, unknown>;
  addedEntities: number[];
  removedEntities: number[];
  changedCount?: number;
}

export interface HistorySummary {
  totalSnapshots: number;
  matchCount: number;
  matchIds: number[];
  oldestTick?: number;
  newestTick?: number;
}

export interface MatchHistorySummary {
  matchId: number;
  snapshotCount: number;
  firstTick: number;
  lastTick: number;
  oldestTimestamp?: string;
  newestTimestamp?: string;
}

export interface ModuleData {
  name: string;
  version?: string;
  description?: string;
  components: string[];
  commands?: string[];
  source?: "builtin" | "jar";
  jarFile?: string;
}

export interface AIData {
  name: string;
  version?: string;
  description?: string;
  requiredModules?: string[];
  source?: "builtin" | "jar";
  jarFile?: string;
}

export interface ResourceData {
  id: string;
  name: string;
  mimeType?: string;
  contentType?: string;
  size: number;
  chunked?: boolean;
  chunkCount?: number;
  createdAt?: string;
}

export interface ResourceChunkInfo {
  resourceId: string;
  chunkIndex: number;
  size: number;
  checksum?: string;
}

export interface GuiInfo {
  version: string;
  downloadUrl: string;
  jarUrl: string;
}

export interface RestoreConfig {
  enabled: boolean;
  autoRestore: boolean;
  persistenceType?: string;
}

export interface ContainerData {
  id: number;
  name: string;
  status:
    | "CREATED"
    | "STARTING"
    | "RUNNING"
    | "PAUSED"
    | "STOPPING"
    | "STOPPED";
  currentTick: number;
  autoAdvancing: boolean;
  autoAdvanceIntervalMs: number;
  matchCount?: number;
  moduleCount?: number;
  loadedModules?: string[];
}

export interface CreateContainerRequest {
  name: string;
  maxEntities?: number;
  maxComponents?: number;
  maxCommandsPerTick?: number;
  maxMemoryMb?: number;
  moduleJars?: string[];
  moduleScanDirectory?: string;
}

export interface ContainerStatsData {
  entityCount: number;
  maxEntities: number;
  usedMemoryBytes: number;
  maxMemoryBytes: number;
  jvmMaxMemoryBytes: number;
  jvmUsedMemoryBytes: number;
  matchCount: number;
  moduleCount: number;
}

export interface SystemMetricsData {
  systemName: string;
  executionTimeMs: number;
  executionTimeNanos: number;
  success: boolean;
}

export interface CommandMetricsData {
  commandName: string;
  executionTimeMs: number;
  executionTimeNanos: number;
  success: boolean;
}

export interface SnapshotMetricsData {
  totalGenerations: number;
  cacheHits: number;
  cacheMisses: number;
  incrementalUpdates: number;
  fullRebuilds: number;
  avgGenerationMs: number;
  lastGenerationMs: number;
  maxGenerationMs: number;
  cacheHitRate: number;
  incrementalRate: number;
}

export interface ContainerMetricsData {
  containerId: number;
  currentTick: number;
  lastTickMs: number;
  avgTickMs: number;
  minTickMs: number;
  maxTickMs: number;
  totalTicks: number;
  lastTickNanos: number;
  avgTickNanos: number;
  minTickNanos: number;
  maxTickNanos: number;
  totalEntities: number;
  totalComponentTypes: number;
  commandQueueSize: number;
  snapshotMetrics?: SnapshotMetricsData;
  lastTickSystems?: SystemMetricsData[];
  lastTickCommands?: CommandMetricsData[];
}

// Legacy aliases for backward compatibility
export type PartitionData = ContainerData;
export type CreatePartitionRequest = CreateContainerRequest;

// ============================================================================
// API Client
// ============================================================================

class ApiClient {
  private baseUrl: string;
  private authToken: string | null = null;
  private refreshToken: string | null = null;

  constructor(baseUrl: string = "") {
    this.baseUrl = baseUrl;
  }

  // === Token Management ===

  setAuthToken(token: string): void {
    this.authToken = token;
  }

  setRefreshToken(token: string): void {
    this.refreshToken = token;
  }

  clearAuthToken(): void {
    this.authToken = null;
    this.refreshToken = null;
  }

  isAuthenticated(): boolean {
    return this.authToken !== null;
  }

  getAuthToken(): string | null {
    return this.authToken;
  }

  logout(): void {
    this.clearAuthToken();
  }

  // === HTTP Methods ===

  private async fetch<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...((options.headers as Record<string, string>) || {}),
    };

    if (this.authToken) {
      headers["Authorization"] = `Bearer ${this.authToken}`;
    }

    const response = await fetch(`${this.baseUrl}${path}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "");
      throw new Error(
        `API error: ${response.status} ${response.statusText}${errorText ? ` - ${errorText}` : ""}`,
      );
    }

    const text = await response.text();
    return text ? JSON.parse(text) : ({} as T);
  }

  private async fetchBlob(path: string): Promise<Blob> {
    const headers: Record<string, string> = {};
    if (this.authToken) {
      headers["Authorization"] = `Bearer ${this.authToken}`;
    }

    const response = await fetch(`${this.baseUrl}${path}`, { headers });
    if (!response.ok) {
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }
    return response.blob();
  }

  private async uploadFile<T>(
    path: string,
    file: File | Blob,
    filename?: string,
  ): Promise<T> {
    const formData = new FormData();
    formData.append("file", file, filename || "upload");

    const headers: Record<string, string> = {};
    if (this.authToken) {
      headers["Authorization"] = `Bearer ${this.authToken}`;
    }

    const response = await fetch(`${this.baseUrl}${path}`, {
      method: "POST",
      headers,
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }

    const text = await response.text();
    return text ? JSON.parse(text) : ({} as T);
  }

  // =========================================================================
  // AUTH ENDPOINTS (/api/auth)
  // =========================================================================

  async login(username: string, password: string): Promise<LoginResponse> {
    const response = await this.fetch<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
    if (response.token) {
      this.authToken = response.token;
    }
    if (response.refreshToken) {
      this.refreshToken = response.refreshToken;
    }
    return response;
  }

  async refreshAuthToken(): Promise<LoginResponse> {
    const response = await this.fetch<LoginResponse>("/api/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken: this.refreshToken }),
    });
    if (response.token) {
      this.authToken = response.token;
    }
    return response;
  }

  async getCurrentUser(): Promise<UserData> {
    return this.fetch<UserData>("/api/auth/me");
  }

  // =========================================================================
  // USER ENDPOINTS (/api/auth/users)
  // =========================================================================

  async getUsers(): Promise<UserData[]> {
    return this.fetch<UserData[]>("/api/auth/users");
  }

  async getUser(userId: number): Promise<UserData> {
    return this.fetch<UserData>(`/api/auth/users/${userId}`);
  }

  async getUserByUsername(username: string): Promise<UserData> {
    return this.fetch<UserData>(`/api/auth/users/username/${username}`);
  }

  async createUser(request: CreateUserRequest): Promise<UserData> {
    return this.fetch<UserData>("/api/auth/users", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateUserPassword(userId: number, newPassword: string): Promise<void> {
    await this.fetch<void>(`/api/auth/users/${userId}/password`, {
      method: "PUT",
      body: JSON.stringify({ password: newPassword }),
    });
  }

  async updateUserRoles(userId: number, roles: string[]): Promise<UserData> {
    return this.fetch<UserData>(`/api/auth/users/${userId}/roles`, {
      method: "PUT",
      body: JSON.stringify({ roles }),
    });
  }

  async addUserRole(userId: number, roleName: string): Promise<UserData> {
    return this.fetch<UserData>(`/api/auth/users/${userId}/roles/${roleName}`, {
      method: "POST",
    });
  }

  async removeUserRole(userId: number, roleName: string): Promise<UserData> {
    return this.fetch<UserData>(`/api/auth/users/${userId}/roles/${roleName}`, {
      method: "DELETE",
    });
  }

  async setUserEnabled(userId: number, enabled: boolean): Promise<UserData> {
    return this.fetch<UserData>(`/api/auth/users/${userId}/enabled`, {
      method: "PUT",
      body: JSON.stringify({ enabled }),
    });
  }

  async deleteUser(userId: number): Promise<void> {
    await this.fetch<void>(`/api/auth/users/${userId}`, { method: "DELETE" });
  }

  // =========================================================================
  // ROLE ENDPOINTS (/api/auth/roles)
  // =========================================================================

  async getRoles(): Promise<RoleData[]> {
    return this.fetch<RoleData[]>("/api/auth/roles");
  }

  async getRole(roleId: number): Promise<RoleData> {
    return this.fetch<RoleData>(`/api/auth/roles/${roleId}`);
  }

  async getRoleByName(roleName: string): Promise<RoleData> {
    return this.fetch<RoleData>(`/api/auth/roles/name/${roleName}`);
  }

  async createRole(request: CreateRoleRequest): Promise<RoleData> {
    return this.fetch<RoleData>("/api/auth/roles", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateRoleDescription(
    roleId: number,
    description: string,
  ): Promise<RoleData> {
    return this.fetch<RoleData>(`/api/auth/roles/${roleId}/description`, {
      method: "PUT",
      body: JSON.stringify({ description }),
    });
  }

  async updateRoleIncludes(
    roleId: number,
    includes: string[],
  ): Promise<RoleData> {
    return this.fetch<RoleData>(`/api/auth/roles/${roleId}/includes`, {
      method: "PUT",
      body: JSON.stringify({ includes }),
    });
  }

  async deleteRole(roleId: number): Promise<void> {
    await this.fetch<void>(`/api/auth/roles/${roleId}`, { method: "DELETE" });
  }

  // =========================================================================
  // MATCH ENDPOINTS (/api/matches)
  // =========================================================================

  async getMatches(): Promise<MatchData[]> {
    return this.fetch<MatchData[]>("/api/matches");
  }

  async getMatch(matchId: number): Promise<MatchData> {
    return this.fetch<MatchData>(`/api/matches/${matchId}`);
  }

  async createMatch(modules?: string[]): Promise<MatchData> {
    return this.fetch<MatchData>("/api/matches", {
      method: "POST",
      body: JSON.stringify({ modules: modules || [] }),
    });
  }

  async deleteMatch(matchId: number): Promise<void> {
    await this.fetch<void>(`/api/matches/${matchId}`, { method: "DELETE" });
  }

  // =========================================================================
  // PLAYER ENDPOINTS (/api/players)
  // =========================================================================

  async getPlayers(): Promise<PlayerData[]> {
    return this.fetch<PlayerData[]>("/api/players");
  }

  async getPlayer(playerId: number): Promise<PlayerData> {
    return this.fetch<PlayerData>(`/api/players/${playerId}`);
  }

  async createPlayer(name?: string, externalId?: string): Promise<PlayerData> {
    return this.fetch<PlayerData>("/api/players", {
      method: "POST",
      body: JSON.stringify({ name, externalId }),
    });
  }

  async deletePlayer(playerId: number): Promise<void> {
    await this.fetch<void>(`/api/players/${playerId}`, { method: "DELETE" });
  }

  // =========================================================================
  // PLAYER-MATCH ENDPOINTS (/api/player-matches)
  // =========================================================================

  /**
   * Join a player to a match.
   * Returns WebSocket endpoints for player-scoped snapshot streaming.
   */
  async joinPlayerToMatch(
    playerId: number,
    matchId: number,
    team?: number,
  ): Promise<JoinMatchResponse> {
    return this.fetch<JoinMatchResponse>("/api/player-matches", {
      method: "POST",
      body: JSON.stringify({ playerId, matchId, team }),
    });
  }

  async getPlayerMatch(
    playerId: number,
    matchId: number,
  ): Promise<PlayerMatchData> {
    return this.fetch<PlayerMatchData>(
      `/api/player-matches/player/${playerId}/match/${matchId}`,
    );
  }

  async getMatchPlayers(matchId: number): Promise<PlayerMatchData[]> {
    return this.fetch<PlayerMatchData[]>(
      `/api/player-matches/match/${matchId}`,
    );
  }

  async getPlayerMatches(playerId: number): Promise<PlayerMatchData[]> {
    return this.fetch<PlayerMatchData[]>(
      `/api/player-matches/player/${playerId}`,
    );
  }

  async removePlayerFromMatch(
    playerId: number,
    matchId: number,
  ): Promise<void> {
    await this.fetch<void>(
      `/api/player-matches/player/${playerId}/match/${matchId}`,
      {
        method: "DELETE",
      },
    );
  }

  // =========================================================================
  // SESSION ENDPOINTS (/api/sessions)
  // =========================================================================

  async connectSession(
    playerId: number,
    matchId: number,
  ): Promise<SessionData> {
    return this.fetch<SessionData>("/api/sessions/connect", {
      method: "POST",
      body: JSON.stringify({ playerId, matchId }),
    });
  }

  async getSession(playerId: number, matchId: number): Promise<SessionData> {
    return this.fetch<SessionData>(
      `/api/sessions/player/${playerId}/match/${matchId}`,
    );
  }

  async canReconnect(playerId: number, matchId: number): Promise<boolean> {
    const result = await this.fetch<{ canReconnect: boolean }>(
      `/api/sessions/player/${playerId}/match/${matchId}/can-reconnect`,
    );
    return result.canReconnect;
  }

  async getMatchSessions(matchId: number): Promise<SessionData[]> {
    return this.fetch<SessionData[]>(`/api/sessions/match/${matchId}`);
  }

  async getActiveMatchSessions(matchId: number): Promise<SessionData[]> {
    return this.fetch<SessionData[]>(`/api/sessions/match/${matchId}/active`);
  }

  async getSessions(): Promise<SessionData[]> {
    return this.fetch<SessionData[]>("/api/sessions");
  }

  /**
   * Create a session by connecting a player to a match.
   */
  async createSession(playerId: number, matchId: number): Promise<SessionData> {
    return this.fetch<SessionData>("/api/sessions/connect", {
      method: "POST",
      body: JSON.stringify({ playerId, matchId }),
    });
  }

  /**
   * Reconnect a player to an existing disconnected session.
   */
  async reconnectSession(
    playerId: number,
    matchId: number,
  ): Promise<SessionData> {
    return this.fetch<SessionData>("/api/sessions/reconnect", {
      method: "POST",
      body: JSON.stringify({ playerId, matchId }),
    });
  }

  /**
   * Disconnect a player's session (can be reconnected later).
   */
  async disconnectSession(playerId: number, matchId: number): Promise<void> {
    await this.fetch<void>("/api/sessions/disconnect", {
      method: "POST",
      body: JSON.stringify({ playerId, matchId }),
    });
  }

  /**
   * Abandon a player's session (cannot be reconnected).
   */
  async abandonSession(playerId: number, matchId: number): Promise<void> {
    await this.fetch<void>("/api/sessions/abandon", {
      method: "POST",
      body: JSON.stringify({ playerId, matchId }),
    });
  }

  // =========================================================================
  // COMMAND ENDPOINTS (/api/commands)
  // =========================================================================

  async getCommands(): Promise<CommandData[]> {
    return this.fetch<CommandData[]>("/api/commands");
  }

  async enqueueCommand(request: EnqueueCommandRequest): Promise<void> {
    await this.fetch<void>("/api/commands", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async sendCommandToMatch(
    matchId: number,
    commandName: string,
    parameters?: Record<string, unknown>,
  ): Promise<void> {
    await this.enqueueCommand({ matchId, commandName, parameters });
  }

  async sendCommandToSession(
    sessionId: number,
    commandName: string,
    parameters?: Record<string, unknown>,
  ): Promise<void> {
    await this.fetch<void>(`/api/commands/session/${sessionId}`, {
      method: "POST",
      body: JSON.stringify({ commandName, parameters }),
    });
  }

  // =========================================================================
  // SIMULATION ENDPOINTS (/api/simulation)
  // =========================================================================

  async getCurrentTick(): Promise<number> {
    const result = await this.fetch<{ tick: number }>("/api/simulation/tick");
    return result.tick;
  }

  async tick(): Promise<number> {
    const result = await this.fetch<{ tick: number }>("/api/simulation/tick", {
      method: "POST",
    });
    return result.tick ?? 0;
  }

  async play(intervalMs: number = 16): Promise<void> {
    await this.fetch<void>(`/api/simulation/play?intervalMs=${intervalMs}`, {
      method: "POST",
    });
  }

  async stop(): Promise<void> {
    await this.fetch<void>("/api/simulation/stop", { method: "POST" });
  }

  async getSimulationStatus(): Promise<SimulationStatus> {
    return this.fetch<SimulationStatus>("/api/simulation/status");
  }

  // =========================================================================
  // SNAPSHOT ENDPOINTS (/api/snapshots)
  // =========================================================================

  async getAllSnapshots(): Promise<Record<number, SnapshotData>> {
    return this.fetch<Record<number, SnapshotData>>("/api/snapshots");
  }

  async getSnapshot(matchId: number): Promise<SnapshotData> {
    return this.fetch<SnapshotData>(`/api/snapshots/match/${matchId}`);
  }

  /**
   * Get player-scoped snapshot for a specific match.
   * Returns only entities owned by the specified player.
   */
  async getPlayerSnapshot(
    matchId: number,
    playerId: number,
  ): Promise<SnapshotData> {
    return this.fetch<SnapshotData>(
      `/api/snapshots/match/${matchId}/player/${playerId}`,
    );
  }

  // =========================================================================
  // DELTA SNAPSHOT ENDPOINTS (/api/snapshots/delta)
  // =========================================================================

  async getDelta(
    matchId: number,
    fromTick: number,
    toTick: number,
  ): Promise<DeltaSnapshotData> {
    return this.fetch<DeltaSnapshotData>(
      `/api/snapshots/delta/${matchId}?fromTick=${fromTick}&toTick=${toTick}`,
    );
  }

  async recordSnapshotForDelta(matchId: number): Promise<void> {
    await this.fetch<void>(`/api/snapshots/delta/${matchId}/record`, {
      method: "POST",
    });
  }

  async getDeltaHistory(
    matchId: number,
  ): Promise<{ ticks: number[]; count: number }> {
    return this.fetch<{ ticks: number[]; count: number }>(
      `/api/snapshots/delta/${matchId}/history`,
    );
  }

  async clearDeltaHistory(matchId: number): Promise<void> {
    await this.fetch<void>(`/api/snapshots/delta/${matchId}/history`, {
      method: "DELETE",
    });
  }

  // =========================================================================
  // HISTORY ENDPOINTS (/api/history)
  // =========================================================================

  async getHistorySummary(): Promise<HistorySummary> {
    return this.fetch<HistorySummary>("/api/history");
  }

  async getMatchHistorySummary(matchId: number): Promise<MatchHistorySummary> {
    return this.fetch<MatchHistorySummary>(`/api/history/${matchId}`);
  }

  async getHistorySnapshots(
    matchId: number,
    options?: { fromTick?: number; toTick?: number; limit?: number },
  ): Promise<SnapshotData[]> {
    const params = new URLSearchParams();
    if (options?.fromTick !== undefined)
      params.set("fromTick", String(options.fromTick));
    if (options?.toTick !== undefined)
      params.set("toTick", String(options.toTick));
    if (options?.limit !== undefined)
      params.set("limit", String(options.limit));
    const query = params.toString();
    return this.fetch<SnapshotData[]>(
      `/api/history/${matchId}/snapshots${query ? `?${query}` : ""}`,
    );
  }

  async getLatestSnapshots(
    matchId: number,
    limit: number = 10,
  ): Promise<SnapshotData[]> {
    return this.fetch<SnapshotData[]>(
      `/api/history/${matchId}/snapshots/latest?limit=${limit}`,
    );
  }

  async getSnapshotAtTick(
    matchId: number,
    tick: number,
  ): Promise<SnapshotData> {
    return this.fetch<SnapshotData>(`/api/history/${matchId}/snapshot/${tick}`);
  }

  async deleteMatchHistory(matchId: number): Promise<void> {
    await this.fetch<void>(`/api/history/${matchId}`, { method: "DELETE" });
  }

  async deleteOldSnapshots(
    matchId: number,
    olderThanTick: number,
  ): Promise<void> {
    await this.fetch<void>(
      `/api/history/${matchId}/older-than/${olderThanTick}`,
      {
        method: "DELETE",
      },
    );
  }

  // =========================================================================
  // MODULE ENDPOINTS (/api/modules)
  // =========================================================================

  async getModules(): Promise<ModuleData[]> {
    return this.fetch<ModuleData[]>("/api/modules");
  }

  async getModule(moduleName: string): Promise<ModuleData> {
    return this.fetch<ModuleData>(`/api/modules/${moduleName}`);
  }

  async uploadModule(jarFile: File): Promise<ModuleData> {
    return this.uploadFile<ModuleData>(
      "/api/modules/upload",
      jarFile,
      jarFile.name,
    );
  }

  async uninstallModule(moduleName: string): Promise<void> {
    await this.fetch<void>(`/api/modules/${moduleName}`, { method: "DELETE" });
  }

  async deleteModule(moduleName: string): Promise<void> {
    return this.uninstallModule(moduleName);
  }

  async reloadModules(): Promise<ModuleData[]> {
    return this.fetch<ModuleData[]>("/api/modules/reload", { method: "POST" });
  }

  // =========================================================================
  // AI ENDPOINTS (/api/ai)
  // =========================================================================

  async getAIs(): Promise<AIData[]> {
    return this.fetch<AIData[]>("/api/ai");
  }

  async getAI(aiName: string): Promise<AIData> {
    return this.fetch<AIData>(`/api/ai/${aiName}`);
  }

  async uploadAI(jarFile: File): Promise<AIData> {
    return this.uploadFile<AIData>("/api/ai/upload", jarFile, jarFile.name);
  }

  async uninstallAI(aiName: string): Promise<void> {
    await this.fetch<void>(`/api/ai/${aiName}`, { method: "DELETE" });
  }

  async deleteAI(aiName: string): Promise<void> {
    return this.uninstallAI(aiName);
  }

  async reloadAIs(): Promise<AIData[]> {
    return this.fetch<AIData[]>("/api/ai/reload", { method: "POST" });
  }

  // =========================================================================
  // RESOURCE ENDPOINTS (/api/resources)
  // =========================================================================

  async getResources(): Promise<ResourceData[]> {
    return this.fetch<ResourceData[]>("/api/resources");
  }

  async getResource(resourceId: string): Promise<ResourceData> {
    return this.fetch<ResourceData>(`/api/resources/${resourceId}`);
  }

  async uploadResource(file: File, name?: string): Promise<ResourceData> {
    return this.uploadFile<ResourceData>(
      "/api/resources",
      file,
      name || file.name,
    );
  }

  async uploadResourceChunk(
    resourceId: string,
    chunkIndex: number,
    chunk: Blob,
  ): Promise<void> {
    await this.uploadFile<void>(
      `/api/resources/${resourceId}/chunks/${chunkIndex}`,
      chunk,
    );
  }

  async downloadResource(resourceId: string): Promise<Blob> {
    return this.fetchBlob(`/api/resources/${resourceId}/data`);
  }

  async getResourceChunks(resourceId: string): Promise<ResourceChunkInfo[]> {
    return this.fetch<ResourceChunkInfo[]>(
      `/api/resources/${resourceId}/chunks`,
    );
  }

  async downloadResourceChunk(
    resourceId: string,
    chunkIndex: number,
  ): Promise<Blob> {
    return this.fetchBlob(`/api/resources/${resourceId}/chunks/${chunkIndex}`);
  }

  async deleteResource(resourceId: string): Promise<void> {
    await this.fetch<void>(`/api/resources/${resourceId}`, {
      method: "DELETE",
    });
  }

  // =========================================================================
  // GUI DOWNLOAD ENDPOINTS (/api/gui)
  // =========================================================================

  async getGuiInfo(): Promise<GuiInfo> {
    return this.fetch<GuiInfo>("/api/gui/info");
  }

  async downloadGui(): Promise<Blob> {
    return this.fetchBlob("/api/gui/download");
  }

  async downloadGuiJar(): Promise<Blob> {
    return this.fetchBlob("/api/gui/download/jar");
  }

  // =========================================================================
  // RESTORE ENDPOINTS (/api/restore)
  // =========================================================================

  async restoreMatch(matchId: number, tick?: number): Promise<void> {
    const params = tick !== undefined ? `?tick=${tick}` : "";
    await this.fetch<void>(`/api/restore/match/${matchId}${params}`, {
      method: "POST",
    });
  }

  async restoreAllMatches(): Promise<void> {
    await this.fetch<void>("/api/restore/all", { method: "POST" });
  }

  async canRestoreMatch(matchId: number): Promise<boolean> {
    const result = await this.fetch<{ available: boolean }>(
      `/api/restore/match/${matchId}/available`,
    );
    return result.available;
  }

  async getRestoreConfig(): Promise<RestoreConfig> {
    return this.fetch<RestoreConfig>("/api/restore/config");
  }

  // =========================================================================
  // CONTAINER ENDPOINTS (/api/containers)
  // =========================================================================

  async getContainers(): Promise<ContainerData[]> {
    return this.fetch<ContainerData[]>("/api/containers");
  }

  async getContainer(containerId: number): Promise<ContainerData> {
    return this.fetch<ContainerData>(`/api/containers/${containerId}`);
  }

  async createContainer(
    request: CreateContainerRequest,
  ): Promise<ContainerData> {
    return this.fetch<ContainerData>("/api/containers", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async deleteContainer(containerId: number): Promise<void> {
    await this.fetch<void>(`/api/containers/${containerId}`, {
      method: "DELETE",
    });
  }

  async startContainer(containerId: number): Promise<ContainerData> {
    return this.fetch<ContainerData>(`/api/containers/${containerId}/start`, {
      method: "POST",
    });
  }

  async stopContainer(containerId: number): Promise<ContainerData> {
    return this.fetch<ContainerData>(`/api/containers/${containerId}/stop`, {
      method: "POST",
    });
  }

  async pauseContainer(containerId: number): Promise<ContainerData> {
    return this.fetch<ContainerData>(`/api/containers/${containerId}/pause`, {
      method: "POST",
    });
  }

  async resumeContainer(containerId: number): Promise<ContainerData> {
    return this.fetch<ContainerData>(`/api/containers/${containerId}/resume`, {
      method: "POST",
    });
  }

  async getContainerTick(containerId: number): Promise<{ tick: number }> {
    return this.fetch<{ tick: number }>(`/api/containers/${containerId}/tick`);
  }

  async advanceContainerTick(containerId: number): Promise<{ tick: number }> {
    return this.fetch<{ tick: number }>(`/api/containers/${containerId}/tick`, {
      method: "POST",
    });
  }

  async playContainer(
    containerId: number,
    intervalMs: number = 16,
  ): Promise<ContainerData> {
    return this.fetch<ContainerData>(
      `/api/containers/${containerId}/play?intervalMs=${intervalMs}`,
      {
        method: "POST",
      },
    );
  }

  async startContainerAutoAdvance(
    containerId: number,
    intervalMs: number = 16,
  ): Promise<ContainerData> {
    return this.playContainer(containerId, intervalMs);
  }

  async stopContainerAutoAdvance(containerId: number): Promise<ContainerData> {
    return this.fetch<ContainerData>(
      `/api/containers/${containerId}/stop-auto`,
      {
        method: "POST",
      },
    );
  }

  async getContainerMatches(containerId: number): Promise<MatchData[]> {
    return this.fetch<MatchData[]>(`/api/containers/${containerId}/matches`);
  }

  async getMatchInContainer(
    containerId: number,
    matchId: number,
  ): Promise<MatchData> {
    return this.fetch<MatchData>(
      `/api/containers/${containerId}/matches/${matchId}`,
    );
  }

  async createMatchInContainer(
    containerId: number,
    matchId: number,
    enabledModuleNames?: string[],
    enabledAINames?: string[],
  ): Promise<MatchData> {
    return this.fetch<MatchData>(`/api/containers/${containerId}/matches`, {
      method: "POST",
      body: JSON.stringify({
        id: matchId,
        enabledModuleNames: enabledModuleNames || [],
        enabledAINames: enabledAINames || [],
      }),
    });
  }

  async deleteMatchFromContainer(
    containerId: number,
    matchId: number,
  ): Promise<void> {
    await this.fetch<void>(
      `/api/containers/${containerId}/matches/${matchId}`,
      {
        method: "DELETE",
      },
    );
  }

  async getContainerModules(containerId: number): Promise<string[]> {
    return this.fetch<string[]>(`/api/containers/${containerId}/modules`);
  }

  async reloadContainerModules(containerId: number): Promise<string[]> {
    return this.fetch<string[]>(
      `/api/containers/${containerId}/modules/reload`,
      {
        method: "POST",
      },
    );
  }

  async getContainerCommands(containerId: number): Promise<CommandData[]> {
    return this.fetch<CommandData[]>(`/api/containers/${containerId}/commands`);
  }

  async sendCommandToContainer(
    containerId: number,
    commandName: string,
    parameters?: Record<string, unknown>,
  ): Promise<void> {
    await this.fetch<void>(`/api/containers/${containerId}/commands`, {
      method: "POST",
      body: JSON.stringify({ commandName, parameters: parameters || {} }),
    });
  }

  async connectSessionInContainer(
    containerId: number,
    matchId: number,
    playerId: number,
  ): Promise<SessionData> {
    return this.fetch<SessionData>(
      `/api/containers/${containerId}/matches/${matchId}/sessions`,
      {
        method: "POST",
        body: JSON.stringify({ playerId }),
      },
    );
  }

  async getSessionsInContainer(
    containerId: number,
    matchId: number,
  ): Promise<SessionData[]> {
    return this.fetch<SessionData[]>(
      `/api/containers/${containerId}/matches/${matchId}/sessions`,
    );
  }

  /**
   * Get all sessions across all matches in a container.
   */
  async getAllContainerSessions(containerId: number): Promise<SessionData[]> {
    return this.fetch<SessionData[]>(`/api/containers/${containerId}/sessions`);
  }

  /**
   * Reconnect a player's session in a match.
   */
  async reconnectSessionInContainer(
    containerId: number,
    matchId: number,
    playerId: number,
  ): Promise<SessionData> {
    return this.fetch<SessionData>(
      `/api/containers/${containerId}/matches/${matchId}/sessions/${playerId}/reconnect`,
      { method: "POST" },
    );
  }

  /**
   * Disconnect a player's session in a match.
   */
  async disconnectSessionInContainer(
    containerId: number,
    matchId: number,
    playerId: number,
  ): Promise<void> {
    await this.fetch<void>(
      `/api/containers/${containerId}/matches/${matchId}/sessions/${playerId}/disconnect`,
      { method: "POST" },
    );
  }

  /**
   * Abandon a player's session in a match.
   */
  async abandonSessionInContainer(
    containerId: number,
    matchId: number,
    playerId: number,
  ): Promise<void> {
    await this.fetch<void>(
      `/api/containers/${containerId}/matches/${matchId}/sessions/${playerId}/abandon`,
      { method: "POST" },
    );
  }

  // =========================================================================
  // CONTAINER-SCOPED PLAYER ENDPOINTS
  // =========================================================================

  /**
   * Get all players (container-scoped).
   */
  async getPlayersInContainer(containerId: number): Promise<PlayerData[]> {
    return this.fetch<PlayerData[]>(`/api/containers/${containerId}/players`);
  }

  /**
   * Create a player (container-scoped).
   */
  async createPlayerInContainer(
    containerId: number,
    id?: number,
  ): Promise<PlayerData> {
    return this.fetch<PlayerData>(`/api/containers/${containerId}/players`, {
      method: "POST",
      body: JSON.stringify({ id }),
    });
  }

  /**
   * Get a player by ID (container-scoped).
   */
  async getPlayerInContainer(
    containerId: number,
    playerId: number,
  ): Promise<PlayerData> {
    return this.fetch<PlayerData>(
      `/api/containers/${containerId}/players/${playerId}`,
    );
  }

  /**
   * Delete a player (container-scoped).
   */
  async deletePlayerInContainer(
    containerId: number,
    playerId: number,
  ): Promise<void> {
    await this.fetch<void>(
      `/api/containers/${containerId}/players/${playerId}`,
      {
        method: "DELETE",
      },
    );
  }

  // =========================================================================
  // CONTAINER-SCOPED PLAYER-MATCH ENDPOINTS
  // =========================================================================

  /**
   * Join a player to a match in a container.
   */
  async joinPlayerToMatchInContainer(
    containerId: number,
    matchId: number,
    playerId: number,
  ): Promise<JoinMatchResponse> {
    return this.fetch<JoinMatchResponse>(
      `/api/containers/${containerId}/matches/${matchId}/players`,
      {
        method: "POST",
        body: JSON.stringify({ playerId }),
      },
    );
  }

  /**
   * Get all players in a match (container-scoped).
   */
  async getPlayersInMatch(
    containerId: number,
    matchId: number,
  ): Promise<PlayerMatchData[]> {
    return this.fetch<PlayerMatchData[]>(
      `/api/containers/${containerId}/matches/${matchId}/players`,
    );
  }

  /**
   * Remove a player from a match (container-scoped).
   */
  async removePlayerFromMatchInContainer(
    containerId: number,
    matchId: number,
    playerId: number,
  ): Promise<void> {
    await this.fetch<void>(
      `/api/containers/${containerId}/matches/${matchId}/players/${playerId}`,
      { method: "DELETE" },
    );
  }

  // Legacy aliases for backward compatibility
  async getPartitions(): Promise<ContainerData[]> {
    return this.getContainers();
  }

  async getPartition(partitionId: number): Promise<ContainerData> {
    return this.getContainer(partitionId);
  }

  async createPartition(
    request: CreateContainerRequest,
  ): Promise<ContainerData> {
    return this.createContainer(request);
  }

  async deletePartition(partitionId: number): Promise<void> {
    return this.deleteContainer(partitionId);
  }

  async startPartition(partitionId: number): Promise<ContainerData> {
    return this.startContainer(partitionId);
  }

  async stopPartition(partitionId: number): Promise<ContainerData> {
    return this.stopContainer(partitionId);
  }

  async pausePartition(partitionId: number): Promise<ContainerData> {
    return this.pauseContainer(partitionId);
  }

  async resumePartition(partitionId: number): Promise<ContainerData> {
    return this.resumeContainer(partitionId);
  }

  async getPartitionTick(partitionId: number): Promise<{ tick: number }> {
    return this.getContainerTick(partitionId);
  }

  async advancePartitionTick(partitionId: number): Promise<{ tick: number }> {
    return this.advanceContainerTick(partitionId);
  }

  async playPartition(
    partitionId: number,
    intervalMs: number = 16,
  ): Promise<ContainerData> {
    return this.playContainer(partitionId, intervalMs);
  }

  async startPartitionAutoAdvance(
    partitionId: number,
    intervalMs: number = 16,
  ): Promise<ContainerData> {
    return this.startContainerAutoAdvance(partitionId, intervalMs);
  }

  async stopPartitionAutoAdvance(partitionId: number): Promise<ContainerData> {
    return this.stopContainerAutoAdvance(partitionId);
  }

  async getPartitionMatches(partitionId: number): Promise<MatchData[]> {
    return this.getContainerMatches(partitionId);
  }

  async createMatchInPartition(
    partitionId: number,
    matchId: number,
    enabledModuleNames?: string[],
    enabledAINames?: string[],
  ): Promise<MatchData> {
    return this.createMatchInContainer(
      partitionId,
      matchId,
      enabledModuleNames,
      enabledAINames,
    );
  }

  async deleteMatchFromPartition(
    partitionId: number,
    matchId: number,
  ): Promise<void> {
    return this.deleteMatchFromContainer(partitionId, matchId);
  }

  async getPartitionModules(partitionId: number): Promise<string[]> {
    return this.getContainerModules(partitionId);
  }

  async reloadPartitionModules(partitionId: number): Promise<string[]> {
    return this.reloadContainerModules(partitionId);
  }

  async getPartitionCommands(partitionId: number): Promise<CommandData[]> {
    return this.getContainerCommands(partitionId);
  }

  async sendCommandToPartition(
    partitionId: number,
    commandName: string,
    parameters?: Record<string, unknown>,
  ): Promise<void> {
    return this.sendCommandToContainer(partitionId, commandName, parameters);
  }

  async connectSessionInPartition(
    partitionId: number,
    matchId: number,
    playerId: number,
  ): Promise<SessionData> {
    return this.connectSessionInContainer(partitionId, matchId, playerId);
  }
}

export const apiClient = new ApiClient();
export default apiClient;

// ============================================================================
// WebSocket URL Helpers
// ============================================================================

/**
 * Build WebSocket URL for match snapshot streaming.
 * @param containerId - The container ID
 * @param matchId - The match ID
 */
export function buildSnapshotWebSocketUrl(
  containerId: number,
  matchId: number,
): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws/containers/${containerId}/matches/${matchId}/snapshot`;
}

/**
 * Build WebSocket URL for player-scoped snapshot streaming.
 * @param containerId - The container ID
 * @param matchId - The match ID
 * @param playerId - The player ID
 */
export function buildPlayerSnapshotWebSocketUrl(
  containerId: number,
  matchId: number,
  playerId: number,
): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws/containers/${containerId}/matches/${matchId}/players/${playerId}/snapshot`;
}

/**
 * Build WebSocket URL for player-scoped delta snapshot streaming.
 * @param containerId - The container ID
 * @param matchId - The match ID
 * @param playerId - The player ID
 */
export function buildPlayerDeltaSnapshotWebSocketUrl(
  containerId: number,
  matchId: number,
  playerId: number,
): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws/containers/${containerId}/matches/${matchId}/players/${playerId}/delta`;
}

/**
 * Build WebSocket URL for player-scoped error streaming.
 */
export function buildPlayerErrorWebSocketUrl(
  matchId: number,
  playerId: number,
): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws/matches/${matchId}/players/${playerId}/errors`;
}
