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
	PROGRAMDIR             = filepath.Dir(os.Args[0])
	DIR                    = filepath.Join(PROGRAMDIR, "config")
	RESULTDIR              = filepath.Join(PROGRAMDIR, "result")
	StartDtStr             = time.Now().Format("2006-01-02_15:04:05")
	CSVInterimResultsPath  = filepath.Join(RESULTDIR, StartDtStr+"_result.csv")
	JSONInterimResultsPath = filepath.Join(RESULTDIR, StartDtStr+"_result.json")
	FinalResultsPathSorted = filepath.Join(RESULTDIR, StartDtStr+"_final.txt")
)

func (C Configuration) PrintInformation() {
	fmt.Printf(`-------------------------------------
Configuration :
User ID : %v%v%v
WS Header Host: %v%v%v
WS Header Path : %v%v%v
Address Port : %v%v%v
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
Xray-core : %v%v%v
Xray-loglevel : %v%v%v
Shuffling : %v%v%v
Writer : %v%v%v
Total Threads : %v%v%v
-------------------------------------
`,
		utils.Colors.OKBLUE, C.Config.UserId, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.WsHeaderHost, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.WsHeaderPath, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.AddressPort, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.DoUploadTest, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.DoFrontingTest, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Download.MinDlSpeed, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Download.MaxDlTime, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Upload.MinUlSpeed, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Upload.MaxUlTime, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.FrontingTimeout, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Download.MaxDlLatency, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Upload.MaxUlLatency, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.NTries, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Vpn, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.LogLevel, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Shuffling, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Config.Writer, utils.Colors.ENDC,
		utils.Colors.OKBLUE, C.Worker.Threads, utils.Colors.ENDC,
	)
}

func (C Configuration) CreateTestConfig(configPath string) Configuration {

	if configPath == "" {
		log.Fatalf("Configuration file are not loaded please use the --config or -c flag to use the configuration file.")
	}

	jsonFile, err := os.Open(configPath)
	if err != nil {
		log.Printf("%vError occurred during opening the configuration file.\n%v",
			utils.Colors.WARNING, utils.Colors.ENDC)
		log.Fatal(err)
	}
	defer func(jsonFile *os.File) {
		err := jsonFile.Close()
		if err != nil {
		}
	}(jsonFile)

	var jsonFileContent map[string]interface{}
	byteValue, _ := io.ReadAll(jsonFile)

	content := json.Unmarshal(byteValue, &jsonFileContent)
	if content != nil {
		return Configuration{}
	}

	C.Config.UserId = jsonFileContent["id"].(string)
	C.Config.WsHeaderHost = jsonFileContent["host"].(string)
	C.Config.AddressPort = jsonFileContent["port"].(string)
	//C.Config.Sni = jsonFileContent["serverName"].(string)
	C.Config.WsHeaderPath = "/" + strings.TrimLeft(jsonFileContent["path"].(string), "/")

	C.PrintInformation()
	return C
}

func CreateInterimResultsFile(interimResultsPath string, nTries int, writer string) error {
	emptyFile, err := os.Create(interimResultsPath)
	if err != nil {
		return fmt.Errorf("failed to create interim results file: %w", err)
	}

	defer func(emptyFile *os.File) {
		err := emptyFile.Close()
		if err != nil {

		}
	}(emptyFile)

	if strings.ToLower(writer) == "csv" {

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

	}
	return nil
}
