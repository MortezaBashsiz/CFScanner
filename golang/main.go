package main

import (
	"github.com/spf13/cobra"
	"os"
	"runtime"
	"strings"
)

// Program Info
var (
	version  = "1.5"
	build    = "Custom"
	codename = "CFScanner , CloudFlare Scanner."
)

func Version() string {
	return version
}

// VersionStatement returns a list of strings representing the full version info.
func VersionStatement() string {
	return strings.Join([]string{
		"CFScanner ", Version(), " (", codename, ") ", build, " (", runtime.Version(), " ", runtime.GOOS, "/", runtime.GOARCH, ")",
	}, "")
}

func main() {
	rootCmd := run()

	RegisterCommands(rootCmd)

	if len(os.Args) <= 1 {
		err := rootCmd.Help()
		if err != nil {
			return
		}
		os.Exit(1)
	}

	err := rootCmd.Execute()

	cobra.CheckErr(err)
}
