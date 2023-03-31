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
	BIN                       = filepath.Join(PROGRAMDIR, "..", "bin", "v2ray")
	CONFIGDIR                 = filepath.Join(PROGRAMDIR, "..", "config")
	RESULTDIR                 = filepath.Join(PROGRAMDIR, "..", "result")
	START_DT_STR              = time.Now().Format("2006-01-02_15:04:05")
	INTERIM_RESULTS_PATH      = filepath.Join(RESULTDIR, START_DT_STR+"_result.csv")
	FINAL_RESULTS_PATH_SORTED = filepath.Join(RESULTDIR, START_DT_STR+"_final.txt")
)

func PrintInformation(Config ConfigStruct, Worker Worker, shuffle Shuffling) {
	fmt.Printf(`-------------------------------------
Configuration :
User ID : %v%v%v
WS Header Host: %v%v%v
WS Header Path : %v%v%v
Address Port : %v%v%v
SNI : %v%v%v
Start Proccess Timeout : %v%v%v
Upload Test : %v%v%v
Fronting Request Test : %v%v%v
Minimum Download Speed : %v%v%v
Maximum Download Time : %v%v%v
Minimum Upload Speed : %v%v%v
Maximum Upload Time : %v%v%v
Fronting Timeout : %v%v%v
Maximum Download Latency : %v%v%v
Maximum Upload Latency : %v%v%v
Number of Tries : %v%v%v
VPN Mode : %v%v%v
Shuffling : %v%v%v
Total Threads : %v%v%v
-------------------------------------
`,
		utils.Colors.OKBLUE, Config.User_id, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Ws_header_host, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Ws_header_path, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Address_port, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Sni, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Startprocess_timeout, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Do_upload_test, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Do_fronting_test, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Download.Min_dl_speed, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Download.Max_dl_time, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Upload.Min_ul_speed, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Upload.Max_ul_time, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Fronting_timeout, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Download.Max_dl_latency, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Upload.Max_ul_latency, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.N_tries, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Vpn, utils.Colors.ENDC,
		utils.Colors.OKBLUE, shuffle, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Threads, utils.Colors.ENDC,
	)
}

func CreateTestConfig(configPath string, startprocessTimeout float64,
	doUploadTest bool, minDlSpeed float64,
	minUlSpeed float64, maxDlTime float64,
	maxUlTime float64, frontingTimeout float64,
	fronting bool, maxDlLatency float64,
	maxUlLatency float64, nTries int, Vpn bool, threads int, shuffle bool) (ConfigStruct, Worker, Shuffling) {

	if configPath == "" {
		log.Fatalf("Configuration file are not loaded please use the --config or -c flag to use the configuration file.")
	}

	jsonFile, err := os.Open(configPath)
	if err != nil {
		log.Printf("%vError occurred during opening the configuration file.\n%v",
			utils.Colors.WARNING, utils.Colors.ENDC)
		log.Fatal(err)
	}
	defer jsonFile.Close()

	var jsonFileContent map[string]interface{}
	byteValue, _ := io.ReadAll(jsonFile)
	json.Unmarshal(byteValue, &jsonFileContent)

	ConfigObject := ConfigStruct{
		User_id:          jsonFileContent["id"].(string),
		Ws_header_host:   jsonFileContent["host"].(string),
		Address_port:     jsonFileContent["port"].(string),
		Sni:              jsonFileContent["serverName"].(string),
		Ws_header_path:   "/" + strings.TrimLeft(jsonFileContent["path"].(string), "/"),
		Fronting_timeout: frontingTimeout,
		N_tries:          nTries,
		TestBool: TestBool{
			Do_upload_test:   doUploadTest,
			Do_fronting_test: fronting,
		},
	}

	WorkerObject := Worker{
		Download: Download{
			Min_dl_speed:   minDlSpeed,
			Max_dl_time:    maxDlTime,
			Max_dl_latency: maxDlLatency,
		},
		Upload: Upload{
			Min_ul_speed:   minUlSpeed,
			Max_ul_time:    maxUlTime,
			Max_ul_latency: maxUlLatency,
		},
		Startprocess_timeout: startprocessTimeout,
		Threads:              threads,
		Vpn:                  Vpn,
	}

	PrintInformation(ConfigObject, WorkerObject, Shuffling(shuffle))
	return ConfigObject, WorkerObject, Shuffling(shuffle)
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
