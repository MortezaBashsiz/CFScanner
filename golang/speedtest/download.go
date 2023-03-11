package speedtest

import (
	"fmt"
	"net/http"
	"strings"
	"time"
)

// conducts a download speed test on ip and returns download speed and download latency
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
