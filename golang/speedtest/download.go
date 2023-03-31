package speedtest

import (
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// conducts a download speed test on ip and returns download speed and download latency
func DownloadSpeedTest(nBytes int, proxies map[string]string, timeout time.Duration) (float64, float64, error) {
	startTime := time.Now()

	// Create byte slice of nBytes
	data := make([]byte, nBytes)

	// Create request
	req, err := http.NewRequest("GET", "https://speed.cloudflare.com/__down", nil)
	if err != nil {
		return 0, 0, fmt.Errorf("error creating request: %v", err)
	}

	// Add bytes query parameter
	q := req.URL.Query()
	q.Add("bytes", strconv.Itoa(nBytes))
	req.URL.RawQuery = q.Encode()

	// Set up client
	client := &http.Client{
		Timeout: timeout,
		Transport: &http.Transport{
			Proxy: http.ProxyFromEnvironment,
		},
	}

	// Send request and write response to data slice
	resp, err := client.Do(req)
	if err != nil {
		return 0, 0, fmt.Errorf("error sending request: %v", err)
	}
	defer resp.Body.Close()

	_, err = io.ReadFull(resp.Body, data)
	if err != nil {
		return 0, 0, fmt.Errorf("error reading response body: %v", err)
	}

	// Calculate download time and speed
	totalTime := time.Since(startTime).Seconds()
	cfTime := float64(0)
	serverTiming := resp.Header.Get("Server-Timing")
	if serverTiming != "" {
		timings := strings.Split(serverTiming, "=")
		if len(timings) > 1 {
			cfTiming, err := strconv.ParseFloat(timings[1], 64)
			if err == nil {
				cfTime = cfTiming / 1000.0
			}
		}
	}
	downloadTime := totalTime - cfTime
	downloadSpeed := float64(nBytes) * 8 / (downloadTime * 1000000)

	return downloadSpeed, downloadTime, nil
}
