package scanner

import (
	"CFScanner/speedtest"
	utils "CFScanner/utils"
	"encoding/csv"
	"fmt"
	"log"
	"math"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

var (
	PROGRAMDIR           = filepath.Dir(os.Args[0])
	BINDIR               = filepath.Join(PROGRAMDIR, "..", "bin")
	CONFIGDIR            = filepath.Join(PROGRAMDIR, "..", "config")
	RESULTDIR            = filepath.Join(PROGRAMDIR, "..", "result")
	START_DT_STR         = time.Now().Format("15")
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
	Min_dl_speed         float64       // kilobytes per second
	Min_ul_speed         float64       // kilobytes per second
	Max_dl_time          float64       // seconds
	Max_ul_time          float64       // seconds
	Max_dl_latency       float64       // seconds
	Max_ul_latency       float64       // seconds
	Fronting_timeout     float64       // seconds
	Startprocess_timeout time.Duration // seconds
	N_tries              int
	No_vpn               bool
}

func Checkip(ip string, Config ConfigStruct) map[string]interface{} {
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
	// var proc fakeProcess
	// v2ray_config_path := createV2rayConfig(ip, Config)

	// for tryIdx := 0; tryIdx < Config.n_tries; tryIdx++ {
	// 	if !frontingTest(ip, time.Duration(Config.fronting_timeout)) {
	// 		return nil
	// 	}
	// }

	var proxies map[string]string
	// var proccess *exec.Cmd

	// var process *exec.Cmd
	// var err error
	// process, proxies, err = startV2RayService(v2ray_config_path, Config.startprocess_timeout)
	// // fmt.Println(proxies)
	// if err != nil {
	// 	fmt.Printf("%vERROR - %vCould not start v2ray service%v\n",
	// 		utils.Colors.FAIL, utils.Colors.WARNING, utils.Colors.ENDC)
	// 	log.Fatal(err)
	// 	return nil
	// }

	for tryIdx := 0; tryIdx < Config.N_tries; tryIdx++ {
		// check download speed
		var err error
		nBytes := Config.Min_dl_speed * 1000 * Config.Max_dl_time
		dlSpeed, dlLatency, err = speedtest.DownloadSpeedTest(int(nBytes), nil, time.Duration(Config.Max_dl_time))
		if err != nil {
			if strings.Contains(strings.ToLower(err.Error()), "download/upload too slow") {
				fmt.Printf("%vFAIL %v%15s Download too slow\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip)
			} else {
				fmt.Printf("%vFAIL %v%15s Download error%v\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
			}
			// process.Process.Kill()
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
				// process.Process.Kill()
				return nil
			}
		} else {
			fmt.Printf("%vFAIL %v%15s high download latency %.4f s > %.4f s%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, dlLatency, Config.Max_dl_latency, utils.Colors.ENDC)
			// process.Process.Kill()
			return nil
		}
		// upload speed test
		if Config.Do_upload_test {
			var err error
			nBytes := Config.Min_ul_speed * 1000 * Config.Max_ul_time
			upSpeed, upLatency, err = speedtest.UploadSpeedTest(int(nBytes), proxies, time.Duration(Config.Max_ul_time))
			if err != nil {
				fmt.Printf("%sFAIL %supload unknown error%s\n", utils.Colors.FAIL, utils.Colors.WARNING, utils.Colors.ENDC)
				// log.Fatal(err)
				// process.Process.Kill()
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
					// process.Process.Kill()
					return nil
				}
			} else {
				fmt.Printf("%sFAIL %s upload latency too high %v %s\n",
					utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
				// process.Process.Kill()
				return nil
			}
		}

		dltimeLatency := math.Round(dlLatency)
		uptimeLatency := math.Round(upLatency)
		fmt.Printf("%vOK Download: %.2fkBps , Upload: %.2fkbps , UP_Latency: %v , DL_Latency: %v , IP: %5s %v\n",
			utils.Colors.OKGREEN, utils.Float64ToKBps(dlSpeed), utils.Float64ToKBps(upSpeed), uptimeLatency, dltimeLatency, ip, utils.Colors.ENDC)

	}
	// process.Process.Kill()
	return result
}

func Scanner(testConfig *ConfigStruct, cidrList []string, threadsCount int) {
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
					writeResultToFile(res, downMeanJitter, upMeanJitter, meanDownSpeed, meanUpSpeed, meanDownLatency, meanUpLatency)
				}
			}
		}(batches[i])
	}

	wg.Wait()
}

func writeResultToFile(res map[string]interface{}, downMeanJitter float64, upMeanJitter float64, meanDownSpeed float64, meanUpSpeed float64, meanDownLatency float64, meanUpLatency float64) {
	resParts := []interface{}{
		meanDownSpeed, meanUpSpeed,
		meanDownLatency, meanUpLatency,
		downMeanJitter, upMeanJitter,
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
	f, err := os.OpenFile(INTERIM_RESULTS_PATH, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
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
