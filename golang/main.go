package main

import (
	"bufio"
	"encoding/csv"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"math"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/spf13/cobra"
	"gonum.org/v1/gonum/stat"
)

var (
	SCRIPTDIR            = filepath.Dir(os.Args[0])
	BINDIR               = filepath.Join(SCRIPTDIR, "..", "bin")
	CONFIGDIR            = filepath.Join(SCRIPTDIR, "..", "config")
	RESULTDIR            = filepath.Join(SCRIPTDIR, "..", "result")
	START_DT_STR         = time.Now().Format("15")
	INTERIM_RESULTS_PATH = filepath.Join(RESULTDIR, START_DT_STR+"_result.csv")
)

type ConfigStruct struct {
	local_port           int
	address_port         string
	user_id              string
	ws_header_host       string
	ws_header_path       string
	sni                  string
	do_upload_test       bool
	min_dl_speed         float64       // kilobytes per second
	min_ul_speed         float64       // kilobytes per second
	max_dl_time          float64       // seconds
	max_ul_time          float64       // seconds
	max_dl_latency       float64       // seconds
	max_ul_latency       float64       // seconds
	fronting_timeout     float64       // seconds
	startprocess_timeout time.Duration // seconds
	n_tries              int
	no_vpn               bool
}

var config = ConfigStruct{
	local_port:           0,
	address_port:         "0",
	user_id:              "",
	ws_header_host:       "",
	ws_header_path:       "",
	sni:                  "",
	do_upload_test:       false,
	min_dl_speed:         50.0,
	min_ul_speed:         50.0,
	max_dl_time:          -2.0,
	max_ul_time:          -2.0,
	max_dl_latency:       -1.0,
	max_ul_latency:       -1.0,
	fronting_timeout:     -1.0,
	startprocess_timeout: -1.0,
	n_tries:              -1,
	no_vpn:               false,
}

// var v2rayTemplate = `{
//   "inbounds": [{
//     "port": PORTPORT,
//     "listen": "127.0.0.1",
//     "tag": "socks-inbound",
//     "protocol": "socks",
//     "settings": {
//       "auth": "noauth",
//       "udp": false,
//       "ip": "127.0.0.1"
//     },
//     "sniffing": {
//       "enabled": true,
//       "destOverride": ["http", "tls"]
//     }
//   }],
//   "outbounds": [
//     {
// 		"protocol": "vmess",
//     "settings": {
//       "vnext": [{
//         "address": "IP.IP.IP.IP",
//         "port": CFPORTCFPORT,
//         "users": [{"id": "IDID" }]
//       }]
//     },
// 		"streamSettings": {
//         "network": "ws",
//         "security": "tls",
//         "wsSettings": {
//             "headers": {
//                 "Host": "HOSTHOST"
//             },
//             "path": "ENDPOINTENDPOINT"
//         },
//         "tlsSettings": {
//             "serverName": "RANDOMHOST",
//             "allowInsecure": false
//         }
//     }
// 	}],
//   "other": {}
// }`

type _COLORS struct {
	OKBLUE  string
	OKGREEN string
	WARNING string
	FAIL    string
	ENDC    string
}

var colors = _COLORS{
	OKBLUE:  "\033[94m",
	OKGREEN: "\033[92m",
	WARNING: "\033[93m",
	FAIL:    "\033[91m",
	ENDC:    "\033[0m",
}

// func getFreePort() int {
// 	l, err := net.Listen("tcp", "localhost:0")
// 	if err != nil {
// 		log.Fatal(err)
// 	}
// 	defer l.Close()

// 	addr := l.Addr().(*net.TCPAddr)
// 	return addr.Port
// }

func createDir(dirPath string) {
	if _, err := os.Stat(dirPath); os.IsNotExist(err) {
		os.MkdirAll(dirPath, 0755)
		fmt.Printf("Directory created: %s\n", dirPath)
	}
}

// func waitForPort(host string, port int, timeout time.Duration) error {
// 	startTime := time.Now()
// 	for {
// 		conn, err := net.DialTimeout("tcp", fmt.Sprintf("%s:%d", host, port), timeout)
// 		if err == nil {
// 			conn.Close()
// 			return nil
// 		}
// 		if time.Since(startTime) >= timeout {
// 			return fmt.Errorf("waited too long for the port %d on host %s to start accepting connections", port, host)
// 		}
// 		time.Sleep(time.Millisecond * 10)
// 	}
// }

