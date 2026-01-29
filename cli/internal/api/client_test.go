// Package api provides the HTTP client for the Lightning Control Plane API.
package api

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestNewClient(t *testing.T) {
	client := NewClient("http://localhost:8081", "test-token")

	if client.baseURL != "http://localhost:8081" {
		t.Errorf("expected baseURL http://localhost:8081, got %s", client.baseURL)
	}
	if client.authToken != "test-token" {
		t.Errorf("expected authToken test-token, got %s", client.authToken)
	}
	if client.httpClient == nil {
		t.Error("expected httpClient to be initialized")
	}
	if client.httpClient.Timeout != 30*time.Second {
		t.Errorf("expected timeout 30s, got %v", client.httpClient.Timeout)
	}
}

func TestDeploy(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("expected POST, got %s", r.Method)
		}
		if r.URL.Path != "/api/v1/deploy" {
			t.Errorf("expected path /api/v1/deploy, got %s", r.URL.Path)
		}
		if r.Header.Get("Content-Type") != "application/json" {
			t.Errorf("expected Content-Type application/json, got %s", r.Header.Get("Content-Type"))
		}
		if r.Header.Get("X-Control-Plane-Token") != "test-token" {
			t.Errorf("expected X-Control-Plane-Token test-token, got %s", r.Header.Get("X-Control-Plane-Token"))
		}

		var req DeployRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			t.Fatalf("failed to decode request: %v", err)
		}
		if len(req.Modules) != 2 {
			t.Errorf("expected 2 modules, got %d", len(req.Modules))
		}

		resp := DeployResponse{
			MatchID:     "match-123",
			NodeID:      "node-1",
			ContainerID: 42,
			Status:      "RUNNING",
			CreatedAt:   time.Now(),
			Modules:     req.Modules,
			Endpoints: Endpoints{
				HTTP:      "http://localhost:8080/api/containers/42",
				WebSocket: "ws://localhost:8080/ws/containers/42/snapshots/match-123",
				Commands:  "ws://localhost:8080/containers/42/commands",
			},
		}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client := NewClient(server.URL, "test-token")
	autoStart := true
	resp, err := client.Deploy(&DeployRequest{
		Modules:   []string{"entity-module", "health-module"},
		AutoStart: &autoStart,
	})

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.MatchID != "match-123" {
		t.Errorf("expected matchId match-123, got %s", resp.MatchID)
	}
	if resp.NodeID != "node-1" {
		t.Errorf("expected nodeId node-1, got %s", resp.NodeID)
	}
	if resp.ContainerID != 42 {
		t.Errorf("expected containerId 42, got %d", resp.ContainerID)
	}
}

func TestGetDeployment(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			t.Errorf("expected GET, got %s", r.Method)
		}
		if r.URL.Path != "/api/v1/deploy/match-123" {
			t.Errorf("expected path /api/v1/deploy/match-123, got %s", r.URL.Path)
		}

		resp := DeployResponse{
			MatchID:     "match-123",
			NodeID:      "node-1",
			ContainerID: 42,
			Status:      "RUNNING",
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	resp, err := client.GetDeployment("match-123")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.MatchID != "match-123" {
		t.Errorf("expected matchId match-123, got %s", resp.MatchID)
	}
}

