package config

import (
	"CFScanner/utils"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"
)

var (
	PROGRAMDIR                = filepath.Dir(os.Args[0])
	BINDIR                    = filepath.Join(PROGRAMDIR, "..", "bin")
	CONFIGDIR                 = filepath.Join(PROGRAMDIR, "..", "config")
	RESULTDIR                 = filepath.Join(PROGRAMDIR, "..", "result")
	START_DT_STR              = time.Now().Format("2006-01-02_15:04:05")
	INTERIM_RESULTS_PATH      = filepath.Join(RESULTDIR, START_DT_STR+"_result.csv")
	FINAL_RESULTS_PATH_SORTED = filepath.Join(RESULTDIR, START_DT_STR+"_final.txt")
)

type ConfigStruct struct {
	Local_port           int
	Address_port         string
	User_id              string
	Ws_header_host       string
	Ws_header_path       string
	Sni                  string
	Do_upload_test       bool
	Do_fronting_test     bool
	Min_dl_speed         float64 // kilobytes per second
	Min_ul_speed         float64 // kilobytes per second
	Max_dl_time          float64 // seconds
	Max_ul_time          float64 // seconds
	Max_dl_latency       float64 // seconds
	Max_ul_latency       float64 // seconds
	Fronting_timeout     float64 // seconds
	Startprocess_timeout float64 // seconds
	N_tries              int
	Vpn                  bool
}

func PrintInformation(Config ConfigStruct) {
	fmt.Printf("-------------------------------------\n")
	fmt.Printf("Configuration :\n")
	fmt.Printf("User ID : %v%v%v\n", utils.Colors.OKBLUE, Config.User_id, utils.Colors.ENDC)
	fmt.Printf("WS Header Host: %v%v%v\n", utils.Colors.OKBLUE, Config.Ws_header_host, utils.Colors.ENDC)
	fmt.Printf("WS Header Path : %v%v%v\n", utils.Colors.OKBLUE, Config.Ws_header_path, utils.Colors.ENDC)
	fmt.Printf("Address Port : %v%v%v\n", utils.Colors.OKBLUE, Config.Address_port, utils.Colors.ENDC)
	fmt.Printf("SNI : %v%v%v\n", utils.Colors.OKBLUE, Config.Sni, utils.Colors.ENDC)
	fmt.Printf("Start Proccess Timeout : %v%v%v\n", utils.Colors.OKBLUE, Config.Startprocess_timeout, utils.Colors.ENDC)
	fmt.Printf("Upload Test : %v%v%v\n", utils.Colors.OKBLUE, Config.Do_upload_test, utils.Colors.ENDC)
	fmt.Printf("Fronting Request Test : %v%v%v\n", utils.Colors.OKBLUE, Config.Do_fronting_test, utils.Colors.ENDC)
	fmt.Printf("Minimum Download Speed : %v%v%v\n", utils.Colors.OKBLUE, Config.Min_dl_speed, utils.Colors.ENDC)
	fmt.Printf("Maximum Download Time : %v%v%v\n", utils.Colors.OKBLUE, Config.Max_dl_time, utils.Colors.ENDC)
	fmt.Printf("Minimum Upload Speed : %v%v%v\n", utils.Colors.OKBLUE, Config.Min_ul_speed, utils.Colors.ENDC)
	fmt.Printf("Maximum Upload Time : %v%v%v\n", utils.Colors.OKBLUE, Config.Max_ul_time, utils.Colors.ENDC)
	fmt.Printf("Fronting Timeout : %v%v%v\n", utils.Colors.OKBLUE, Config.Fronting_timeout, utils.Colors.ENDC)
	fmt.Printf("Maximum Download Latency : %v%v%v\n", utils.Colors.OKBLUE, Config.Max_dl_latency, utils.Colors.ENDC)
	fmt.Printf("Maximum Upload Latency : %v%v%v\n", utils.Colors.OKBLUE, Config.Max_ul_latency, utils.Colors.ENDC)
	fmt.Printf("Number of Tries : %v%v%v\n", utils.Colors.OKBLUE, Config.N_tries, utils.Colors.ENDC)
	fmt.Printf("VPN Mode : %v%v%v\n", utils.Colors.OKBLUE, Config.Vpn, utils.Colors.ENDC)
}

func CreateTestConfig(configPath string, startprocessTimeout float64,
	doUploadTest bool, minDlSpeed float64,
	minUlSpeed float64, maxDlTime float64,
	maxUlTime float64, frontingTimeout float64,
	fronting bool, maxDlLatency float64,
	maxUlLatency float64, nTries int, Vpn bool) ConfigStruct {

	jsonFile, err := os.Open(configPath)
	if err != nil {
		fmt.Printf("%verror occurred during opening the configuration file.%v",
			utils.Colors.WARNING, utils.Colors.ENDC)
		log.Fatal(err)
	}
	defer jsonFile.Close()

	var jsonFileContent map[string]interface{}
	byteValue, _ := io.ReadAll(jsonFile)
	json.Unmarshal(byteValue, &jsonFileContent)

	// proctimeout := int64(startprocessTimeout / int64(time.Millisecond))

	ConfigObject := ConfigStruct{
		User_id:              jsonFileContent["id"].(string),
		Ws_header_host:       jsonFileContent["host"].(string),
		Address_port:         jsonFileContent["port"].(string),
		Sni:                  jsonFileContent["serverName"].(string),
		Ws_header_path:       "/" + strings.TrimLeft(jsonFileContent["path"].(string), "/"),
		Startprocess_timeout: startprocessTimeout,
		Do_upload_test:       doUploadTest,
		Min_dl_speed:         minDlSpeed,
		Min_ul_speed:         minUlSpeed,
		Max_dl_time:          maxDlTime,
		Max_ul_time:          maxUlTime,
		Fronting_timeout:     frontingTimeout,
		Do_fronting_test:     fronting,
		Max_dl_latency:       maxDlLatency,
		Max_ul_latency:       maxUlLatency,
		N_tries:              nTries,
		Vpn:                  Vpn,
	}
	PrintInformation(ConfigObject)
	return ConfigObject
}

func CreateInterimResultsFile(interimResultsPath string, nTries int) error {
	emptyFile, err := os.Create(interimResultsPath)
	if err != nil {
		return fmt.Errorf("failed to create interim results file: %w", err)
	}
	defer emptyFile.Close()

	titles := []string{
		"ip",
		"avg_download_speed", "avg_upload_speed",
		"avg_download_latency", "avg_upload_latency",
		"avg_download_jitter", "avg_upload_jitter",
	}

	for i := 1; i <= nTries; i++ {
		titles = append(titles, fmt.Sprintf("ip_%d", i))
	}

	for i := 1; i <= nTries; i++ {
		titles = append(titles, fmt.Sprintf("download_speed_%d", i))
	}

	for i := 1; i <= nTries; i++ {
		titles = append(titles, fmt.Sprintf("upload_speed_%d", i))
	}

	for i := 1; i <= nTries; i++ {
		titles = append(titles, fmt.Sprintf("download_latency_%d", i))
	}

	for i := 1; i <= nTries; i++ {
		titles = append(titles, fmt.Sprintf("upload_latency_%d", i))
	}

	if _, err := fmt.Fprintln(emptyFile, strings.Join(titles, ",")); err != nil {
		return fmt.Errorf("failed to write titles to interim results file: %w", err)
	}

	return nil
}