// func frontingTest(ip string, timeout time.Duration) bool {
// 	success := false
// 	client := &http.Client{
// 		Timeout: timeout,
// 		Transport: &http.Transport{
// 			TLSClientConfig: &tls.Config{
// 				ServerName:         "speed.cloudflare.com",
// 				InsecureSkipVerify: true,
// 			},
// 		},
// 	}
// 	req, err := http.NewRequest("GET", fmt.Sprintf("https://%s", ip), nil)
// 	if err != nil {
// 		fmt.Printf("Error creating request for IP %s: %v\n", ip, err)
// 		return success
// 	}
// 	req.Header.Set("Host", "speed.cloudflare.com")
// 	resp, err := client.Do(req)
// 	if err != nil {
// 		switch err.(type) {
// 		case net.Error:
// 			netErr := err.(net.Error)
// 			if netErr.Timeout() {
// 				fmt.Printf("Fronting test connect timeout for IP %s\n", ip)
// 			} else {
// 				fmt.Printf("Fronting test connection error for IP %s: %v\n", ip, err)
// 			}
// 		default:
// 			fmt.Printf("Fronting test unknown error for IP %s: %v\n", ip, err)
// 		}
// 		return success
// 	}
// 	defer resp.Body.Close()
// 	if resp.StatusCode != http.StatusOK {
// 		fmt.Printf("Fronting test error for IP %s: %d\n", ip, resp.StatusCode)
// 	} else {
// 		success = true
// 	}
// 	return success
// }

// func createV2rayConfig(edgeIP string, testConfig ConfigStruct) string {
// 	localPortStr := strconv.Itoa(getFreePort())
// 	config := strings.ReplaceAll(v2rayTemplate, "PORTPORT", localPortStr)
// 	config = strings.ReplaceAll(config, "IP.IP.IP.IP", edgeIP)
// 	config = strings.ReplaceAll(config, "CFPORTCFPORT", testConfig.address_port)
// 	config = strings.ReplaceAll(config, "IDID", testConfig.user_id)
// 	config = strings.ReplaceAll(config, "HOSTHOST", testConfig.ws_header_host)
// 	config = strings.ReplaceAll(config, "ENDPOINTENDPOINT", testConfig.ws_header_path)
// 	config = strings.ReplaceAll(config, "RANDOMHOST", testConfig.sni)

// 	configPath := fmt.Sprintf("%s/config-%s.json", CONFIGDIR, edgeIP)
// 	configFile, err := os.Create(configPath)
// 	if err != nil {
// 		log.Fatal(err)
// 	}
// 	defer configFile.Close()

// 	configFile.WriteString(config)

// 	return configPath
// }

// func downloadSpeedTest(nBytes int, proxies map[string]string, timeout time.Duration) (float64, float64, error) {
// 	startTime := time.Now()
// 	client := &http.Client{
// 		Timeout: time.Duration(timeout) * time.Second,
// 		Transport: &http.Transport{
// 			Proxy: http.ProxyFromEnvironment,
// 		},
// 	}
// 	req, err := http.NewRequest("GET", "https://speed.cloudflare.com/__down", nil)
// 	if err != nil {
// 		return 0, 0, fmt.Errorf("error creating request: %v", err)
// 	}
// 	fmt.Println(req.Body)
// 	q := req.URL.Query()
// 	q.Add("bytes", fmt.Sprintf("%d", nBytes))
// 	req.URL.RawQuery = q.Encode()

// 	for k, v := range proxies {
// 		req.Header.Set(k, v)
// 	}

// 	resp, err := client.Do(req)
// 	if err != nil {
// 		return 0, 0, fmt.Errorf("error sending request: %v", err)
// 	}

// 	defer resp.Body.Close()

// 	totalTime := time.Since(startTime).Seconds()

// 	cfTime := time.Duration(0)

// 	serverTiming := resp.Header.Get("Server-Timing")
// 	if serverTiming != "" {
// 		// parse cf timing from server-timing header
// 		for _, timing := range strings.Split(serverTiming, ",") {
// 			if strings.HasPrefix(timing, "cf") {
// 				cfTime, _ = time.ParseDuration(timing[3:])
// 			}
// 		}
// 	}
// 	cfRay := resp.Header.Get("CF-RAY")
// 	latency, err := time.ParseDuration(cfRay + "ms")
// 	fmt.Println(latency)
// 	if err != nil {
// 		return 0, 0, err
// 	}
// 	downloadTime := totalTime - cfTime.Seconds()
// 	mb := float64(nBytes*8) / (10 * 10 * 10 * 10 * 10 * 10)
// 	downloadSpeed := mb / downloadTime

