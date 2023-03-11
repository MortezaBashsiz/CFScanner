package main

import (
	configuration "CFScanner/configuration"
	scan "CFScanner/scanner"
	utils "CFScanner/utils"
	"bufio"
	"fmt"
	"log"
	"os"
	"runtime"
	"strings"

	"github.com/spf13/cobra"
)

var config = configuration.ConfigStruct{
	Local_port:           0,
	Address_port:         "0",
	User_id:              "",
	Ws_header_host:       "",
	Ws_header_path:       "",
	Sni:                  "",
	Do_upload_test:       false,
	Do_fronting_test:     false,
	Min_dl_speed:         50.0,
	Min_ul_speed:         50.0,
	Max_dl_time:          -2.0,
	Max_ul_time:          -2.0,
	Max_dl_latency:       -1.0,
	Max_ul_latency:       -1.0,
	Fronting_timeout:     -1.0,
	Startprocess_timeout: -1.0,
	N_tries:              -1,
	Vpn:                  false,
}

// Program Info
var (
	version  = "1.0"
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
	var threads int
	var configPath string
	var Vpn bool
	var subnets string
	var doUploadTest bool
	var nTries int
	var minDLSpeed float64
	var minULSpeed float64
	var maxDLTime float64
	var maxULTime float64

	var startProcessTimeout float64
	var frontingTimeout float64
	var maxDLLatency float64
	var maxULLatency float64
	var fronting bool
	var v2raypath string

	var bigIPList []string

	rootCmd := &cobra.Command{
		Use:   os.Args[0],
		Short: codename,
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println(VersionStatement())
			if v2raypath != "" {
				configuration.BINDIR = v2raypath
			}
			if !Vpn {
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
			if file {
				if subnets != "" {
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
				}
			} else {
				// Parsing cidr input
				if strings.Contains(subnets, "/") {
					var subnetips []string
					subnetips = append(subnetips, subnets)

					ips := utils.IPParser(subnetips)

					IPLIST = append(IPLIST, ips...)
				} else {
					// Parsing ip input
					var validateip string = utils.IPValidator(subnets)
					IPLIST = append(IPLIST, validateip)
				}
			}

			// Create Configuration file
			testConfig := configuration.CreateTestConfig(configPath, startProcessTimeout, doUploadTest,
				minDLSpeed, minULSpeed, maxDLTime, maxULTime,
				frontingTimeout, fronting, maxDLLatency, maxULLatency,
				nTries, Vpn)

			// Total number of IPS
			var nTotalIPs int

			for _, ips := range IPLIST {
				numIPs := utils.GetNumIPsInCIDR(ips)
				nTotalIPs += numIPs
			}

			// Parsing and Validanting IPLISTS
			bigIPList = utils.IPParser(IPLIST)

			fmt.Println("Total Threads : ", utils.Colors.OKBLUE, threads, utils.Colors.ENDC)
			fmt.Printf("Starting to scan %v%d%v IPS.\n", utils.Colors.OKGREEN, nTotalIPs, utils.Colors.ENDC)
			fmt.Println("-------------------------------------")
			// begin scanning process
			scan.Scanner(&testConfig, bigIPList, threadsCount)
			fmt.Println("Results Written in :", configuration.INTERIM_RESULTS_PATH)
			fmt.Println("Sorted IPS Written in :", configuration.FINAL_RESULTS_PATH_SORTED)
		},
	}
	rootCmd.PersistentFlags().IntVarP(&threads, "threads", "t", 1, "Number of threads to use for parallel scanning")
	rootCmd.PersistentFlags().StringVarP(&configPath, "config", "c", "", "The path to the config file. For config file example, see https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/ClientConfig.json")
	rootCmd.PersistentFlags().BoolVar(&Vpn, "vpn", false, "If passed, test with creating vpn connections")
	rootCmd.PersistentFlags().StringVarP(&subnets, "subnets", "s", "", "The file or subnet. each line should be in the form of ip.ip.ip.ip/subnet_mask or ip.ip.ip.ip.")
	rootCmd.PersistentFlags().BoolVar(&doUploadTest, "upload", false, "If True, upload test will be conducted")
	rootCmd.PersistentFlags().BoolVar(&fronting, "fronting", false, "If True, fronting request test will be conducted")
	rootCmd.PersistentFlags().IntVar(&nTries, "tries", 1, "Number of times to try each IP. An IP is marked as OK if all tries are successful")
	rootCmd.PersistentFlags().Float64Var(&minDLSpeed, "download-speed", 50, "Minimum acceptable download speed in kilobytes per second")
	rootCmd.PersistentFlags().Float64Var(&minULSpeed, "upload-speed", 50, "Maximum acceptable upload speed in kilobytes per second")
	rootCmd.PersistentFlags().Float64Var(&maxDLTime, "download-time", 2, "Maximum (effective, excluding http time) time to spend for each download")
	rootCmd.PersistentFlags().Float64Var(&maxULTime, "upload-time", 2, "Maximum (effective, excluding http time) time to spend for each upload")
	rootCmd.PersistentFlags().Float64Var(&frontingTimeout, "fronting-timeout", 1.0, "Maximum time to wait for fronting response")
	rootCmd.PersistentFlags().Float64Var(&maxDLLatency, "download-latency", 2.0, "Maximum allowed latency for download")
	rootCmd.PersistentFlags().Float64Var(&maxULLatency, "upload-latency", 2.0, "Maximum allowed latency for download")
	rootCmd.PersistentFlags().Float64Var(&startProcessTimeout, "startprocess-timeout", 10, "")
	rootCmd.PersistentFlags().StringVar(&v2raypath, "v2ray-path", "", "Custom V2Ray path for using v2ray binary on another directory.")

	if len(os.Args) <= 1 {
		rootCmd.Help()
	}

	err := rootCmd.Execute()

	cobra.CheckErr(err)

}
