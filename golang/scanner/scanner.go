package scanner

import (
	config "CFScanner/configuration"
	"CFScanner/speedtest"
	utils "CFScanner/utils"
	"CFScanner/v2raysvc"
	"encoding/csv"
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

func Checkip(ip string, Config config.ConfigStruct, Worker config.Worker) map[string]interface{} {

	var result = resultMap(ip)

	var Upload = &Worker.Upload
	var Download = &Worker.Download

	var proxies map[string]string
	var process *exec.Cmd

	if Worker.Vpn {
		v2ray_config_path := v2raysvc.CreateV2rayConfig(ip, Config)
		var err error
		process, proxies, err = v2raysvc.StartV2RayService(v2ray_config_path, time.Duration(Worker.Startprocess_timeout))
		if err != nil {
			log.Printf("%vERROR - %vCould not start v2ray service%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, utils.Colors.ENDC)
			log.Fatal(err)
			return nil
		}
	} else {
		process = nil
		proxies = nil
	}

	for tryIdx := 0; tryIdx < Config.N_tries; tryIdx++ {
		// Fronting test
		if Config.Do_fronting_test {
			fronting := speedtest.FrontingTest(ip, time.Duration(Config.Fronting_timeout))

			if !fronting {
				return nil
			}
		}

		// Check download speed
		var err error
		nBytes := Download.Min_dl_speed * 1000 * Download.Max_dl_time
		downloadSpeed, downloadLatency, err = speedtest.DownloadSpeedTest(int(nBytes), proxies, time.Duration(Download.Max_dl_time))
		if err != nil {
			if strings.Contains(strings.ToLower(err.Error()), "download/upload too slow") {
				log.Printf("%vFAIL %v%15s Download too slow\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip)
			} else {
				log.Printf("%vFAIL %v%15s Download error%v\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
			}
			if Worker.Vpn {
				process.Process.Kill()
			}
			return nil
		}

		if downloadLatency <= Download.Max_dl_latency {
			downloadSpeedKBps := downloadSpeed / 8 * 1000
			if downloadSpeedKBps >= Download.Min_dl_speed {
				result["download"].(map[string]interface{})["speed"] =
					append(result["download"].(map[string]interface{})["speed"].([]float64), downloadSpeed)
				result["download"].(map[string]interface{})["latency"] =
					append(result["download"].(map[string]interface{})["latency"].([]int), int(math.Round(downloadLatency)))

			} else {
				log.Printf("%vFAIL %v%15s Download too slow %.4f kBps < %.4f kBps%v\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, downloadSpeedKBps, Download.Min_dl_speed, utils.Colors.ENDC)
				if Worker.Vpn {
					process.Process.Kill()
				}
				return nil
			}
		} else {
			log.Printf("%vFAIL %v%15s High Worker latency %.4f s > %.4f s%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, downloadLatency, Download.Max_dl_latency, utils.Colors.ENDC)
			if Worker.Vpn {
				process.Process.Kill()
			}
			return nil
		}
		// upload speed test
		if Config.Do_upload_test {
			var err error
			nBytes := Upload.Min_ul_speed * 1000 * Upload.Max_ul_time
			uploadSpeed, uploadLatency, err = speedtest.UploadSpeedTest(int(nBytes), proxies, time.Duration(Upload.Max_ul_time))
			if err != nil {
				log.Printf("%sFAIL %v%15s Upload error : %v%v\n", utils.Colors.FAIL, utils.Colors.WARNING, ip, err, utils.Colors.ENDC)
				if Worker.Vpn {
					process.Process.Kill()
				}
				return nil
			}
			if uploadLatency <= Upload.Max_ul_latency {
				uploadSpeedKbps := uploadSpeed / 8 * 1000
				if uploadSpeedKbps >= Upload.Min_ul_speed {
					result["upload"].(map[string]interface{})["speed"] =
						append(result["upload"].(map[string]interface{})["speed"].([]float64), uploadSpeed)
					result["upload"].(map[string]interface{})["latency"] =
						append(result["upload"].(map[string]interface{})["latency"].([]int), int(math.Round(uploadLatency)))

				} else {
					log.Printf("%sFAIL %v%15s Upload too slow %f kBps < %f kBps%s\n",
						utils.Colors.FAIL, utils.Colors.WARNING, ip, uploadSpeedKbps, Upload.Min_ul_speed, utils.Colors.ENDC)
					if Worker.Vpn {
						process.Process.Kill()
					}
					return nil
				}
			} else {
				log.Printf("%sFAIL %v%15s Upload latency too high  %s\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
				if Worker.Vpn {
					process.Process.Kill()
				}
				return nil
			}
		}

		dltimeLatency := math.Round(downloadLatency)
		uptimeLatency := math.Round(uploadLatency)
		log.Printf("%vOK IP: %v , Download: %.2fkBps , Upload: %.2fkbps , UP_Latency: %v , DL_Latency: %v%v\n",
			utils.Colors.OKGREEN, ip, utils.Float64ToKBps(downloadSpeed), utils.Float64ToKBps(uploadSpeed), uptimeLatency, dltimeLatency, utils.Colors.ENDC)
		if Worker.Vpn {
			process.Process.Kill()
		}

	}
	if Worker.Vpn {
		process.Process.Kill()
	}
	return result
}

func scannerMap(testConfig *config.ConfigStruct, worker *config.Worker, ip string) {
	res := Checkip(ip, *testConfig, *worker)

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
		if testConfig.Do_upload_test && ok {
			upMeanJitter = utils.MeanJitter(uploadLatency)
		}

		downSpeed, ok := res["download"].(map[string]interface{})["speed"].([]float64)

		// make downSpeedKbps to return kbps
		downSpeedKbps := make([]float64, len(downSpeed))
		for i, v := range downSpeed {
			downSpeedKbps[i] = utils.Float64ToKBps(v)
		}
		if !ok {
			log.Printf("Error getting download speed for IP %s , %v", ip, ok)
		}
		meanDownSpeed := utils.Mean(downSpeedKbps)
		meanuploadSpeed := -1.0

		uploadSpeed, ok := res["upload"].(map[string]interface{})["speed"].([]float64)

		if !ok {
			log.Printf("Error getting upload speed for IP %s", ip)
		}

		// make downSpeedKbps to return kbps
		uploadSpeedKbps := make([]float64, len(uploadSpeed))
		for i, v := range uploadSpeed {
			uploadSpeedKbps[i] = utils.Float64ToKBps(v)
		}
		if testConfig.Do_upload_test {
			meanuploadSpeed = utils.Mean(uploadSpeedKbps)
		}

		meanDownLatency := utils.Mean(downLatency)
		meanuploadLatency := -1.0
		if testConfig.Do_upload_test {
			meanuploadLatency = utils.Mean(uploadLatency)
		}

		// change download latency to string for using it with saveresults func
		var latencystring string

		for _, f := range downLatencyInt {
			latencystring = fmt.Sprintf("%d", f)
		}

		results = append(results, []string{latencystring, ip})

		InterimOutput(res, ip, downMeanJitter,
			upMeanJitter, meanDownSpeed,
			meanuploadSpeed, meanDownLatency, meanuploadLatency)

		InterimResultsWriter(res, ip, downMeanJitter,
			upMeanJitter, meanDownSpeed,
			meanuploadSpeed, meanDownLatency, meanuploadLatency)
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

	SaveResults(results, config.FINAL_RESULTS_PATH_SORTED, true)

}

func InterimOutput(res map[string]interface{}, ip string, downMeanJitter float64, upMeanJitter float64,
	meanDownSpeed float64, meanuploadSpeed float64,
	meanDownLatency float64, meanuploadLatency float64) {

	log.Printf("%sOK %-15s %s avg_down_speed: %7.2fkbps avg_up_speed: %7.4fkbps avg_down_latency: %6.2fms avg_up_latency: %6.2fms avg_down_jitter: %6.2fms avg_up_jitter: %4.2fms%s\n",
		utils.Colors.OKGREEN,
		res["ip"].(string),
		utils.Colors.OKBLUE,
		meanDownSpeed,
		meanuploadSpeed,
		meanDownLatency,
		meanuploadLatency,
		downMeanJitter,
		upMeanJitter,
		utils.Colors.ENDC,
	)
}

func InterimResultsWriter(res map[string]interface{}, ip string, downMeanJitter float64, upMeanJitter float64,
	meanDownSpeed float64, meanuploadSpeed float64,
	meanDownLatency float64, meanuploadLatency float64) {

	WriteResultToFile(res, ip, downMeanJitter, upMeanJitter, meanDownSpeed, meanuploadSpeed, meanDownLatency, meanuploadLatency)
}

func WriteResultToFile(res map[string]interface{}, ip string, downMeanJitter float64, upMeanJitter float64, meanDownSpeed float64, meanuploadSpeed float64, meanDownLatency float64, meanuploadLatency float64) {
	resParts := []interface{}{
		ip,
		meanDownSpeed, meanuploadSpeed,
		meanDownLatency, meanuploadLatency,
		downMeanJitter, upMeanJitter,
	}

	ip, ok := res["ip"].(string)
	if ok {
		for _, ip := range ip {
			resParts = append(resParts, ip)
		}
	}

	downSpeed, ok := res["download"].(map[string]interface{})["speed"].([]float64)
	if ok {
		for _, speed := range downSpeed {
			resParts = append(resParts, speed)
		}
	}

	uploadSpeed, ok := res["upload"].(map[string]interface{})["speed"].([]float64)
	if ok {
		for _, speed := range uploadSpeed {
			resParts = append(resParts, speed)
		}
	}

	downLatency, ok := res["download"].(map[string]interface{})["latency"].([]float64)
	if ok {
		for _, latency := range downLatency {
			resParts = append(resParts, latency)
		}
	}

	uploadLatency, ok := res["upload"].(map[string]interface{})["latency"].([]float64)
	if ok {
		for _, latency := range uploadLatency {
			resParts = append(resParts, latency)
		}
	}

	// Open the file for appending the results
	f, err := os.OpenFile(config.INTERIM_RESULTS_PATH, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		fmt.Printf("Failed to open file: %s\n", err)
		return
	}
	defer f.Close()

	// Write the result parts to the file
	w := csv.NewWriter(f)
	if err := w.Write(utils.StringifySlice(resParts)); err != nil {
		fmt.Printf("Failed to write to file: %s\n", err)
	}
	w.Flush()
}

func SaveResults(results [][]string, savePath string, sort bool) error {
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