// 	fmt.Println(downloadSpeed)
// 	return downloadSpeed, latency.Seconds(), nil
// }

func downloadSpeedTest(nBytes int, timeout time.Duration) (float64, float64, error) {
	startTime := time.Now()
	client := &http.Client{
		Timeout: timeout * time.Second,
		Transport: &http.Transport{
			Proxy: http.ProxyFromEnvironment,
		},
	}
	req, err := http.NewRequest("GET", "https://speed.cloudflare.com/__down", nil)
	if err != nil {
		return 0, 0, fmt.Errorf("error creating request: %v", err)
	}
	q := req.URL.Query()
	q.Add("bytes", fmt.Sprintf("%d", nBytes))
	req.URL.RawQuery = q.Encode()

	resp, err := client.Do(req)
	if err != nil {
		return 0, 0, fmt.Errorf("error sending request: %v", err)
	}

	defer resp.Body.Close()

	totalTime := time.Since(startTime).Seconds()

	cfTime := time.Duration(0)

	serverTiming := resp.Header.Get("Server-Timing")
	if serverTiming != "" {
		// parse cf timing from server-timing header
		for _, timing := range strings.Split(serverTiming, ",") {
			if strings.HasPrefix(timing, "cf") {
				cfTime, _ = time.ParseDuration(timing[3:])
			}
		}
	}
	// cfRay := resp.Header.Get("CF-RAY")
	// latency, err := time.ParseDuration(cfRay + "ms")
	// if err != nil {
	// 	return 0, 0, err
	// }
	downloadTime := totalTime - cfTime.Seconds()
	mb := float64(nBytes*8) / (10 * 10 * 10 * 10 * 10 * 10)
	downloadSpeed := mb / downloadTime

	return downloadSpeed, downloadTime, nil
}

func uploadSpeedTest(nBytes int, proxies map[string]string, timeout time.Duration) (float64, float64, error) {
	startTime := time.Now()
	req, err := http.NewRequest("POST", "https://speed.cloudflare.com/__up", strings.NewReader(strings.Repeat("0", nBytes)))
	if err != nil {
		return 0, 0, err
	}
	for k, v := range proxies {
		req.Header.Set(k, v)
	}
	client := &http.Client{Timeout: timeout * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return 0, 0, err
	}
	defer resp.Body.Close()
	totalTime := time.Since(startTime).Seconds()
	cfTime := time.Duration(0)
	serverTiming := resp.Header.Get("Server-Timing")
	if serverTiming != "" {
		// parse cf timing from server-timing header
		for _, timing := range strings.Split(serverTiming, ",") {
			if strings.HasPrefix(timing, "cf") {
				cfTime, _ = time.ParseDuration(timing[3:])
			}
		}
	}
	latency := totalTime - cfTime.Seconds()
	mb := float64(nBytes*8) / (10 * 10 * 10 * 10 * 10 * 10)
	uploadSpeed := mb / latency

	return uploadSpeed, latency, nil
}

// func raiseSpeedTimeout() {
// 	panic("Download/upload too slow!")
// }

// type fakeProcess struct{}

// func (fp fakeProcess) Kill() error {
// 	return syscall.Kill(syscall.Getpid(), syscall.SIGTERM)
// }

// func startV2RayService(v2rayConfPath string, timeout time.Duration) (*exec.Cmd, map[string]string, error) {
// 	v2rayConfFile, err := os.Open(v2rayConfPath)
// 	if err != nil {
// 		return nil, nil, err
// 	}
// 	defer v2rayConfFile.Close()

// 	var v2rayConf map[string]interface{}
// 	err = json.NewDecoder(v2rayConfFile).Decode(&v2rayConf)
// 	if err != nil {
// 		return nil, nil, err
// 	}

// 	v2rayListen := v2rayConf["inbounds"].([]interface{})[0].(map[string]interface{})["listen"].(string)
// 	v2rayPort := int(v2rayConf["inbounds"].([]interface{})[0].(map[string]interface{})["port"].(float64))

// 	fmt.Println(v2rayListen, v2rayPort)

