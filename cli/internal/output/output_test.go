// Package output provides output formatting for the Lightning CLI.
package output

import (
	"bytes"
	"encoding/json"
	"strings"
	"testing"

	"gopkg.in/yaml.v3"
)

func TestNewWriter(t *testing.T) {
	tests := []struct {
		input    string
		expected Format
	}{
		{"table", FormatTable},
		{"TABLE", FormatTable},
		{"json", FormatJSON},
		{"JSON", FormatJSON},
		{"yaml", FormatYAML},
		{"YAML", FormatYAML},
		{"quiet", FormatQuiet},
		{"QUIET", FormatQuiet},
		{"invalid", FormatTable}, // default
		{"", FormatTable},        // default
	}

	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			w := NewWriter(tt.input)
			if w.format != tt.expected {
				t.Errorf("expected format %s, got %s", tt.expected, w.format)
			}
		})
	}
}

func TestPrintClusterStatus_Table(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatTable, out: &buf}

	w.PrintClusterStatus(10, 8, 2, 100, 45, 0.45)

	output := buf.String()
	if !strings.Contains(output, "10 total") {
		t.Errorf("expected '10 total' in output, got %s", output)
	}
	if !strings.Contains(output, "8 healthy") {
		t.Errorf("expected '8 healthy' in output, got %s", output)
	}
	if !strings.Contains(output, "2 draining") {
		t.Errorf("expected '2 draining' in output, got %s", output)
	}
	if !strings.Contains(output, "45/100") {
		t.Errorf("expected '45/100' in output, got %s", output)
	}
	if !strings.Contains(output, "45%") {
		t.Errorf("expected '45%%' in output, got %s", output)
	}
}

func TestPrintClusterStatus_JSON(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatJSON, out: &buf}

	w.PrintClusterStatus(10, 8, 2, 100, 45, 0.45)

	var result map[string]interface{}
	if err := json.Unmarshal(buf.Bytes(), &result); err != nil {
		t.Fatalf("failed to parse JSON: %v", err)
	}

	if result["totalNodes"].(float64) != 10 {
		t.Errorf("expected totalNodes 10, got %v", result["totalNodes"])
	}
	if result["healthyNodes"].(float64) != 8 {
		t.Errorf("expected healthyNodes 8, got %v", result["healthyNodes"])
	}
}

func TestPrintClusterStatus_YAML(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatYAML, out: &buf}

	w.PrintClusterStatus(10, 8, 2, 100, 45, 0.45)

	var result map[string]interface{}
	if err := yaml.Unmarshal(buf.Bytes(), &result); err != nil {
		t.Fatalf("failed to parse YAML: %v", err)
	}

	if result["totalNodes"].(int) != 10 {
		t.Errorf("expected totalNodes 10, got %v", result["totalNodes"])
	}
}

func TestPrintClusterStatus_Quiet(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatQuiet, out: &buf}

	w.PrintClusterStatus(10, 8, 2, 100, 45, 0.45)

	output := buf.String()
	if !strings.Contains(output, "8/10 healthy 45%") {
		t.Errorf("expected '8/10 healthy 45%%' in output, got %s", output)
	}
}

func TestPrintNodes_Table(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatTable, out: &buf}

	nodes := []NodeRow{
		{
			NodeID:     "node-1",
			Status:     "HEALTHY",
			Address:    "localhost:8080",
			Containers: 5,
			Matches:    3,
			CPUPercent: 45.5,
			Memory:     "512/1024MB",
		},
	}

	w.PrintNodes(nodes)

	output := buf.String()
	if !strings.Contains(output, "NODE ID") {
		t.Errorf("expected header 'NODE ID' in output, got %s", output)
	}
	if !strings.Contains(output, "node-1") {
		t.Errorf("expected 'node-1' in output, got %s", output)
	}
	if !strings.Contains(output, "HEALTHY") {
		t.Errorf("expected 'HEALTHY' in output, got %s", output)
	}
}

func TestPrintNodes_Quiet(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatQuiet, out: &buf}

	nodes := []NodeRow{
		{NodeID: "node-1"},
		{NodeID: "node-2"},
	}

	w.PrintNodes(nodes)

	output := buf.String()
	lines := strings.Split(strings.TrimSpace(output), "\n")
	if len(lines) != 2 {
		t.Errorf("expected 2 lines, got %d", len(lines))
	}
	if lines[0] != "node-1" {
		t.Errorf("expected 'node-1', got %s", lines[0])
	}
}

func TestPrintNodes_JSON(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatJSON, out: &buf}

	nodes := []NodeRow{
		{NodeID: "node-1", Status: "HEALTHY"},
	}

	w.PrintNodes(nodes)

	var result []NodeRow
	if err := json.Unmarshal(buf.Bytes(), &result); err != nil {
		t.Fatalf("failed to parse JSON: %v", err)
	}

	if len(result) != 1 {
		t.Errorf("expected 1 node, got %d", len(result))
	}
	if result[0].NodeID != "node-1" {
		t.Errorf("expected nodeId 'node-1', got %s", result[0].NodeID)
	}
}