func TestUndeploy(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodDelete {
			t.Errorf("expected DELETE, got %s", r.Method)
		}
		if r.URL.Path != "/api/v1/deploy/match-123" {
			t.Errorf("expected path /api/v1/deploy/match-123, got %s", r.URL.Path)
		}

		w.WriteHeader(http.StatusNoContent)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	err := client.Undeploy("match-123")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestGetClusterStatus(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/cluster/status" {
			t.Errorf("expected path /api/cluster/status, got %s", r.URL.Path)
		}

		resp := ClusterStatus{
			TotalNodes:        5,
			HealthyNodes:      4,
			DrainingNodes:     1,
			TotalCapacity:     100,
			UsedCapacity:      45,
			AverageSaturation: 0.45,
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	resp, err := client.GetClusterStatus()

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.TotalNodes != 5 {
		t.Errorf("expected totalNodes 5, got %d", resp.TotalNodes)
	}
	if resp.HealthyNodes != 4 {
		t.Errorf("expected healthyNodes 4, got %d", resp.HealthyNodes)
	}
}

func TestListNodes(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/cluster/nodes" {
			t.Errorf("expected path /api/cluster/nodes, got %s", r.URL.Path)
		}

		nodes := []Node{
			{
				NodeID:           "node-1",
				AdvertiseAddress: "http://localhost:8080",
				Status:           "HEALTHY",
			},
			{
				NodeID:           "node-2",
				AdvertiseAddress: "http://localhost:8081",
				Status:           "DRAINING",
			},
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(nodes)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	nodes, err := client.ListNodes()

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(nodes) != 2 {
		t.Errorf("expected 2 nodes, got %d", len(nodes))
	}
	if nodes[0].NodeID != "node-1" {
		t.Errorf("expected nodeId node-1, got %s", nodes[0].NodeID)
	}
}

func TestListMatches(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/matches" {
			t.Errorf("expected path /api/matches, got %s", r.URL.Path)
		}

		matches := []Match{
			{
				MatchID:     "match-1",
				NodeID:      "node-1",
				ContainerID: 1,
				Status:      "RUNNING",
			},
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(matches)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	matches, err := client.ListMatches("")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(matches) != 1 {
		t.Errorf("expected 1 match, got %d", len(matches))
	}
}

func TestListMatchesWithStatusFilter(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/matches" {
			t.Errorf("expected path /api/matches, got %s", r.URL.Path)
		}
		if r.URL.Query().Get("status") != "RUNNING" {
			t.Errorf("expected status query param RUNNING, got %s", r.URL.Query().Get("status"))
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode([]Match{})
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	_, err := client.ListMatches("RUNNING")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestListModules(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/modules" {
			t.Errorf("expected path /api/modules, got %s", r.URL.Path)
		}

		modules := []Module{
			{
				Name:        "entity-module",
				Version:     "1.0.0",
				Description: "Entity management module",
			},
			{
				Name:        "health-module",
				Version:     "1.0.0",
				Description: "Health and damage module",
			},
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(modules)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	modules, err := client.ListModules()

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(modules) != 2 {
		t.Errorf("expected 2 modules, got %d", len(modules))
	}
}

func TestAPIError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(APIError{
			Code:      "NOT_FOUND",
			Message:   "Match not found",
			Timestamp: time.Now(),
		})
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	_, err := client.GetDeployment("not-exists")

	if err == nil {
		t.Fatal("expected error, got nil")
	}

	apiErr, ok := err.(*APIError)
	if !ok {
		t.Fatalf("expected *APIError, got %T", err)
	}
	if apiErr.Code != "NOT_FOUND" {
		t.Errorf("expected code NOT_FOUND, got %s", apiErr.Code)
	}
}

func TestAPIErrorString(t *testing.T) {
	apiErr := &APIError{
		Code:    "TEST_ERROR",
		Message: "Test message",
	}

	expected := "TEST_ERROR: Test message"
	if apiErr.Error() != expected {
		t.Errorf("expected %s, got %s", expected, apiErr.Error())
	}
}

func TestHTTPError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Internal Server Error"))
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	_, err := client.GetClusterStatus()

	if err == nil {
		t.Fatal("expected error, got nil")
	}
}

func TestFinishMatch(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("expected POST, got %s", r.Method)
		}
		if r.URL.Path != "/api/matches/match-123/finish" {
			t.Errorf("expected path /api/matches/match-123/finish, got %s", r.URL.Path)
		}

		resp := Match{
			MatchID: "match-123",
			Status:  "FINISHED",
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	match, err := client.FinishMatch("match-123")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if match.Status != "FINISHED" {
		t.Errorf("expected status FINISHED, got %s", match.Status)
	}
}

func TestDeleteMatch(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodDelete {
			t.Errorf("expected DELETE, got %s", r.Method)
		}
		if r.URL.Path != "/api/matches/match-123" {
			t.Errorf("expected path /api/matches/match-123, got %s", r.URL.Path)
		}
		w.WriteHeader(http.StatusNoContent)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	err := client.DeleteMatch("match-123")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestDistributeModule(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("expected POST, got %s", r.Method)
		}
		if r.URL.Path != "/api/modules/entity-module/1.0.0/distribute" {
			t.Errorf("expected path /api/modules/entity-module/1.0.0/distribute, got %s", r.URL.Path)
		}

		resp := DistributeResult{
			ModuleName:    "entity-module",
			ModuleVersion: "1.0.0",
			NodesUpdated:  5,
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client := NewClient(server.URL, "")
	result, err := client.DistributeModule("entity-module", "1.0.0")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if result.NodesUpdated != 5 {
		t.Errorf("expected nodesUpdated 5, got %d", result.NodesUpdated)
	}
}
