// Package output provides output formatting for the Lightning CLI.
package output

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
	"strings"
	"text/tabwriter"

	"gopkg.in/yaml.v3"
)

// Format represents an output format.
type Format string

const (
	// FormatTable outputs data as a table.
	FormatTable Format = "table"
	// FormatJSON outputs data as JSON.
	FormatJSON Format = "json"
	// FormatYAML outputs data as YAML.
	FormatYAML Format = "yaml"
	// FormatQuiet outputs only essential info (IDs, etc.).
	FormatQuiet Format = "quiet"
)

// Writer handles output formatting.
type Writer struct {
	format Format
	out    io.Writer
}

// NewWriter creates a new output writer.
func NewWriter(format string) *Writer {
	f := Format(strings.ToLower(format))
	if f != FormatTable && f != FormatJSON && f != FormatYAML && f != FormatQuiet {
		f = FormatTable
	}
	return &Writer{
		format: f,
		out:    os.Stdout,
	}
}

// Print outputs data in the configured format.
func (w *Writer) Print(data interface{}) error {
	switch w.format {
	case FormatJSON:
		return w.printJSON(data)
	case FormatYAML:
		return w.printYAML(data)
	default:
		// Table and Quiet are handled by specific methods
		return w.printJSON(data)
	}
}

// PrintDeployment outputs deployment information.
func (w *Writer) PrintDeployment(matchID, nodeID string, containerId int64, status string, endpoints map[string]string) {
	switch w.format {
	case FormatQuiet:
		fmt.Fprintln(w.out, matchID)
	case FormatJSON:
		data := map[string]interface{}{
			"matchId":     matchID,
			"nodeId":      nodeID,
			"containerId": containerId,
			"status":      status,
			"endpoints":   endpoints,
		}
		w.printJSON(data)
	case FormatYAML:
		data := map[string]interface{}{
			"matchId":     matchID,
			"nodeId":      nodeID,
			"containerId": containerId,
			"status":      status,
			"endpoints":   endpoints,
		}
		w.printYAML(data)
	default:
		tw := tabwriter.NewWriter(w.out, 0, 0, 2, ' ', 0)
		fmt.Fprintf(tw, "MATCH ID\t%s\n", matchID)
		fmt.Fprintf(tw, "NODE ID\t%s\n", nodeID)
		fmt.Fprintf(tw, "CONTAINER ID\t%d\n", containerId)
		fmt.Fprintf(tw, "STATUS\t%s\n", status)
		fmt.Fprintf(tw, "\nENDPOINTS:\n")
		fmt.Fprintf(tw, "  HTTP\t%s\n", endpoints["http"])
		fmt.Fprintf(tw, "  WebSocket\t%s\n", endpoints["websocket"])
		fmt.Fprintf(tw, "  Commands\t%s\n", endpoints["commands"])
		tw.Flush()
	}
}

// PrintClusterStatus outputs cluster status information.
func (w *Writer) PrintClusterStatus(total, healthy, draining, capacity, used int, saturation float64) {
	switch w.format {
	case FormatQuiet:
		fmt.Fprintf(w.out, "%d/%d healthy %.0f%%\n", healthy, total, saturation*100)
	case FormatJSON:
		data := map[string]interface{}{
			"totalNodes":        total,
			"healthyNodes":      healthy,
			"drainingNodes":     draining,
			"totalCapacity":     capacity,
			"usedCapacity":      used,
			"averageSaturation": saturation,
		}
		w.printJSON(data)
	case FormatYAML:
		data := map[string]interface{}{
			"totalNodes":        total,
			"healthyNodes":      healthy,
			"drainingNodes":     draining,
			"totalCapacity":     capacity,
			"usedCapacity":      used,
			"averageSaturation": saturation,
		}
		w.printYAML(data)
	default:
		tw := tabwriter.NewWriter(w.out, 0, 0, 2, ' ', 0)
		fmt.Fprintf(tw, "NODES\t%d total, %d healthy, %d draining\n", total, healthy, draining)
		fmt.Fprintf(tw, "CAPACITY\t%d/%d (%.0f%% saturated)\n", used, capacity, saturation*100)
		tw.Flush()
	}
}