func TestPrintMatches_Table(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatTable, out: &buf}

	matches := []MatchRow{
		{
			MatchID:     "match-1",
			NodeID:      "node-1",
			Status:      "RUNNING",
			PlayerCount: 4,
			Modules:     "entity,health",
		},
	}

	w.PrintMatches(matches)

	output := buf.String()
	if !strings.Contains(output, "MATCH ID") {
		t.Errorf("expected header 'MATCH ID' in output, got %s", output)
	}
	if !strings.Contains(output, "match-1") {
		t.Errorf("expected 'match-1' in output, got %s", output)
	}
}

func TestPrintMatches_Quiet(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatQuiet, out: &buf}

	matches := []MatchRow{
		{MatchID: "match-1"},
		{MatchID: "match-2"},
	}

	w.PrintMatches(matches)

	output := buf.String()
	lines := strings.Split(strings.TrimSpace(output), "\n")
	if len(lines) != 2 {
		t.Errorf("expected 2 lines, got %d", len(lines))
	}
}

func TestPrintModules_Table(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatTable, out: &buf}

	modules := []ModuleRow{
		{
			Name:        "entity-module",
			Version:     "1.0.0",
			Description: "Entity management",
		},
	}

	w.PrintModules(modules)

	output := buf.String()
	if !strings.Contains(output, "NAME") {
		t.Errorf("expected header 'NAME' in output, got %s", output)
	}
	if !strings.Contains(output, "entity-module") {
		t.Errorf("expected 'entity-module' in output, got %s", output)
	}
}

func TestPrintModules_Quiet(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatQuiet, out: &buf}

	modules := []ModuleRow{
		{Name: "entity-module"},
		{Name: "health-module"},
	}

	w.PrintModules(modules)

	output := buf.String()
	lines := strings.Split(strings.TrimSpace(output), "\n")
	if len(lines) != 2 {
		t.Errorf("expected 2 lines, got %d", len(lines))
	}
}

func TestPrintDeployment_Table(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatTable, out: &buf}

	endpoints := map[string]string{
		"http":      "http://localhost:8080/api/containers/1",
		"websocket": "ws://localhost:8080/ws/snapshots/match-1",
		"commands":  "ws://localhost:8080/commands",
	}

	w.PrintDeployment("match-1", "node-1", 1, "RUNNING", endpoints)

	output := buf.String()
	if !strings.Contains(output, "match-1") {
		t.Errorf("expected 'match-1' in output, got %s", output)
	}
	if !strings.Contains(output, "node-1") {
		t.Errorf("expected 'node-1' in output, got %s", output)
	}
	if !strings.Contains(output, "ENDPOINTS:") {
		t.Errorf("expected 'ENDPOINTS:' in output, got %s", output)
	}
}

func TestPrintDeployment_Quiet(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatQuiet, out: &buf}

	w.PrintDeployment("match-1", "node-1", 1, "RUNNING", nil)

	output := strings.TrimSpace(buf.String())
	if output != "match-1" {
		t.Errorf("expected 'match-1', got %s", output)
	}
}

func TestPrintDeployment_JSON(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatJSON, out: &buf}

	endpoints := map[string]string{
		"http": "http://localhost:8080",
	}

	w.PrintDeployment("match-1", "node-1", 1, "RUNNING", endpoints)

	var result map[string]interface{}
	if err := json.Unmarshal(buf.Bytes(), &result); err != nil {
		t.Fatalf("failed to parse JSON: %v", err)
	}

	if result["matchId"].(string) != "match-1" {
		t.Errorf("expected matchId 'match-1', got %v", result["matchId"])
	}
}

func TestPrintMessage(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatTable, out: &buf}

	w.PrintMessage("Hello World")

	if buf.String() != "Hello World\n" {
		t.Errorf("expected 'Hello World\\n', got %q", buf.String())
	}
}

func TestPrintMessage_Quiet(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatQuiet, out: &buf}

	w.PrintMessage("Hello World")

	if buf.String() != "" {
		t.Errorf("expected empty string in quiet mode, got %q", buf.String())
	}
}

func TestPrintSuccess(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatTable, out: &buf}

	w.PrintSuccess("Done")

	if !strings.Contains(buf.String(), "✓ Done") {
		t.Errorf("expected '✓ Done', got %s", buf.String())
	}
}

func TestPrintSuccess_Quiet(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatQuiet, out: &buf}

	w.PrintSuccess("Done")

	if buf.String() != "" {
		t.Errorf("expected empty string in quiet mode, got %q", buf.String())
	}
}

func TestPrint_JSON(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatJSON, out: &buf}

	data := map[string]string{"key": "value"}
	w.Print(data)

	var result map[string]string
	if err := json.Unmarshal(buf.Bytes(), &result); err != nil {
		t.Fatalf("failed to parse JSON: %v", err)
	}

	if result["key"] != "value" {
		t.Errorf("expected key 'value', got %s", result["key"])
	}
}

func TestPrint_YAML(t *testing.T) {
	var buf bytes.Buffer
	w := &Writer{format: FormatYAML, out: &buf}

	data := map[string]string{"key": "value"}
	w.Print(data)

	var result map[string]string
	if err := yaml.Unmarshal(buf.Bytes(), &result); err != nil {
		t.Fatalf("failed to parse YAML: %v", err)
	}

	if result["key"] != "value" {
		t.Errorf("expected key 'value', got %s", result["key"])
	}
}