// 	v2rayCmd := exec.Command(path.Join(BINDIR, "v2ray"), "run", v2rayConfPath)
// 	v2rayCmd.Stdout = nil
// 	v2rayCmd.Stderr = nil

// 	err = v2rayCmd.Start()
// 	if err != nil {
// 		return nil, nil, err
// 	}

// 	err = waitForPort(v2rayListen, v2rayPort, timeout)
// 	if err != nil {
// 		return nil, nil, err
// 	}

// 	proxies := map[string]string{
// 		"http":  fmt.Sprintf("socks5://%s:%d", v2rayListen, v2rayPort),
// 		"https": fmt.Sprintf("socks5://%s:%d", v2rayListen, v2rayPort),
// 	}

// 	return v2rayCmd, proxies, nil
// }

func checkip(ip string, Config ConfigStruct) map[string]interface{} {
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
	// fmt.Println(result)
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
	// 		colors.FAIL, colors.WARNING, colors.ENDC)
	// 	log.Fatal(err)
	// 	return nil
	// }

	for tryIdx := 0; tryIdx < Config.n_tries; tryIdx++ {
		// check download speed
		var err error
		nBytes := Config.min_dl_speed * 1000 * Config.max_dl_time
		dlSpeed, dlLatency, err = downloadSpeedTest(int(nBytes), time.Duration(Config.max_dl_time))
		if err != nil {
			if strings.Contains(strings.ToLower(err.Error()), "download/upload too slow") {
				fmt.Printf("%vFAIL %v%15s Download too slow\n",
					colors.FAIL, colors.WARNING, ip)
			} else {
				fmt.Printf("%vFAIL %v%15s Download error%v\n",
					colors.FAIL, colors.WARNING, ip, colors.ENDC)
			}
			// process.Process.Kill()
			return nil
		}

		if dlLatency <= Config.max_dl_latency {
			dlSpeedKBps := dlSpeed / 8 * 1000
			if dlSpeedKBps >= Config.min_dl_speed {
				result["download"].(map[string]interface{})["speed"] =
					append(result["download"].(map[string]interface{})["speed"].([]float64), dlSpeed)
				result["download"].(map[string]interface{})["latency"] =
					append(result["download"].(map[string]interface{})["latency"].([]int), int(math.Round(dlLatency)))

			} else {
				fmt.Printf("%vFAIL %v%15s download too slow %.4f kBps < %.4f kBps%v\n",
					colors.FAIL, colors.WARNING, ip, dlSpeedKBps, Config.min_dl_speed, colors.ENDC)
				// process.Process.Kill()
				return nil
			}
		} else {
			fmt.Printf("%vFAIL %v%15s high download latency %.4f s > %.4f s%v\n",
				colors.FAIL, colors.WARNING, ip, dlLatency, Config.max_dl_latency, colors.ENDC)
			// process.Process.Kill()
			return nil
		}
		// upload speed test
		if Config.do_upload_test {
			var err error
			nBytes := Config.min_ul_speed * 1000 * Config.max_ul_time
			upSpeed, upLatency, err = uploadSpeedTest(int(nBytes), proxies, time.Duration(Config.max_ul_time))
			if err != nil {
				fmt.Printf("%sFAIL %supload unknown error%s\n", colors.FAIL, colors.WARNING, colors.ENDC)
				// log.Fatal(err)
				// process.Process.Kill()
				return nil
			}
			if upLatency <= Config.max_ul_latency {
				upSpeedKbps := upSpeed / 8 * 1000
				if upSpeedKbps >= Config.min_ul_speed {
					result["upload"].(map[string]interface{})["speed"] =
						append(result["upload"].(map[string]interface{})["speed"].([]float64), upSpeed)
					result["upload"].(map[string]interface{})["latency"] =
						append(result["upload"].(map[string]interface{})["latency"].([]int), int(math.Round(upLatency)))

				} else {
					fmt.Printf("%sFAIL %s upload too slow %f kBps < %f kBps%s\n",
						colors.FAIL, colors.WARNING, upSpeedKbps, Config.min_ul_speed, colors.ENDC)
					// process.Process.Kill()
					return nil
				}
			} else {
				fmt.Printf("%sFAIL %s upload latency too high %v %s\n",
					colors.FAIL, colors.WARNING, ip, colors.ENDC)
				// process.Process.Kill()
				return nil
			}
		}

		dltimeLatency := math.Round(dlLatency)
		uptimeLatency := math.Round(upLatency)
		fmt.Printf("%vOK Download: %.2fkBps , Upload: %.2fkbps , UP_Latency: %v , DL_Latency: %v , IP: %5s %v\n",
			colors.OKGREEN, Float64ToKBps(dlSpeed), Float64ToKBps(upSpeed), uptimeLatency, dltimeLatency, ip, colors.ENDC)

	}
	// process.Process.Kill()
	return result
}

