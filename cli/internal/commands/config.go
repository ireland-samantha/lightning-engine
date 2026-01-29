package commands

import (
	"fmt"

	"github.com/ireland-samantha/lightning-engine/cli/internal/config"
	"github.com/spf13/cobra"
)

var configCmd = &cobra.Command{
	Use:   "config",
	Short: "Configuration management",
	Long: `Manage CLI configuration settings.

Commands:
  get - Get a configuration value
  set - Set a configuration value`,
}

// Get subcommand
var configGetCmd = &cobra.Command{
	Use:   "get <key>",
	Short: "Get a configuration value",
	Long: `Get the current value of a configuration key.

Available keys:
  control_plane_url - Control Plane URL
  auth_token        - Authentication token
  output_format     - Default output format

Examples:
  lightning config get control_plane_url
  lightning config get output_format`,
	Args: cobra.ExactArgs(1),
	RunE: runConfigGet,
}

func init() {
	configCmd.AddCommand(configGetCmd)
}

func runConfigGet(cmd *cobra.Command, args []string) error {
	key := args[0]

	var value string
	switch key {
	case "control_plane_url":
		value = config.GetControlPlaneURL()
	case "auth_token":
		token := config.GetAuthToken()
		if token != "" {
			value = "********" // Don't show full token
		} else {
			value = "(not set)"
		}
	case "output_format":
		value = config.GetOutputFormat()
	default:
		return fmt.Errorf("unknown configuration key: %s", key)
	}

	fmt.Println(value)
	return nil
}

// Set subcommand
var configSetCmd = &cobra.Command{
	Use:   "set <key> <value>",
	Short: "Set a configuration value",
	Long: `Set a configuration value and save to ~/.lightning.yaml.

Available keys:
  control_plane_url - Control Plane URL
  auth_token        - Authentication token
  output_format     - Default output format (table, json, yaml, quiet)

Examples:
  lightning config set control_plane_url http://localhost:8081
  lightning config set output_format json
  lightning config set auth_token my-secret-token`,
	Args: cobra.ExactArgs(2),
	RunE: runConfigSet,
}

func init() {
	configCmd.AddCommand(configSetCmd)
}

func runConfigSet(cmd *cobra.Command, args []string) error {
	key := args[0]
	value := args[1]

	// Load current config
	cfg, err := config.Load()
	if err != nil {
		return err
	}

	switch key {
	case "control_plane_url":
		cfg.ControlPlaneURL = value
	case "auth_token":
		cfg.AuthToken = value
	case "output_format":
		// Validate format
		switch value {
		case "table", "json", "yaml", "quiet":
			cfg.OutputFormat = value
		default:
			return fmt.Errorf("invalid output format: %s (valid: table, json, yaml, quiet)", value)
		}
	default:
		return fmt.Errorf("unknown configuration key: %s", key)
	}

	if err := config.Save(cfg); err != nil {
		return fmt.Errorf("failed to save config: %w", err)
	}

	out.PrintSuccess(fmt.Sprintf("Set %s = %s", key, value))
	return nil
}
