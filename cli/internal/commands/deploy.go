package commands

import (
	"fmt"

	"github.com/ireland-samantha/lightning-engine/cli/internal/api"
	"github.com/spf13/cobra"
)

var (
	deployModules       []string
	deployPreferredNode string
	deployAutoStart     bool
)

var deployCmd = &cobra.Command{
	Use:   "deploy",
	Short: "Deploy a game to the cluster",
	Long: `Deploy a new game match to the Lightning Engine cluster.

The control plane will select the best available node based on load and capacity,
create a container, and start the match with the specified modules.

Examples:
  # Deploy with specific modules
  lightning deploy --modules EntityModule,HealthModule,GridMapModule

  # Deploy to a specific node
  lightning deploy --modules EntityModule --node node-1

  # Deploy and get JSON output
  lightning deploy --modules EntityModule -o json`,
	RunE: runDeploy,
}

func init() {
	deployCmd.Flags().StringSliceVarP(&deployModules, "modules", "m", nil, "Modules to enable (required)")
	deployCmd.Flags().StringVarP(&deployPreferredNode, "node", "n", "", "Preferred node ID")
	deployCmd.Flags().BoolVar(&deployAutoStart, "auto-start", true, "Auto-start the container")
	deployCmd.MarkFlagRequired("modules")
}

func runDeploy(cmd *cobra.Command, args []string) error {
	req := &api.DeployRequest{
		Modules:   deployModules,
		AutoStart: &deployAutoStart,
	}

	if deployPreferredNode != "" {
		req.PreferredNodeID = &deployPreferredNode
	}

	resp, err := apiClient.Deploy(req)
	if err != nil {
		out.PrintError(err)
		return err
	}

	endpoints := map[string]string{
		"http":      resp.Endpoints.HTTP,
		"websocket": resp.Endpoints.WebSocket,
		"commands":  resp.Endpoints.Commands,
	}

	out.PrintDeployment(resp.MatchID, resp.NodeID, resp.ContainerID, resp.Status, endpoints)

	return nil
}

// Undeploy subcommand
var undeployCmd = &cobra.Command{
	Use:   "undeploy <match-id>",
	Short: "Remove a deployed game from the cluster",
	Long: `Remove a deployed game match from the cluster.

This will stop the container and delete the match.

Examples:
  lightning undeploy node-1-42-7`,
	Args: cobra.ExactArgs(1),
	RunE: runUndeploy,
}

func init() {
	deployCmd.AddCommand(undeployCmd)
}

func runUndeploy(cmd *cobra.Command, args []string) error {
	matchID := args[0]

	if err := apiClient.Undeploy(matchID); err != nil {
		out.PrintError(err)
		return err
	}

	out.PrintSuccess(fmt.Sprintf("Match %s undeployed", matchID))
	return nil
}

// Status subcommand
var deployStatusCmd = &cobra.Command{
	Use:   "status <match-id>",
	Short: "Get deployment status",
	Long: `Get the status of a deployed game match.

Examples:
  lightning deploy status node-1-42-7
  lightning deploy status node-1-42-7 -o json`,
	Args: cobra.ExactArgs(1),
	RunE: runDeployStatus,
}

func init() {
	deployCmd.AddCommand(deployStatusCmd)
}

func runDeployStatus(cmd *cobra.Command, args []string) error {
	matchID := args[0]

	resp, err := apiClient.GetDeployment(matchID)
	if err != nil {
		out.PrintError(err)
		return err
	}

	endpoints := map[string]string{
		"http":      resp.Endpoints.HTTP,
		"websocket": resp.Endpoints.WebSocket,
		"commands":  resp.Endpoints.Commands,
	}

	out.PrintDeployment(resp.MatchID, resp.NodeID, resp.ContainerID, resp.Status, endpoints)

	return nil
}
