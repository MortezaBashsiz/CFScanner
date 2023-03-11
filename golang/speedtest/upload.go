package speedtest

import (
	"net/http"
	"strings"
	"time"
)

// conducts a upload speed test on ip and returns upload speed and upload latency
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
