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

func Checkip(ip string, Config config.ConfigStruct) map[string]interface{} {
	var dlSpeed float64
	var dlLatency float64
	var upSpeed float64
	var upLatency float64

	result := map[string]interface{}{
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

	var proxies map[string]string
	var process *exec.Cmd
	if Config.Vpn {
		v2ray_config_path := v2raysvc.CreateV2rayConfig(ip, Config)
		var err error
		process, proxies, err = v2raysvc.StartV2RayService(v2ray_config_path, time.Duration(Config.Startprocess_timeout))
		if err != nil {
			fmt.Printf("%vERROR - %vCould not start v2ray service%v\n",
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
		nBytes := Config.Min_dl_speed * 1000 * Config.Max_dl_time
		dlSpeed, dlLatency, err = speedtest.DownloadSpeedTest(int(nBytes), proxies, time.Duration(Config.Max_dl_time))
		if err != nil {
			if strings.Contains(strings.ToLower(err.Error()), "download/upload too slow") {
				fmt.Printf("%vFAIL %v%15s Download too slow\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip)
			} else {
				fmt.Printf("%vFAIL %v%15s Download error%v\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
			}
			if Config.Vpn {
				process.Process.Kill()
			}
			return nil
		}

		if dlLatency <= Config.Max_dl_latency {
			dlSpeedKBps := dlSpeed / 8 * 1000
			if dlSpeedKBps >= Config.Min_dl_speed {
				result["download"].(map[string]interface{})["speed"] =
					append(result["download"].(map[string]interface{})["speed"].([]float64), dlSpeed)
				result["download"].(map[string]interface{})["latency"] =
					append(result["download"].(map[string]interface{})["latency"].([]int), int(math.Round(dlLatency)))

			} else {
				fmt.Printf("%vFAIL %v%15s download too slow %.4f kBps < %.4f kBps%v\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, dlSpeedKBps, Config.Min_dl_speed, utils.Colors.ENDC)
				if Config.Vpn {
					process.Process.Kill()
				}
				return nil
			}
		} else {
			fmt.Printf("%vFAIL %v%15s high download latency %.4f s > %.4f s%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, dlLatency, Config.Max_dl_latency, utils.Colors.ENDC)
			if Config.Vpn {
				process.Process.Kill()
			}
			return nil
		}
		// upload speed test
		if Config.Do_upload_test {
			var err error
			nBytes := Config.Min_ul_speed * 1000 * Config.Max_ul_time
			upSpeed, upLatency, err = speedtest.UploadSpeedTest(int(nBytes), proxies, time.Duration(Config.Max_ul_time))
			if err != nil {
				fmt.Println(err)
				fmt.Printf("%sFAIL %supload unknown error%s\n", utils.Colors.FAIL, utils.Colors.WARNING, utils.Colors.ENDC)
				if Config.Vpn {
					process.Process.Kill()
				}
				return nil
			}
			if upLatency <= Config.Max_ul_latency {
				upSpeedKbps := upSpeed / 8 * 1000
				if upSpeedKbps >= Config.Min_ul_speed {
					result["upload"].(map[string]interface{})["speed"] =
						append(result["upload"].(map[string]interface{})["speed"].([]float64), upSpeed)
					result["upload"].(map[string]interface{})["latency"] =
						append(result["upload"].(map[string]interface{})["latency"].([]int), int(math.Round(upLatency)))

				} else {
					fmt.Printf("%sFAIL %s upload too slow %f kBps < %f kBps%s\n",
						utils.Colors.FAIL, utils.Colors.WARNING, upSpeedKbps, Config.Min_ul_speed, utils.Colors.ENDC)
					if Config.Vpn {
						process.Process.Kill()
					}
					return nil
				}
			} else {
				fmt.Printf("%sFAIL %s upload latency too high %v %s\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
				if Config.Vpn {
					process.Process.Kill()
				}
				return nil
			}
		}

		dltimeLatency := math.Round(dlLatency)
		uptimeLatency := math.Round(upLatency)
		fmt.Printf("%vOK IP: %v , Download: %.2fkBps , Upload: %.2fkbps , UP_Latency: %v , DL_Latency: %v%v\n",
			utils.Colors.OKGREEN, ip, utils.Float64ToKBps(dlSpeed), utils.Float64ToKBps(upSpeed), uptimeLatency, dltimeLatency, utils.Colors.ENDC)
		if Config.Vpn {
			process.Process.Kill()
		}

	}
	if Config.Vpn {
		process.Process.Kill()
	}
	return result
}

func Scanner(testConfig *config.ConfigStruct, cidrList []string, threadsCount int) {
	var wg sync.WaitGroup

	n := len(cidrList)
	batchSize := len(cidrList) / threadsCount
	batches := make([][]string, threadsCount)
	results := [][]string{}

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
				res := Checkip(ip, *testConfig)

				if res != nil {
					downLatencyInt, ok := res["download"].(map[string]interface{})["latency"].([]int)

					if !ok {
						log.Printf("Error getting download latency for IP %s", ip)
						continue
					}

					// make downLatencyInt to float64
					downLatency := make([]float64, len(downLatencyInt))
					for i, v := range downLatencyInt {
						downLatency[i] = float64(v)
					}

					downMeanJitter := utils.MeanJitter(downLatency)

					upLatencyInt, ok := res["upload"].(map[string]interface{})["latency"].([]int)

					if !ok {
						log.Printf("Error getting upload latency for IP %s", ip)
						continue
					}

					// make upLatencyInt to float64
					upLatency := make([]float64, len(upLatencyInt))
					for i, v := range upLatencyInt {
						upLatency[i] = float64(v)
					}

					upMeanJitter := -1.0
					if testConfig.Do_upload_test && ok {
						upMeanJitter = utils.MeanJitter(upLatency)
					}

					downSpeed, ok := res["download"].(map[string]interface{})["speed"].([]float64)

					// make downSpeedKbps to return kbps
					downSpeedKbps := make([]float64, len(downSpeed))
					for i, v := range downSpeed {
						downSpeedKbps[i] = utils.Float64ToKBps(v)
					}
					if !ok {
						log.Printf("Error getting download speed for IP %s , %v", ip, ok)
						continue
					}
					meanDownSpeed := utils.Mean(downSpeedKbps)
					meanUpSpeed := -1.0

					upSpeed, ok := res["upload"].(map[string]interface{})["speed"].([]float64)

					if !ok {
						log.Printf("Error getting upload speed for IP %s", ip)
						continue
					}

					// make downSpeedKbps to return kbps
					upSpeedKbps := make([]float64, len(upSpeed))
					for i, v := range upSpeed {
						upSpeedKbps[i] = utils.Float64ToKBps(v)
					}
					if testConfig.Do_upload_test {
						meanUpSpeed = utils.Mean(upSpeedKbps)
					}

					meanDownLatency := utils.Mean(downLatency)
					meanUpLatency := -1.0
					if testConfig.Do_upload_test {
						meanUpLatency = utils.Mean(upLatency)
					}

					// change download latency to string for using it with saveresults func
					var latencystring string

					for _, f := range downLatencyInt {
						latencystring = fmt.Sprintf("%d", f)
					}

					results = append(results, []string{latencystring, ip})

					fmt.Printf("%sOK %-15s %savg_down_speed: %.2fkbps avg_up_speed: %.2fkbps avg_down_latency: %6.2fms avg_up_latency: %6.2fms avg_down_jitter: %6.2fms avg_up_jitter: %4.2fms%s\n",
						utils.Colors.OKGREEN,
						res["ip"].(string),
						utils.Colors.OKBLUE,
						meanDownSpeed,
						meanUpSpeed,
						meanDownLatency,
						meanUpLatency,
						downMeanJitter,
						upMeanJitter,
						utils.Colors.ENDC,
					)

					WriteResultToFile(res, ip, downMeanJitter, upMeanJitter, meanDownSpeed, meanUpSpeed, meanDownLatency, meanUpLatency)
					SaveResults(results, config.FINAL_RESULTS_PATH_SORTED, true)
				}
			}
		}(batches[i])
	}
	wg.Wait()
}

func WriteResultToFile(res map[string]interface{}, ip string, downMeanJitter float64, upMeanJitter float64, meanDownSpeed float64, meanUpSpeed float64, meanDownLatency float64, meanUpLatency float64) {
	resParts := []interface{}{
		ip,
		meanDownSpeed, meanUpSpeed,
		meanDownLatency, meanUpLatency,
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

	upSpeed, ok := res["upload"].(map[string]interface{})["speed"].([]float64)
	if ok {
		for _, speed := range upSpeed {
			resParts = append(resParts, speed)
		}
	}

	downLatency, ok := res["download"].(map[string]interface{})["latency"].([]float64)
	if ok {
		for _, latency := range downLatency {
			resParts = append(resParts, latency)
		}
	}

	upLatency, ok := res["upload"].(map[string]interface{})["latency"].([]float64)
	if ok {
		for _, latency := range upLatency {
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