func createTestConfig(configPath string, startprocessTimeout time.Duration,
	doUploadTest bool, minDlSpeed float64,
	minUlSpeed float64, maxDlTime float64,
	maxUlTime float64, frontingTimeout float64,
	maxDlLatency float64, maxUlLatency float64,
	nTries int, noVpn bool) ConfigStruct {

	jsonFile, err := os.Open(configPath)
	if err != nil {
		log.Fatal(err)
	}
	defer jsonFile.Close()

	var jsonFileContent map[string]interface{}
	byteValue, _ := ioutil.ReadAll(jsonFile)
	json.Unmarshal(byteValue, &jsonFileContent)

	// proctimeout := int64(startprocessTimeout / int64(time.Millisecond))

	ConfigObject := ConfigStruct{
		user_id:              jsonFileContent["id"].(string),
		ws_header_host:       jsonFileContent["host"].(string),
		address_port:         jsonFileContent["port"].(string),
		sni:                  jsonFileContent["serverName"].(string),
		ws_header_path:       "/" + strings.TrimLeft(jsonFileContent["path"].(string), "/"),
		startprocess_timeout: startprocessTimeout,
		do_upload_test:       doUploadTest,
		min_dl_speed:         minDlSpeed,
		min_ul_speed:         minUlSpeed,
		max_dl_time:          maxDlTime,
		max_ul_time:          maxUlTime,
		fronting_timeout:     frontingTimeout,
		max_dl_latency:       maxDlLatency,
		max_ul_latency:       maxUlLatency,
		n_tries:              nTries,
		no_vpn:               noVpn,
	}
	fmt.Println("Config :", "\n", "User ID :", colors.OKBLUE, ConfigObject.user_id, colors.ENDC, "\n",
		"WS Header Host:", colors.OKBLUE, ConfigObject.ws_header_host, colors.ENDC, "\n",
		"WS Header Path : ", colors.OKBLUE, ConfigObject.ws_header_path, colors.ENDC, "\n",
		"Address Port :", colors.OKBLUE, ConfigObject.address_port, colors.ENDC, "\n",
		"SNI :", colors.OKBLUE, ConfigObject.sni, colors.ENDC, "\n",
		"Start Proccess Timeout :", colors.OKBLUE, ConfigObject.startprocess_timeout, colors.ENDC, "\n",
		"Upload Test :", colors.OKBLUE, ConfigObject.do_upload_test, colors.ENDC, "\n",
		"Minimum Download Speed :", colors.OKBLUE, ConfigObject.min_dl_speed, colors.ENDC, "\n",
		"Maximum Download Time :", colors.OKBLUE, ConfigObject.max_dl_time, colors.ENDC, "\n",
		"Minimum Upload Speed :", colors.OKBLUE, ConfigObject.min_ul_speed, colors.ENDC, "\n",
		"Maximum Upload Time :", colors.OKBLUE, ConfigObject.max_ul_time, colors.ENDC, "\n",
		"Fronting Timeout :", colors.OKBLUE, ConfigObject.fronting_timeout, colors.ENDC, "\n",
		"Maximum Download Latency :", colors.OKBLUE, ConfigObject.max_dl_latency, colors.ENDC, "\n",
		"Maximum Upload Latency :", colors.OKBLUE, ConfigObject.max_ul_latency, colors.ENDC, "\n",
		"Number of Tries :", colors.OKBLUE, ConfigObject.n_tries, colors.ENDC, "\n",
		"No VPN Mode :", colors.OKBLUE, ConfigObject.no_vpn, colors.ENDC)

	return ConfigObject
}

