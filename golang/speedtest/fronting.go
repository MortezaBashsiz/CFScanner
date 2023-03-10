package speedtest

import (
	"crypto/tls"
	"fmt"
	"net"
	"net/http"
	"time"
)

// conducts a fronting test on an ip and return true if status 200 is received
func FrontingTest(ip string, timeout time.Duration) bool {

	client := &http.Client{
		Timeout: timeout * time.Second,
		Transport: &http.Transport{
			TLSClientConfig: &tls.Config{
				ServerName:         "speed.cloudflare.com",
				InsecureSkipVerify: true,
			},
		},
	}

	var success bool = false

	req, err := http.NewRequest("GET", fmt.Sprintf("https://%s", ip), nil)
	if err != nil {
		fmt.Printf("Error creating request for IP %s: %v\n", ip, err)
		return success
	}
	req.Header.Set("Host", "speed.cloudflare.com")
	resp, err := client.Do(req)

	if err != nil {
		switch err := err.(type) {
		case net.Error:
			if err.Timeout() {
				fmt.Printf("Fronting test connect timeout for IP %s\n", ip)
			} else {
				fmt.Printf("Fronting test connection error for IP %s: %v\n", ip, err)
			}
		default:
			fmt.Printf("Fronting test unknown error for IP %s: %v\n", ip, err)
		}
		return success
	}

	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		fmt.Printf("Fronting test error for IP %s: %d\n", ip, resp.StatusCode)
	} else {
		success = true
	}

	return success
}
