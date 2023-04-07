package main

import (
	configuration "CFScanner/configuration"
	"CFScanner/scanner"
	"CFScanner/utils"
	"bufio"
	"fmt"
	"github.com/spf13/cobra"
	"log"
	"math/rand"
	"os"
	"strings"
	"time"
)

func run() *cobra.Command {
	rootCmd := &cobra.Command{
		Use:     os.Args[0],
		Short:   codename,
		Version: version,
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println(VersionStatement())

			if vpnPath != "" {
				configuration.BIN = vpnPath
			}

			if Vpn {
				utils.CreateDir(configuration.DIR)
			}

			utils.CreateDir(configuration.RESULTDIR)

			// set file output type
			var outputType string
			if writerType == "csv" {
				outputType = configuration.CSVInterimResultsPath
			}
			if writerType == "json" {
				outputType = configuration.JSONInterimResultsPath
			}

			if err := configuration.CreateInterimResultsFile(outputType, nTries, writerType); err != nil {
				fmt.Printf("Error creating interim results file: %v\n", err)
			}
			// number of threads for scanning
			threadsCount := threads

			// lists of ip for scanning process
			var LIST []string

			LIST = IOScanner(LIST)

			// Parsing and Validating IP LISTS
			bigIPList = utils.IPParser(LIST)

			// Shuffling IPList
			if shuffle {
				rand.Shuffle(len(bigIPList), func(i, j int) {
					bigIPList[i], bigIPList[j] = bigIPList[j], bigIPList[i]
				})
			}

			// Total number of IPS
			numberIPS := utils.TotalIps(bigIPList)

			if int(numberIPS) <= 0 {
				log.Fatal("Scanning Failed : No IP detected")
			}

			config := configuration.Configuration{
				Config: configuration.ConfigStruct{
					FrontingTimeout: frontingTimeout,
					NTries:          nTries,
					Writer:          writerType,
					TestBool: configuration.TestBool{
						DoUploadTest:   doUploadTest,
						DoFrontingTest: fronting,
					},
				},

				Worker: configuration.Worker{
					Threads:             threads,
					Vpn:                 Vpn,
					StartProcessTimeout: startProcessTimeout,
					Download: struct {
						MinDlSpeed   float64
						MaxDlTime    float64
						MaxDlLatency float64
					}{MinDlSpeed: minDLSpeed, MaxDlTime: maxDLTime, MaxDlLatency: maxDLLatency},
					Upload: struct {
						MinUlSpeed   float64
						MaxUlTime    float64
						MaxUlLatency float64
					}{MinUlSpeed: minULSpeed, MaxUlTime: maxULTime, MaxUlLatency: maxULLatency},
				},

				Shuffling: shuffle,
			}

			// Create Configuration file & append vpn fields
			config = config.CreateTestConfig(configPath)

			timer := time.Now()
			fmt.Printf("Starting to scan %v%d%v IPS.\n\n", utils.Colors.OKGREEN, numberIPS, utils.Colors.ENDC)
			// Begin scanning process
			scanner.Start(config, config.Worker, bigIPList, threadsCount)

			fmt.Println("Results Written in :", outputType)
			fmt.Println("Sorted IPS Written in :", configuration.FinalResultsPathSorted)
			fmt.Println("Time Elapse :", time.Since(timer))
		},
	}
	return rootCmd
}

func IOScanner(LIST []string) []string {
	file, _ := utils.Exists(subnets)

	if file && subnets != "" {
		subnetFilePath := subnets
		subnetFile, err := os.Open(subnetFilePath)
		if err != nil {
			log.Fatal(err)
		}
		defer func(subnetFile *os.File) {
			err := subnetFile.Close()
			if err != nil {

			}
		}(subnetFile)

		newScanner := bufio.NewScanner(subnetFile)
		for newScanner.Scan() {
			LIST = append(LIST, strings.TrimSpace(newScanner.Text()))
		}
		if err := newScanner.Err(); err != nil {
			log.Fatal(err)
		}

	} else {
		// type conversion of string subnet to []string
		var subnet []string
		subnet = append(subnet, subnets)

		ips := utils.IPParser(subnet)

		LIST = append(LIST, ips...)

	}
	return LIST
}
