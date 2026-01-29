package commands

import (
	"fmt"

	"github.com/ireland-samantha/lightning-engine/cli/internal/output"
	"github.com/spf13/cobra"
)

var clusterCmd = &cobra.Command{
	Use:   "cluster",
	Short: "Cluster management commands",
	Long: `Manage the Lightning Engine cluster.

Commands:
  status  - Show cluster health overview
  nodes   - List all nodes in the cluster`,
}

// Status subcommand
var clusterStatusCmd = &cobra.Command{
	Use:   "status",
	Short: "Show cluster health overview",
	Long: `Display the cluster health status including node count, capacity, and saturation.

Examples:
  lightning cluster status
  lightning cluster status -o json`,
	RunE: runClusterStatus,
}

func init() {
	clusterCmd.AddCommand(clusterStatusCmd)
}

func runClusterStatus(cmd *cobra.Command, args []string) error {
	status, err := apiClient.GetClusterStatus()
	if err != nil {
		out.PrintError(err)
		return err
	}

	out.PrintClusterStatus(
		status.TotalNodes,
		status.HealthyNodes,
		status.DrainingNodes,
		status.TotalCapacity,
		status.UsedCapacity,
		status.AverageSaturation,
	)

	return nil
}

// Nodes subcommand
var clusterNodesCmd = &cobra.Command{
	Use:   "nodes",
	Short: "List all nodes in the cluster",
	Long: `List all registered nodes in the cluster with their status and metrics.

Examples:
  lightning cluster nodes
  lightning cluster nodes -o json
  lightning cluster nodes -o quiet`,
	RunE: runClusterNodes,
}

func init() {
	clusterCmd.AddCommand(clusterNodesCmd)
}

func runClusterNodes(cmd *cobra.Command, args []string) error {
	nodes, err := apiClient.ListNodes()
	if err != nil {
		out.PrintError(err)
		return err
	}

	rows := make([]output.NodeRow, 0, len(nodes))
	for _, n := range nodes {
		row := output.NodeRow{
			NodeID:  n.NodeID,
			Status:  n.Status,
			Address: n.AdvertiseAddress,
		}

		if n.Metrics != nil {
			row.Containers = n.Metrics.ContainerCount
			row.Matches = n.Metrics.MatchCount
			row.CPUPercent = n.Metrics.CPUUsage
			row.Memory = fmt.Sprintf("%dMB/%dMB", n.Metrics.MemoryUsedMB, n.Metrics.MemoryMaxMB)
		} else {
			row.Memory = "-"
		}

		rows = append(rows, row)
	}

	out.PrintNodes(rows)
	return nil
}

// Node get subcommand
var clusterNodeGetCmd = &cobra.Command{
	Use:   "node <node-id>",
	Short: "Get details for a specific node",
	Long: `Get detailed information about a specific node in the cluster.

Examples:
  lightning cluster node node-1
  lightning cluster node node-1 -o json`,
	Args: cobra.ExactArgs(1),
	RunE: runClusterNodeGet,
}

func init() {
	clusterCmd.AddCommand(clusterNodeGetCmd)
}

func runClusterNodeGet(cmd *cobra.Command, args []string) error {
	nodeID := args[0]

	node, err := apiClient.GetNode(nodeID)
	if err != nil {
		out.PrintError(err)
		return err
	}

	row := output.NodeRow{
		NodeID:  node.NodeID,
		Status:  node.Status,
		Address: node.AdvertiseAddress,
	}

	if node.Metrics != nil {
		row.Containers = node.Metrics.ContainerCount
		row.Matches = node.Metrics.MatchCount
		row.CPUPercent = node.Metrics.CPUUsage
		row.Memory = fmt.Sprintf("%dMB/%dMB", node.Metrics.MemoryUsedMB, node.Metrics.MemoryMaxMB)
	} else {
		row.Memory = "-"
	}

	out.PrintNodes([]output.NodeRow{row})
	return nil
}
