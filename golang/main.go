package main

import (
	"github.com/spf13/cobra"
	"os"
	"runtime"
	"strings"
	"time"
)

// Program Info
var (
	version  = "v" + time.Now().Format("2006.01.02")
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
