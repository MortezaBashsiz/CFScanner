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
		utils.Colors.OKBLUE, Config.UserId, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.WsHeaderHost, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.WsHeaderPath, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.AddressPort, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.Sni, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.StartprocessTimeout, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.DoUploadTest, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.DoFrontingTest, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Download.MinDlSpeed, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Download.MaxDlTime, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Upload.MinUlSpeed, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Upload.MaxUlTime, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.FrontingTimeout, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Download.MaxDlLatency, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Worker.Upload.MaxUlLatency, utils.Colors.ENDC,
		utils.Colors.OKBLUE, Config.NTries, utils.Colors.ENDC,
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
		UserId:          jsonFileContent["id"].(string),
		WsHeaderHost:    jsonFileContent["host"].(string),
		AddressPort:     jsonFileContent["port"].(string),
		Sni:             jsonFileContent["serverName"].(string),
		WsHeaderPath:    "/" + strings.TrimLeft(jsonFileContent["path"].(string), "/"),
		FrontingTimeout: frontingTimeout,
		NTries:          nTries,
		TestBool: TestBool{
			DoUploadTest:   doUploadTest,
			DoFrontingTest: fronting,
		},
	}

	WorkerObject := Worker{
		Download: Download{
			MinDlSpeed:   minDlSpeed,
			MaxDlTime:    maxDlTime,
			MaxDlLatency: maxDlLatency,
		},
		Upload: Upload{
			MinUlSpeed:   minUlSpeed,
			MaxUlTime:    maxUlTime,
			MaxUlLatency: maxUlLatency,
		},
		StartprocessTimeout: startprocessTimeout,
		Threads:             threads,
		Vpn:                 Vpn,
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
