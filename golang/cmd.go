package main

import "github.com/spf13/cobra"

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

func Registercommands(rootCmd *cobra.Command) {
	rootCmd.PersistentFlags().IntVarP(&threads, "threads", "t", 1, "Number of threads to use for parallel scanning")
	rootCmd.PersistentFlags().StringVarP(&configPath, "config", "c", "", "The path to the config file.")
	rootCmd.PersistentFlags().BoolVar(&Vpn, "vpn", false, "If passed, test with creating vpn connections")
	rootCmd.PersistentFlags().StringVarP(&subnets, "subnets", "s", "", "The file or subnet. each line should be in the form of ip.ip.ip.ip/subnet_mask or ip.ip.ip.ip.")
	rootCmd.PersistentFlags().BoolVar(&doUploadTest, "upload", false, "If passed, upload test will be conducted")
	rootCmd.PersistentFlags().BoolVar(&fronting, "fronting", false, "If passed, fronting request test will be conducted")
	rootCmd.PersistentFlags().IntVarP(&nTries, "tries", "n", 1, "Number of times to try each IP. An IP is marked as OK if all tries are successful.")
	rootCmd.PersistentFlags().Float64Var(&minDLSpeed, "download-speed", 50, "Maximum acceptable download speed in kilobytes per second")
	rootCmd.PersistentFlags().Float64Var(&minULSpeed, "upload-speed", 50, "Maximum acceptable upload speed in kilobytes per second")
	rootCmd.PersistentFlags().Float64Var(&maxDLTime, "download-time", 2, "Maximum (effective, excluding http time) time to spend for each download")
	rootCmd.PersistentFlags().Float64Var(&maxULTime, "upload-time", 2, "Maximum (effective, excluding http time) time to spend for each upload")
	rootCmd.PersistentFlags().Float64Var(&frontingTimeout, "fronting-timeout", 1.0, "Maximum time to wait for fronting response")
	rootCmd.PersistentFlags().Float64Var(&maxDLLatency, "download-latency", 3.0, "Maximum allowed latency for download")
	rootCmd.PersistentFlags().Float64Var(&maxULLatency, "upload-latency", 3.0, "Maximum allowed latency for upload")
	rootCmd.PersistentFlags().Float64Var(&startProcessTimeout, "startprocess-timeout", 12, "Process timeout for v2ray.")
	rootCmd.PersistentFlags().StringVar(&v2raypath, "v2ray-path", "", "Custom V2Ray binary path for using v2ray binary in another directory.")

}
