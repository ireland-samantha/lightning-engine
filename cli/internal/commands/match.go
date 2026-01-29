package commands

import (
	"fmt"
	"strings"

	"github.com/ireland-samantha/lightning-engine/cli/internal/output"
	"github.com/spf13/cobra"
)

var (
	matchStatusFilter string
)

var matchCmd = &cobra.Command{
	Use:   "match",
	Short: "Match management commands",
	Long: `Manage matches in the Lightning Engine cluster.

Commands:
  list   - List all matches
  get    - Get match details
  finish - Mark a match as finished
  delete - Delete a match`,
}

// List subcommand
var matchListCmd = &cobra.Command{
	Use:   "list",
	Short: "List all matches in the cluster",
	Long: `List all matches in the cluster, optionally filtered by status.

Examples:
  lightning match list
  lightning match list --status RUNNING
  lightning match list -o json`,
	RunE: runMatchList,
}

func init() {
	matchListCmd.Flags().StringVarP(&matchStatusFilter, "status", "s", "", "Filter by status (RUNNING, FINISHED, ERROR)")
	matchCmd.AddCommand(matchListCmd)
}

func runMatchList(cmd *cobra.Command, args []string) error {
	matches, err := apiClient.ListMatches(matchStatusFilter)
	if err != nil {
		out.PrintError(err)
		return err
	}

	rows := make([]output.MatchRow, 0, len(matches))
	for _, m := range matches {
		rows = append(rows, output.MatchRow{
			MatchID:     m.MatchID,
			NodeID:      m.NodeID,
			Status:      m.Status,
			PlayerCount: m.PlayerCount,
			Modules:     strings.Join(m.ModuleNames, ", "),
		})
	}

	out.PrintMatches(rows)
	return nil
}

// Get subcommand
var matchGetCmd = &cobra.Command{
	Use:   "get <match-id>",
	Short: "Get details for a specific match",
	Long: `Get detailed information about a specific match.

Examples:
  lightning match get node-1-42-7
  lightning match get node-1-42-7 -o json`,
	Args: cobra.ExactArgs(1),
	RunE: runMatchGet,
}

func init() {
	matchCmd.AddCommand(matchGetCmd)
}

func runMatchGet(cmd *cobra.Command, args []string) error {
	matchID := args[0]

	match, err := apiClient.GetMatch(matchID)
	if err != nil {
		out.PrintError(err)
		return err
	}

	rows := []output.MatchRow{
		{
			MatchID:     match.MatchID,
			NodeID:      match.NodeID,
			Status:      match.Status,
			PlayerCount: match.PlayerCount,
			Modules:     strings.Join(match.ModuleNames, ", "),
		},
	}

	out.PrintMatches(rows)
	return nil
}

// Finish subcommand
var matchFinishCmd = &cobra.Command{
	Use:   "finish <match-id>",
	Short: "Mark a match as finished",
	Long: `Mark a match as finished. Finished matches remain in the registry but are no longer active.

Examples:
  lightning match finish node-1-42-7`,
	Args: cobra.ExactArgs(1),
	RunE: runMatchFinish,
}

func init() {
	matchCmd.AddCommand(matchFinishCmd)
}

func runMatchFinish(cmd *cobra.Command, args []string) error {
	matchID := args[0]

	match, err := apiClient.FinishMatch(matchID)
	if err != nil {
		out.PrintError(err)
		return err
	}

	out.PrintSuccess(fmt.Sprintf("Match %s finished (status: %s)", match.MatchID, match.Status))
	return nil
}

// Delete subcommand
var matchDeleteCmd = &cobra.Command{
	Use:   "delete <match-id>",
	Short: "Delete a match from the cluster",
	Long: `Delete a match from the cluster. This also removes the match from the hosting node.

Examples:
  lightning match delete node-1-42-7`,
	Args: cobra.ExactArgs(1),
	RunE: runMatchDelete,
}

func init() {
	matchCmd.AddCommand(matchDeleteCmd)
}

func runMatchDelete(cmd *cobra.Command, args []string) error {
	matchID := args[0]

	if err := apiClient.DeleteMatch(matchID); err != nil {
		out.PrintError(err)
		return err
	}

	out.PrintSuccess(fmt.Sprintf("Match %s deleted", matchID))
	return nil
}
