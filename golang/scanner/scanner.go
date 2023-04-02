package scanner

import (
	config "CFScanner/configuration"
	"CFScanner/speedtest"
	utils "CFScanner/utils"
	"CFScanner/v2raysvc"
	"fmt"
	"log"
	"math"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"sync"
	"time"
)

var results [][]string

var (
	downloadSpeed   float64
	downloadLatency float64
	uploadSpeed     float64
	uploadLatency   float64
)

func resultMap(ip string) map[string]interface{} {
	var result = map[string]interface{}{
		"ip": ip,
		"download": map[string]interface{}{
			"speed":   []float64{},
			"latency": []int{},
		},
		"upload": map[string]interface{}{
			"speed":   []float64{},
			"latency": []int{},
		},
	}
	return result

}

func scanner(ip string, Config config.ConfigStruct, Worker config.Worker) map[string]interface{} {

	var result = resultMap(ip)

	var Upload = &Worker.Upload
	var Download = &Worker.Download

	var proxies map[string]string = nil
	var process *exec.Cmd = nil

	if Worker.Vpn {
		v2rayConfigPath := v2raysvc.CreateV2rayConfig(ip, Config)
		var err error
		process, proxies, err = v2raysvc.StartV2RayService(v2rayConfigPath, time.Duration(Worker.StartprocessTimeout))
		if err != nil {
			log.Printf("%vERROR - %vCould not start v2ray service%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, utils.Colors.ENDC)
			log.Fatal(err)
			return nil
		}

		defer process.Process.Kill()
		defer func() {
			if r := recover(); r != nil {
				log.Printf("%sFAIL %v%15s Panic: %v%v\n", utils.Colors.FAIL, utils.Colors.WARNING, ip, r, utils.Colors.ENDC)
			}
		}()
	}

	for tryIdx := 0; tryIdx < Config.NTries; tryIdx++ {
		// Fronting test
		if Config.DoFrontingTest {
			fronting := speedtest.FrontingTest(ip, time.Duration(Config.FrontingTimeout))

			if !fronting {
				return nil
			}
		}

		// Check download speed
		m, done := downloader(ip, Download, proxies, result)
		if done {
			return m
		}
		// upload speed test
		if Config.DoUploadTest {
			m2, done2 := uploader(ip, Upload, proxies, result)
			if done2 {
				return m2
			}
		}

		dltimeLatency := math.Round(downloadLatency * 1000)
		uptimeLatency := math.Round(uploadLatency * 1000)
		log.Printf("%vOK IP: %v , Download: %7.4fmbps , Upload: %7.4fmbps , UP_Latency: %vms , DL_Latency: %vms%v\n",
			utils.Colors.OKGREEN, ip, downloadSpeed, uploadSpeed, uptimeLatency, dltimeLatency, utils.Colors.ENDC)
	}
	return result
}

func uploader(ip string, Upload *config.Upload, proxies map[string]string, result map[string]interface{}) (map[string]interface{}, bool) {
	var err error
	nBytes := Upload.MinUlSpeed * 1000 * Upload.MaxUlTime
	uploadSpeed, uploadLatency, err = speedtest.UploadSpeedTest(int(nBytes), proxies,
		time.Duration(Upload.MaxUlLatency))

	if err != nil {
		log.Printf("%sFAIL %v%15s Upload error : %v%v\n", utils.Colors.FAIL, utils.Colors.WARNING, ip, err, utils.Colors.ENDC)

		return nil, true
	}
	if uploadLatency <= Upload.MaxUlLatency {
		uploadSpeedKbps := uploadSpeed / 8 * 1000
		if uploadSpeedKbps >= Upload.MinUlSpeed {
			result["upload"].(map[string]interface{})["speed"] =
				append(result["upload"].(map[string]interface{})["speed"].([]float64), uploadSpeed)
			result["upload"].(map[string]interface{})["latency"] =
				append(result["upload"].(map[string]interface{})["latency"].([]int), int(math.Round(uploadLatency*1000)))

		} else {
			log.Printf("%sFAIL %v%15s Upload too slow %f kBps < %f kBps%s\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, uploadSpeedKbps, Upload.MinUlSpeed, utils.Colors.ENDC)

			return nil, true
		}
	} else {
		log.Printf("%sFAIL %v%15s Upload latency too high  %s\n",
			utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)

		return nil, true
	}
	return nil, false
}

func downloader(ip string, Download *config.Download, proxies map[string]string, result map[string]interface{}) (map[string]interface{}, bool) {
	var err error
	nBytes := Download.MinDlSpeed * 1000 * Download.MaxDlTime
	downloadSpeed, downloadLatency, err = speedtest.DownloadSpeedTest(int(nBytes), proxies,
		time.Duration(Download.MaxDlLatency))

	if err != nil {
		if strings.Contains(strings.ToLower(err.Error()), "download/upload too slow") {
			log.Printf("%vFAIL %v%15s Download too slow\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip)
		} else {
			log.Printf("%vFAIL %v%15s Download error%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
		}
		return nil, true
	}

	if downloadLatency <= Download.MaxDlLatency {
		downloadSpeedKBps := downloadSpeed / 8 * 1000
		if downloadSpeedKBps >= Download.MinDlSpeed {
			result["download"].(map[string]interface{})["speed"] =
				append(result["download"].(map[string]interface{})["speed"].([]float64), downloadSpeed)
			result["download"].(map[string]interface{})["latency"] =
				append(result["download"].(map[string]interface{})["latency"].([]int), int(math.Round(downloadLatency*1000)))

		} else {
			log.Printf("%vFAIL %v%15s Download too slow %.4f kBps < %.4f kBps%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, downloadSpeedKBps, Download.MinDlSpeed, utils.Colors.ENDC)
			return nil, true
		}
	} else {
		log.Printf("%vFAIL %v%15s High Download latency %.4f s > %.4f s%v\n",
			utils.Colors.FAIL, utils.Colors.WARNING, ip, downloadLatency, Download.MaxDlLatency, utils.Colors.ENDC)
		return nil, true
	}
	return nil, false
}

func scannerMap(testConfig *config.ConfigStruct, worker *config.Worker, ip string) {
	res := scanner(ip, *testConfig, *worker)

	if res != nil {
		downLatencyInt, ok := res["download"].(map[string]interface{})["latency"].([]int)

		if !ok {
			log.Printf("Error getting download latency for IP %s", ip)
		}

		// make downLatencyInt to float64
		downLatency := make([]float64, len(downLatencyInt))
		for i, v := range downLatencyInt {
			downLatency[i] = float64(v)
		}

		downMeanJitter := utils.MeanJitter(downLatency)

		uploadLatencyInt, ok := res["upload"].(map[string]interface{})["latency"].([]int)

		if !ok {
			log.Printf("Error getting upload latency for IP %s", ip)

		}

		// make uploadLatencyInt to float64
		uploadLatency := make([]float64, len(uploadLatencyInt))
		for i, v := range uploadLatencyInt {
			uploadLatency[i] = float64(v)
		}

		upMeanJitter := -1.0
		if testConfig.DoUploadTest && ok {
			upMeanJitter = utils.MeanJitter(uploadLatency)
		}

		downSpeed, ok := res["download"].(map[string]interface{})["speed"].([]float64)

		if !ok {
			log.Printf("Error getting download speed for IP %s , %v", ip, ok)
		}

		meanDownSpeed := utils.Mean(downSpeed)
		meanuploadSpeed := -1.0

		uploadSpeed, ok := res["upload"].(map[string]interface{})["speed"].([]float64)

		if !ok {
			log.Printf("Error getting upload speed for IP %s", ip)
		}

		if testConfig.DoUploadTest {
			meanuploadSpeed = utils.Mean(uploadSpeed)
		}

		meanDownLatency := utils.Mean(downLatency)
		meanuploadLatency := -1.0
		if testConfig.DoUploadTest {
			meanuploadLatency = utils.Mean(uploadLatency)
		}

		// change download latency to string for using it with saveresults func
		var latencystring string

		for _, f := range downLatencyInt {
			latencystring = fmt.Sprintf("%d", f)
		}

		results = append(results, []string{latencystring, ip})

		var Writer Writer = CSV{
			res:                 res,
			ip:                  ip,
			downloadMeanJitter:  downMeanJitter,
			uploadMeanJitter:    upMeanJitter,
			meanDownloadSpeed:   meanDownSpeed,
			meanDownloadLatency: meanDownLatency,
			meanUploadSpeed:     meanuploadSpeed,
			meanUploadLatency:   meanuploadLatency,
		}

		Writer.Output()
		Writer.CSVWriter()

	}
}

func Worker(testConfig *config.ConfigStruct, worker *config.Worker, cidrList []string, threadsCount int) {
	var wg sync.WaitGroup

	n := len(cidrList)
	batchSize := len(cidrList) / threadsCount
	batches := make([][]string, threadsCount)

	for i := range batches {
		start := i * batchSize
		end := (i + 1) * batchSize
		if i == threadsCount-1 {
			end = n
		}
		batches[i] = cidrList[start:end]

	}
	wg.Add(threadsCount)
	for i := 0; i < threadsCount; i++ {
		go func(batch []string) {
			defer wg.Done()
			for _, ip := range batch {

				scannerMap(testConfig, worker, ip)
			}

		}(batches[i])
	}
	wg.Wait()

	saveResults(results, config.FINAL_RESULTS_PATH_SORTED, true)

}

func saveResults(results [][]string, savePath string, sort bool) error {
	// clean the results and make sure the first element is integer
	for i := 0; i < len(results); i++ {
		ms, err := strconv.Atoi(strings.TrimSuffix(results[i][0], " ms"))
		if err != nil {
			return err
		}
		results[i][0] = strconv.Itoa(ms)
	}

	if sort {
		// sort the results based on response time using bubble sort
		for i := 0; i < len(results); i++ {
			for j := 0; j < len(results)-1; j++ {
				ms1, _ := strconv.Atoi(results[j][0])
				ms2, _ := strconv.Atoi(results[j+1][0])
				if ms1 > ms2 {
					results[j], results[j+1] = results[j+1], results[j]
				}
			}
		}
	}

	// write the results to file
	var lines []string
	for _, res := range results {
		lines = append(lines, strings.Join(res, " "))
	}
	data := []byte(strings.Join(lines, "\n") + "\n")
	err := os.WriteFile(savePath, data, 0644)
	if err != nil {
		return err
	}

	return nil
}