// PrintNodes outputs a list of nodes.
func (w *Writer) PrintNodes(nodes []NodeRow) {
	switch w.format {
	case FormatQuiet:
		for _, n := range nodes {
			fmt.Fprintln(w.out, n.NodeID)
		}
	case FormatJSON:
		w.printJSON(nodes)
	case FormatYAML:
		w.printYAML(nodes)
	default:
		tw := tabwriter.NewWriter(w.out, 0, 0, 2, ' ', 0)
		fmt.Fprintln(tw, "NODE ID\tSTATUS\tADDRESS\tCONTAINERS\tMATCHES\tCPU\tMEMORY")
		for _, n := range nodes {
			fmt.Fprintf(tw, "%s\t%s\t%s\t%d\t%d\t%.1f%%\t%s\n",
				n.NodeID, n.Status, n.Address, n.Containers, n.Matches, n.CPUPercent, n.Memory)
		}
		tw.Flush()
	}
}

// NodeRow represents a row in the nodes table.
type NodeRow struct {
	NodeID     string  `json:"nodeId" yaml:"nodeId"`
	Status     string  `json:"status" yaml:"status"`
	Address    string  `json:"address" yaml:"address"`
	Containers int     `json:"containers" yaml:"containers"`
	Matches    int     `json:"matches" yaml:"matches"`
	CPUPercent float64 `json:"cpuPercent" yaml:"cpuPercent"`
	Memory     string  `json:"memory" yaml:"memory"`
}

// PrintMatches outputs a list of matches.
func (w *Writer) PrintMatches(matches []MatchRow) {
	switch w.format {
	case FormatQuiet:
		for _, m := range matches {
			fmt.Fprintln(w.out, m.MatchID)
		}
	case FormatJSON:
		w.printJSON(matches)
	case FormatYAML:
		w.printYAML(matches)
	default:
		tw := tabwriter.NewWriter(w.out, 0, 0, 2, ' ', 0)
		fmt.Fprintln(tw, "MATCH ID\tNODE\tSTATUS\tPLAYERS\tMODULES")
		for _, m := range matches {
			fmt.Fprintf(tw, "%s\t%s\t%s\t%d\t%s\n",
				m.MatchID, m.NodeID, m.Status, m.PlayerCount, m.Modules)
		}
		tw.Flush()
	}
}

// MatchRow represents a row in the matches table.
type MatchRow struct {
	MatchID     string `json:"matchId" yaml:"matchId"`
	NodeID      string `json:"nodeId" yaml:"nodeId"`
	Status      string `json:"status" yaml:"status"`
	PlayerCount int    `json:"playerCount" yaml:"playerCount"`
	Modules     string `json:"modules" yaml:"modules"`
}

// PrintModules outputs a list of modules.
func (w *Writer) PrintModules(modules []ModuleRow) {
	switch w.format {
	case FormatQuiet:
		for _, m := range modules {
			fmt.Fprintln(w.out, m.Name)
		}
	case FormatJSON:
		w.printJSON(modules)
	case FormatYAML:
		w.printYAML(modules)
	default:
		tw := tabwriter.NewWriter(w.out, 0, 0, 2, ' ', 0)
		fmt.Fprintln(tw, "NAME\tVERSION\tDESCRIPTION")
		for _, m := range modules {
			fmt.Fprintf(tw, "%s\t%s\t%s\n", m.Name, m.Version, m.Description)
		}
		tw.Flush()
	}
}

// ModuleRow represents a row in the modules table.
type ModuleRow struct {
	Name        string `json:"name" yaml:"name"`
	Version     string `json:"version" yaml:"version"`
	Description string `json:"description" yaml:"description"`
}

// PrintMessage outputs a simple message.
func (w *Writer) PrintMessage(msg string) {
	if w.format == FormatQuiet {
		return
	}
	fmt.Fprintln(w.out, msg)
}

// PrintError outputs an error message.
func (w *Writer) PrintError(err error) {
	fmt.Fprintf(os.Stderr, "Error: %v\n", err)
}

// PrintSuccess outputs a success message.
func (w *Writer) PrintSuccess(msg string) {
	if w.format == FormatQuiet {
		return
	}
	fmt.Fprintf(w.out, "✓ %s\n", msg)
}

func (w *Writer) printJSON(data interface{}) error {
	encoder := json.NewEncoder(w.out)
	encoder.SetIndent("", "  ")
	return encoder.Encode(data)
}

func (w *Writer) printYAML(data interface{}) error {
	encoder := yaml.NewEncoder(w.out)
	encoder.SetIndent(2)
	return encoder.Encode(data)
}
