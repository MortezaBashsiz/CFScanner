package config

import (
	scan "CFScanner/scanner"
	"CFScanner/utils"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"os"
	"strings"
	"time"
)

func CreateTestConfig(configPath string, startprocessTimeout time.Duration,
	doUploadTest bool, minDlSpeed float64,
	minUlSpeed float64, maxDlTime float64,
	maxUlTime float64, frontingTimeout float64,
	fronting bool, maxDlLatency float64,
	maxUlLatency float64, nTries int, noVpn bool) scan.ConfigStruct {

	jsonFile, err := os.Open(configPath)
	if err != nil {
		log.Fatal(err)
	}
	defer jsonFile.Close()

	var jsonFileContent map[string]interface{}
	byteValue, _ := io.ReadAll(jsonFile)
	json.Unmarshal(byteValue, &jsonFileContent)

	// proctimeout := int64(startprocessTimeout / int64(time.Millisecond))

	ConfigObject := scan.ConfigStruct{
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
		No_vpn:               noVpn,
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
		"No VPN Mode :", utils.Colors.OKBLUE, ConfigObject.No_vpn, utils.Colors.ENDC)

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
