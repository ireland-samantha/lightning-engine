// Package main is the entry point for the Lightning CLI.
package main

import (
	"fmt"
	"os"

	"github.com/ireland-samantha/lightning-engine/cli/internal/commands"
)

func main() {
	if err := commands.RootCmd.Execute(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