func createInterimResultsFile(interimResultsPath string, nTries int) error {
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

// Stat module
func meanJitter(latencies []float64) float64 {
	if len(latencies) <= 1 {
		return 0
	}
	jitters := make([]float64, len(latencies)-1)
	for i := 1; i < len(latencies); i++ {
		jitters[i-1] = math.Abs(latencies[i] - latencies[i-1])
	}
	return stat.Mean(jitters, nil)
}

func getNumIPsInCIDR(cidr string) int {
	parts := strings.Split(cidr, "/")

	subnetMask := 32
	if len(parts) > 1 {
		mask, err := strconv.Atoi(parts[1])
		if err == nil {
			subnetMask = mask
		}
	}

	numIPs := 1 << uint(32-subnetMask)

	return numIPs
}

// func timeDurationToInt(n time.Duration) int64 {
// 	ms := int64(n / time.Millisecond)
// 	return ms
// }

func cidrToIPList(cidr string) ([]string, error) {
	ip, ipNet, err := net.ParseCIDR(cidr)
	if err != nil {
		return nil, err
	}

	var ips []string
	for ip := ip.Mask(ipNet.Mask); ipNet.Contains(ip); inc(ip) {
		ips = append(ips, ip.String())
	}
	return ips, nil
}

func inc(ip net.IP) {
	for j := len(ip) - 1; j >= 0; j-- {
		ip[j]++
		if ip[j] > 0 {
			break
		}
	}
}

// main
func checkCIDRs(testConfig *ConfigStruct, cidrList []string, threadsCount int) {
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
				res := checkip(ip, *testConfig)

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

					downMeanJitter := meanJitter(downLatency)

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
					if testConfig.do_upload_test && ok {
						upMeanJitter = meanJitter(upLatency)
					}

					downSpeed, ok := res["download"].(map[string]interface{})["speed"].([]float64)

					// make downSpeedKbps to return kbps
					downSpeedKbps := make([]float64, len(downSpeed))
					for i, v := range downSpeed {
						downSpeedKbps[i] = Float64ToKBps(v)
					}
					if !ok {
						log.Printf("Error getting download speed for IP %s , %v", ip, ok)
						continue
					}
					meanDownSpeed := mean(downSpeedKbps)
					meanUpSpeed := -1.0

					upSpeed, ok := res["upload"].(map[string]interface{})["speed"].([]float64)

					if !ok {
						log.Printf("Error getting upload speed for IP %s", ip)
						continue
					}

					// make downSpeedKbps to return kbps
					upSpeedKbps := make([]float64, len(upSpeed))
					for i, v := range upSpeed {
						upSpeedKbps[i] = Float64ToKBps(v)
					}
					if testConfig.do_upload_test {
						meanUpSpeed = mean(upSpeedKbps)
					}

					meanDownLatency := mean(downLatency)
					meanUpLatency := -1.0
					if testConfig.do_upload_test {
						meanUpLatency = mean(upLatency)
					}

					fmt.Printf("%sOK %-15s %savg_down_speed: %.2fkbps avg_up_speed: %.2fkbps avg_down_latency: %6.2fms avg_up_latency: %6.2fms avg_down_jitter: %6.2fms avg_up_jitter: %4.2fms%s\n",
						colors.OKGREEN,
						res["ip"].(string),
						colors.OKBLUE,
						meanDownSpeed,
						meanUpSpeed,
						meanDownLatency,
						meanUpLatency,
						downMeanJitter,
						upMeanJitter,
						colors.ENDC,
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
	if err := w.Write(stringifySlice(resParts)); err != nil {
		fmt.Printf("Failed to write to file: %s\n", err)
	}
	w.Flush()
}

// Helper function to convert a slice of interfaces to a slice of strings
func stringifySlice(s []interface{}) []string {
	out := make([]string, len(s))
	for i, v := range s {
		out[i] = fmt.Sprintf("%v", v)
	}
	return out
}

func mean(latencies []float64) float64 {
	if len(latencies) == 0 {
		return 0
	}
	var sum float64
	for _, x := range latencies {
		sum += x
	}
	return sum / float64(len(latencies))
}

func Float64ToKBps(bytes float64) float64 {
	return bytes / 8 * 1000
}

var (
	version  = "0.3"
	build    = "Custom"
	codename = "CFScanner , CloudFlare Scanner."
)

func Version() string {
	return version
}

// VersionStatement returns a list of strings representing the full version info.
func VersionStatement() string {
	return strings.Join([]string{
		"CFScanner ", Version(), " (", codename, ") ", build, " (", runtime.Version(), " ", runtime.GOOS, "/", runtime.GOARCH, ")",
	}, "")
}

func main() {
	var threads int
	var configPath string
	var noVpn bool
	var subnetsPath string
	var doUploadTest bool
	var nTries int
	var minDLSpeed float64
	var minULSpeed float64
	var maxDLTime float64
	var maxULTime float64

	var startProcessTimeout int
	var frontingTimeout float64
	var maxDLLatency float64
	var maxULLatency float64

	var bigIPList []string

	rootCmd := &cobra.Command{
		Use:   os.Args[0],
		Short: codename,
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println(VersionStatement())
			if !noVpn {
				createDir(CONFIGDIR)
				// configfilepath := configPath
			}
			createDir(RESULTDIR)
			if err := createInterimResultsFile(INTERIM_RESULTS_PATH, nTries); err != nil {
				fmt.Printf("Error creating interim results file: %v\n", err)
			}
			threadsCount := threads

			var cidrList []string
			if subnetsPath != "" {
				subnetFilePath := subnetsPath
				subnetFile, err := os.Open(subnetFilePath)
				if err != nil {
					panic(err)
				}
				defer subnetFile.Close()

				scanner := bufio.NewScanner(subnetFile)
				for scanner.Scan() {
					cidrList = append(cidrList, strings.TrimSpace(scanner.Text()))
				}
				if err := scanner.Err(); err != nil {
					panic(err)
				}
				// } else {
				// 	cidrList = readCidrsFromAsnLookup()
				// }
			}
			testConfig := createTestConfig(configPath, time.Duration(startProcessTimeout), doUploadTest,
				minDLSpeed, minULSpeed, maxDLTime, maxULTime,
				frontingTimeout, maxDLLatency, maxULLatency,
				nTries, noVpn)

			var nTotalIPs int

			for _, cidr := range cidrList {
				numIPs := getNumIPsInCIDR(cidr)
				nTotalIPs += numIPs
			}

			for _, cidr := range cidrList {
				ips, err := cidrToIPList(cidr)
				if err != nil {
					log.Print("Error : ", err)
				}
				bigIPList = append(bigIPList, ips...)
			}

			fmt.Println("---------------------------")
			fmt.Println("Total Threads : ", threads)
			fmt.Printf("Starting to scan %d IPS.\n", nTotalIPs)
			checkCIDRs(&testConfig, bigIPList, threadsCount)
			fmt.Println("Results Written in :", INTERIM_RESULTS_PATH)
		},
	}
	rootCmd.PersistentFlags().IntVar(&threads, "threads", 1, "Number of threads to use for parallel computing")
	rootCmd.PersistentFlags().StringVarP(&configPath, "config", "c", "", "The path to the config file. For confg file example, see https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/ClientConfig.json")
	rootCmd.PersistentFlags().BoolVar(&noVpn, "novpn", false, "If passed, test without creating vpn connections")
	rootCmd.PersistentFlags().StringVar(&subnetsPath, "subnets", "", "(optional) The path to the custom subnets file. each line should be in the form of ip.ip.ip.ip/subnet_mask or ip.ip.ip.ip. If not provided, the program will read the cidrs from asn lookup")
	rootCmd.PersistentFlags().BoolVar(&doUploadTest, "upload-test", false, "If True, upload test will be conducted")
	rootCmd.PersistentFlags().IntVar(&nTries, "tries", 1, "Number of times to try each IP. An IP is marked as OK if all tries are successful")
	rootCmd.PersistentFlags().Float64Var(&minDLSpeed, "download-speed", 50, "Minimum acceptable download speed in kilobytes per second")
	rootCmd.PersistentFlags().Float64Var(&minULSpeed, "upload-speed", 50, "Maximum acceptable upload speed in kilobytes per second")
	rootCmd.PersistentFlags().Float64Var(&maxDLTime, "download-time", 2, "Maximum (effective, excluding http time) time to spend for each download")
	rootCmd.PersistentFlags().Float64Var(&maxULTime, "upload-time", 2, "Maximum (effective, excluding http time) time to spend for each upload")
	rootCmd.PersistentFlags().Float64Var(&frontingTimeout, "fronting-timeout", 1.0, "Maximum time to wait for fronting response")
	rootCmd.PersistentFlags().Float64Var(&maxDLLatency, "download-latency", 2.0, "Maximum allowed latency for download")
	rootCmd.PersistentFlags().Float64Var(&maxULLatency, "upload-latency", 2.0, "Maximum allowed latency for download")
	rootCmd.PersistentFlags().IntVar(&startProcessTimeout, "startprocess-timeout", 5, "")

	err := rootCmd.Execute()
	cobra.CheckErr(err)

}
