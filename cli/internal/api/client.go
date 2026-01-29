// Package api provides the HTTP client for the Lightning Control Plane API.
package api

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

// Client is the HTTP client for the Lightning Control Plane API.
type Client struct {
	baseURL    string
	authToken  string
	httpClient *http.Client
}

// NewClient creates a new API client.
func NewClient(baseURL, authToken string) *Client {
	return &Client{
		baseURL:   baseURL,
		authToken: authToken,
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

// DeployRequest is the request body for deploying a game.
type DeployRequest struct {
	Modules         []string `json:"modules"`
	PreferredNodeID *string  `json:"preferredNodeId,omitempty"`
	AutoStart       *bool    `json:"autoStart,omitempty"`
}

// DeployResponse is the response from a deploy operation.
type DeployResponse struct {
	MatchID     string    `json:"matchId"`
	NodeID      string    `json:"nodeId"`
	ContainerID int64     `json:"containerId"`
	Status      string    `json:"status"`
	CreatedAt   time.Time `json:"createdAt"`
	Modules     []string  `json:"modules"`
	Endpoints   Endpoints `json:"endpoints"`
}

// Endpoints contains connection information for a deployed game.
type Endpoints struct {
	HTTP      string `json:"http"`
	WebSocket string `json:"websocket"`
	Commands  string `json:"commands"`
}

// ClusterStatus represents the cluster health overview.
type ClusterStatus struct {
	TotalNodes        int     `json:"totalNodes"`
	HealthyNodes      int     `json:"healthyNodes"`
	DrainingNodes     int     `json:"drainingNodes"`
	TotalCapacity     int     `json:"totalCapacity"`
	UsedCapacity      int     `json:"usedCapacity"`
	AverageSaturation float64 `json:"averageSaturation"`
}

// Node represents a cluster node.
type Node struct {
	NodeID           string    `json:"nodeId"`
	AdvertiseAddress string    `json:"advertiseAddress"`
	Status           string    `json:"status"`
	Capacity         Capacity  `json:"capacity"`
	Metrics          *Metrics  `json:"metrics,omitempty"`
	RegisteredAt     time.Time `json:"registeredAt"`
	LastHeartbeat    time.Time `json:"lastHeartbeat"`
}

// Capacity represents node capacity.
type Capacity struct {
	MaxContainers int `json:"maxContainers"`
}

// Metrics represents node metrics.
type Metrics struct {
	ContainerCount int     `json:"containerCount"`
	MatchCount     int     `json:"matchCount"`
	CPUUsage       float64 `json:"cpuUsage"`
	MemoryUsedMB   int64   `json:"memoryUsedMb"`
	MemoryMaxMB    int64   `json:"memoryMaxMb"`
}

// Match represents a match in the cluster.
type Match struct {
	MatchID          string    `json:"matchId"`
	NodeID           string    `json:"nodeId"`
	ContainerID      int64     `json:"containerId"`
	Status           string    `json:"status"`
	CreatedAt        time.Time `json:"createdAt"`
	ModuleNames      []string  `json:"moduleNames"`
	AdvertiseAddress string    `json:"advertiseAddress"`
	WebSocketURL     string    `json:"websocketUrl"`
	PlayerCount      int       `json:"playerCount"`
}

// Module represents a module in the registry.
type Module struct {
	Name        string   `json:"name"`
	Version     string   `json:"version"`
	Description string   `json:"description"`
	Versions    []string `json:"versions,omitempty"`
}

// DistributeResult represents the result of a module distribution.
type DistributeResult struct {
	ModuleName    string `json:"moduleName"`
	ModuleVersion string `json:"moduleVersion"`
	NodesUpdated  int    `json:"nodesUpdated"`
}

// APIError represents an error response from the API.
type APIError struct {
	Code      string    `json:"code"`
	Message   string    `json:"message"`
	Timestamp time.Time `json:"timestamp"`
}

func (e *APIError) Error() string {
	return fmt.Sprintf("%s: %s", e.Code, e.Message)
}

// Deploy deploys a new game match to the cluster.
func (c *Client) Deploy(req *DeployRequest) (*DeployResponse, error) {
	var resp DeployResponse
	if err := c.post("/api/v1/deploy", req, &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// GetDeployment gets the status of a deployed match.
func (c *Client) GetDeployment(matchID string) (*DeployResponse, error) {
	var resp DeployResponse
	if err := c.get(fmt.Sprintf("/api/v1/deploy/%s", matchID), &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// Undeploy removes a deployed match from the cluster.
func (c *Client) Undeploy(matchID string) error {
	return c.delete(fmt.Sprintf("/api/v1/deploy/%s", matchID))
}

// GetClusterStatus gets the cluster health overview.
func (c *Client) GetClusterStatus() (*ClusterStatus, error) {
	var resp ClusterStatus
	if err := c.get("/api/cluster/status", &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// ListNodes lists all nodes in the cluster.
func (c *Client) ListNodes() ([]Node, error) {
	var resp []Node
	if err := c.get("/api/cluster/nodes", &resp); err != nil {
		return nil, err
	}
	return resp, nil
}

// GetNode gets a specific node by ID.
func (c *Client) GetNode(nodeID string) (*Node, error) {
	var resp Node
	if err := c.get(fmt.Sprintf("/api/cluster/nodes/%s", nodeID), &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// ListMatches lists all matches in the cluster.
func (c *Client) ListMatches(status string) ([]Match, error) {
	path := "/api/matches"
	if status != "" {
		path = fmt.Sprintf("%s?status=%s", path, status)
	}
	var resp []Match
	if err := c.get(path, &resp); err != nil {
		return nil, err
	}
	return resp, nil
}

// GetMatch gets a specific match by ID.
func (c *Client) GetMatch(matchID string) (*Match, error) {
	var resp Match
	if err := c.get(fmt.Sprintf("/api/matches/%s", matchID), &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// FinishMatch marks a match as finished.
func (c *Client) FinishMatch(matchID string) (*Match, error) {
	var resp Match
	if err := c.post(fmt.Sprintf("/api/matches/%s/finish", matchID), nil, &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// DeleteMatch deletes a match from the cluster.
func (c *Client) DeleteMatch(matchID string) error {
	return c.delete(fmt.Sprintf("/api/matches/%s", matchID))
}

// ListModules lists all modules in the registry.
func (c *Client) ListModules() ([]Module, error) {
	var resp []Module
	if err := c.get("/api/modules", &resp); err != nil {
		return nil, err
	}
	return resp, nil
}

// GetModuleVersions gets all versions of a module.
func (c *Client) GetModuleVersions(name string) (*Module, error) {
	var resp Module
	if err := c.get(fmt.Sprintf("/api/modules/%s", name), &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// DistributeModule distributes a module to all nodes.
func (c *Client) DistributeModule(name, version string) (*DistributeResult, error) {
	var resp DistributeResult
	if err := c.post(fmt.Sprintf("/api/modules/%s/%s/distribute", name, version), nil, &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// Helper methods for HTTP operations

func (c *Client) get(path string, result interface{}) error {
	req, err := http.NewRequest(http.MethodGet, c.baseURL+path, nil)
	if err != nil {
		return fmt.Errorf("creating request: %w", err)
	}
	return c.doRequest(req, result)
}

func (c *Client) post(path string, body, result interface{}) error {
	var bodyReader io.Reader
	if body != nil {
		data, err := json.Marshal(body)
		if err != nil {
			return fmt.Errorf("marshaling request body: %w", err)
		}
		bodyReader = bytes.NewReader(data)
	}

	req, err := http.NewRequest(http.MethodPost, c.baseURL+path, bodyReader)
	if err != nil {
		return fmt.Errorf("creating request: %w", err)
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	return c.doRequest(req, result)
}

func (c *Client) delete(path string) error {
	req, err := http.NewRequest(http.MethodDelete, c.baseURL+path, nil)
	if err != nil {
		return fmt.Errorf("creating request: %w", err)
	}
	return c.doRequest(req, nil)
}

func (c *Client) doRequest(req *http.Request, result interface{}) error {
	req.Header.Set("Accept", "application/json")
	if c.authToken != "" {
		req.Header.Set("X-Control-Plane-Token", c.authToken)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("making request: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("reading response: %w", err)
	}

	if resp.StatusCode >= 400 {
		var apiErr APIError
		if err := json.Unmarshal(body, &apiErr); err != nil {
			return fmt.Errorf("HTTP %d: %s", resp.StatusCode, string(body))
		}
		return &apiErr
	}

	if result != nil && len(body) > 0 {
		if err := json.Unmarshal(body, result); err != nil {
			return fmt.Errorf("parsing response: %w", err)
		}
	}

	return nil
}
