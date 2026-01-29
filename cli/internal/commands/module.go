package commands

import (
	"fmt"

	"github.com/ireland-samantha/lightning-engine/cli/internal/output"
	"github.com/spf13/cobra"
)

var moduleCmd = &cobra.Command{
	Use:   "module",
	Short: "Module registry commands",
	Long: `Manage modules in the Lightning Engine registry.

Commands:
  list       - List all modules
  versions   - List versions of a module
  distribute - Distribute a module to all nodes`,
}

// List subcommand
var moduleListCmd = &cobra.Command{
	Use:   "list",
	Short: "List all modules in the registry",
	Long: `List all modules registered with the control plane.

Examples:
  lightning module list
  lightning module list -o json`,
	RunE: runModuleList,
}

func init() {
	moduleCmd.AddCommand(moduleListCmd)
}

func runModuleList(cmd *cobra.Command, args []string) error {
	modules, err := apiClient.ListModules()
	if err != nil {
		out.PrintError(err)
		return err
	}

	rows := make([]output.ModuleRow, 0, len(modules))
	for _, m := range modules {
		rows = append(rows, output.ModuleRow{
			Name:        m.Name,
			Version:     m.Version,
			Description: m.Description,
		})
	}

	out.PrintModules(rows)
	return nil
}

// Versions subcommand
var moduleVersionsCmd = &cobra.Command{
	Use:   "versions <module-name>",
	Short: "List all versions of a module",
	Long: `List all available versions of a specific module.

Examples:
  lightning module versions EntityModule
  lightning module versions EntityModule -o json`,
	Args: cobra.ExactArgs(1),
	RunE: runModuleVersions,
}

func init() {
	moduleCmd.AddCommand(moduleVersionsCmd)
}

func runModuleVersions(cmd *cobra.Command, args []string) error {
	moduleName := args[0]

	module, err := apiClient.GetModuleVersions(moduleName)
	if err != nil {
		out.PrintError(err)
		return err
	}

	rows := make([]output.ModuleRow, 0, len(module.Versions))
	for _, v := range module.Versions {
		rows = append(rows, output.ModuleRow{
			Name:        module.Name,
			Version:     v,
			Description: module.Description,
		})
	}

	out.PrintModules(rows)
	return nil
}

// Distribute subcommand
var moduleDistributeCmd = &cobra.Command{
	Use:   "distribute <module-name> <version>",
	Short: "Distribute a module to all nodes",
	Long: `Distribute a module version to all healthy nodes in the cluster.

Examples:
  lightning module distribute EntityModule 1.0.0`,
	Args: cobra.ExactArgs(2),
	RunE: runModuleDistribute,
}

func init() {
	moduleCmd.AddCommand(moduleDistributeCmd)
}

func runModuleDistribute(cmd *cobra.Command, args []string) error {
	moduleName := args[0]
	version := args[1]

	result, err := apiClient.DistributeModule(moduleName, version)
	if err != nil {
		out.PrintError(err)
		return err
	}

	out.PrintSuccess(fmt.Sprintf("Module %s@%s distributed to %d nodes", result.ModuleName, result.ModuleVersion, result.NodesUpdated))
	return nil
}
