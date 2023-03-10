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
	PROGRAMDIR           = filepath.Dir(os.Args[0])
	BINDIR               = filepath.Join(PROGRAMDIR, "..", "bin")
	CONFIGDIR            = filepath.Join(PROGRAMDIR, "..", "config")
	RESULTDIR            = filepath.Join(PROGRAMDIR, "..", "result")
	START_DT_STR         = time.Now().Format("2006-01-02_15:04:05")
	INTERIM_RESULTS_PATH = filepath.Join(RESULTDIR, START_DT_STR+"_result.csv")
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

func CreateTestConfig(configPath string, startprocessTimeout float64,
	doUploadTest bool, minDlSpeed float64,
	minUlSpeed float64, maxDlTime float64,
	maxUlTime float64, frontingTimeout float64,
	fronting bool, maxDlLatency float64,
	maxUlLatency float64, nTries int, Vpn bool) ConfigStruct {

	jsonFile, err := os.Open(configPath)
	if err != nil {
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
	fmt.Println("Config :", "\n", "User ID :", utils.Colors.OKBLUE, ConfigObject.User_id, utils.Colors.ENDC, "\n",
		"WS Header Host:", utils.Colors.OKBLUE, ConfigObject.Ws_header_host, utils.Colors.ENDC, "\n",
		"WS Header Path : ", utils.Colors.OKBLUE, ConfigObject.Ws_header_path, utils.Colors.ENDC, "\n",
		"Address Port :", utils.Colors.OKBLUE, ConfigObject.Address_port, utils.Colors.ENDC, "\n",
		"SNI :", utils.Colors.OKBLUE, ConfigObject.Sni, utils.Colors.ENDC, "\n",
		"Start Proccess Timeout :", utils.Colors.OKBLUE, ConfigObject.Startprocess_timeout, utils.Colors.ENDC, "\n",
		"Upload Test :", utils.Colors.OKBLUE, ConfigObject.Do_upload_test, utils.Colors.ENDC, "\n",
		"Fronting Request Test :", utils.Colors.OKBLUE, ConfigObject.Do_fronting_test, utils.Colors.ENDC, "\n",
		"Minimum Download Speed :", utils.Colors.OKBLUE, ConfigObject.Min_dl_speed, utils.Colors.ENDC, "\n",
		"Maximum Download Time :", utils.Colors.OKBLUE, ConfigObject.Max_dl_time, utils.Colors.ENDC, "\n",
		"Minimum Upload Speed :", utils.Colors.OKBLUE, ConfigObject.Min_ul_speed, utils.Colors.ENDC, "\n",
		"Maximum Upload Time :", utils.Colors.OKBLUE, ConfigObject.Max_ul_time, utils.Colors.ENDC, "\n",
		"Fronting Timeout :", utils.Colors.OKBLUE, ConfigObject.Fronting_timeout, utils.Colors.ENDC, "\n",
		"Maximum Download Latency :", utils.Colors.OKBLUE, ConfigObject.Max_dl_latency, utils.Colors.ENDC, "\n",
		"Maximum Upload Latency :", utils.Colors.OKBLUE, ConfigObject.Max_ul_latency, utils.Colors.ENDC, "\n",
		"Number of Tries :", utils.Colors.OKBLUE, ConfigObject.N_tries, utils.Colors.ENDC, "\n",
		"VPN Mode :", utils.Colors.OKBLUE, ConfigObject.Vpn, utils.Colors.ENDC)

	return ConfigObject
}

func CreateInterimResultsFile(interimResultsPath string, nTries int) error {
	emptyFile, err := os.Create(interimResultsPath)
	if err != nil {
		return fmt.Errorf("failed to create interim results file: %w", err)
	}
	defer emptyFile.Close()

	titles := []string{
		"avg_download_speed", "avg_upload_speed",
		"avg_download_latency", "avg_upload_latency",
		"avg_download_jitter", "avg_upload_jitter",
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
