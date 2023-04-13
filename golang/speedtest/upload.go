package speedtest

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
)

// UploadSpeedTest conducts a upload speed test on ip and returns upload speed and upload latency
func UploadSpeedTest(nBytes int, proxies map[string]string, timeout time.Duration) (float64, float64, error) {
	startTime := time.Now()
	req, err := http.NewRequest("POST", "https://speed.cloudflare.com/__up", strings.NewReader(strings.Repeat("0", nBytes)))
	if err != nil {
		return 0, 0, err
	}

	// set proxy to request
	var proxy *url.URL
	// set proxies to req header
	for _, v := range proxies {
		proxy, _ = url.Parse(v)
	}

	client := &http.Client{
		Timeout:   timeout * time.Second,
		Transport: &http.Transport{Proxy: http.ProxyURL(proxy)}}

	resp, err := client.Do(req)
	if err != nil {
		return 0, 0, err
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			fmt.Errorf("error occured when closing upload body %v", err)
		}
	}(resp.Body)

	totalTime := time.Since(startTime).Seconds()
	cfTime := float64(0)

	serverTiming := req.Header.Get("Server-Timing")
	if serverTiming != "" {
		timings := strings.Split(serverTiming, "=")
		if len(timings) > 1 {
			cfTiming, err := strconv.ParseFloat(timings[1], 64)
			if err == nil {
				cfTime = cfTiming / 1000.0
				fmt.Println(cfTime)
			}
		}

	}
	latency := totalTime - cfTime
	var mb float64 = float64(nBytes) * 8 / (1000000.0)
	uploadSpeed := mb / latency

	return uploadSpeed, latency, nil
}
