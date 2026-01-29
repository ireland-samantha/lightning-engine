package commands

import (
	"fmt"
	"runtime"

	"github.com/spf13/cobra"
)

// Version information - set at build time via ldflags
var (
	Version   = "dev"
	GitCommit = "unknown"
	BuildDate = "unknown"
)

var versionCmd = &cobra.Command{
	Use:   "version",
	Short: "Print version information",
	Long:  `Print the version, git commit, and build date of the Lightning CLI.`,
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Printf("Lightning CLI %s\n", Version)
		fmt.Printf("  Git Commit: %s\n", GitCommit)
		fmt.Printf("  Build Date: %s\n", BuildDate)
		fmt.Printf("  Go Version: %s\n", runtime.Version())
		fmt.Printf("  OS/Arch:    %s/%s\n", runtime.GOOS, runtime.GOARCH)
	},
}
