package speedtest

import (
	"fmt"
	"net/http"
	"strings"
	"time"
)

func DownloadSpeedTest(nBytes int, proxies map[string]string, timeout time.Duration) (float64, float64, error) {
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

func UploadSpeedTest(nBytes int, proxies map[string]string, timeout time.Duration) (float64, float64, error) {
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

// func FrontingTest(ip string, timeout time.Duration) bool {
// 	success := false
// 	client := &http.Client{
// 		Timeout: timeout * time.Second,
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
