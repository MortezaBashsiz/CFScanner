package main

import (
	configuration "CFScanner/configuration"
	scan "CFScanner/scanner"
	utils "CFScanner/utils"
	"bufio"
	"fmt"
	"log"
	"math/rand"
	"os"
	"runtime"
	"strings"
	"time"

	"github.com/spf13/cobra"
)

// Program Info
var (
	version  = "1.2"
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
	rootCmd := &cobra.Command{
		Use:     os.Args[0],
		Short:   codename,
		Version: version,
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println(VersionStatement())
			// Create Configuration file
			Config, worker, _ := configuration.CreateTestConfig(configPath, startProcessTimeout, doUploadTest,
				minDLSpeed, minULSpeed, maxDLTime, maxULTime,
				frontingTimeout, fronting, maxDLLatency, maxULLatency,
				nTries, Vpn, threads, shuffle)

			if v2raypath != "" {
				configuration.BIN = v2raypath
			}
			if Vpn {
				utils.CreateDir(configuration.CONFIGDIR)
			}
			utils.CreateDir(configuration.RESULTDIR)

			if err := configuration.CreateInterimResultsFile(configuration.INTERIM_RESULTS_PATH, nTries); err != nil {
				fmt.Printf("Error creating interim results file: %v\n", err)
			}
			// number of threads for scanning
			threadsCount := threads

			// lists of ip for scanning proccess
			var IPLIST []string

			file, _ := utils.Exists(subnets)

			if file && subnets != "" {
				subnetFilePath := subnets
				subnetFile, err := os.Open(subnetFilePath)
				if err != nil {
					log.Fatal(err)
				}
				defer subnetFile.Close()

				scanner := bufio.NewScanner(subnetFile)
				for scanner.Scan() {
					IPLIST = append(IPLIST, strings.TrimSpace(scanner.Text()))
				}
				if err := scanner.Err(); err != nil {
					log.Fatal(err)
				}

			} else {
				// type conversion of string subnet to []string
				var subnetip []string
				subnetip = append(subnetip, subnets)

				ips := utils.IPParser(subnetip)

				IPLIST = append(IPLIST, ips...)

			}

			// Parsing and Validanting IPLISTS
			bigIPList = utils.IPParser(IPLIST)

			// Shuffeling IPList
			if shuffle {
				rand.Shuffle(len(bigIPList), func(i, j int) {
					bigIPList[i], bigIPList[j] = bigIPList[j], bigIPList[i]
				})
			}

			// Total number of IPS
			numip := utils.TotalIps(bigIPList)

			fmt.Printf("Starting to scan %v%d%v IPS.\n\n", utils.Colors.OKGREEN, numip, utils.Colors.ENDC)
			// Begin scanning process
			scan.Worker(&Config, &worker, bigIPList, threadsCount)

			fmt.Println("Results Written in :", configuration.INTERIM_RESULTS_PATH)
			fmt.Println("Sorted IPS Written in :", configuration.FINAL_RESULTS_PATH_SORTED)
			fmt.Println("Time Elapse :", time.Since(timer))
		},
	}

	Registercommands(rootCmd)

	if len(os.Args) <= 1 {
		rootCmd.Help()
	}

	err := rootCmd.Execute()

	cobra.CheckErr(err)

}
