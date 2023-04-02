package main

import (
	"os"
	"runtime"
	"strings"
	"time"

	"github.com/spf13/cobra"
)

// Program Info
var (
	version  = "1.4"
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
	timer := time.Now()
	rootCmd := run(timer)

	Registercommands(rootCmd)

	if len(os.Args) <= 1 {
		rootCmd.Help()
	}

	err := rootCmd.Execute()

	cobra.CheckErr(err)
}
