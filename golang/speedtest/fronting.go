package speedtest

import (
	utils "CFScanner/utils"
	"crypto/tls"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"net/url"
	"strings"
	"time"
)

// FrontingTest conducts a fronting test on an ip and return true if status 200 is received
func FrontingTest(ip string, proxies map[string]string, timeout time.Duration) bool {

	var success = false

	// set proxy to request
	var proxy *url.URL
	// set proxies to req header
	for _, v := range proxies {
		proxy, _ = url.Parse(v)
	}

	compatibleIP := ip
	if strings.Contains(ip, ":") {
		compatibleIP = fmt.Sprintf("[%s]", ip)
	} else {
		compatibleIP = ip
	}

	req, err := http.NewRequest("GET", fmt.Sprintf("https://%s", compatibleIP), nil)
	if err != nil {
		fmt.Printf("Error creating request for IP %s: %v\n", ip, err)
		return false
	}
	req.Host = "speed.cloudflare.com"

	client := &http.Client{
		Timeout: timeout * time.Second,
		Transport: &http.Transport{Proxy: http.ProxyURL(proxy),
			TLSClientConfig: &tls.Config{
				ServerName:         "speed.cloudflare.com",
				InsecureSkipVerify: true,
			},
		},
	}

	resp, err := client.Do(req)

	if err != nil {
		switch err := err.(type) {
		case net.Error:
			if err.Timeout() {
				log.Printf("%vFAIL%v %v%15s Fronting test connect timeout%v\n",
					utils.Colors.FAIL, utils.Colors.ENDC, utils.Colors.WARNING, ip, utils.Colors.ENDC)
			} else {
				log.Printf("%vFAIL%v %v%15s Fronting test connection error: %v%v\n",
					utils.Colors.FAIL, utils.Colors.ENDC, utils.Colors.WARNING, ip, err, utils.Colors.ENDC)
			}
		default:
			fmt.Printf("%vFAIL%v %v%15s Fronting test unknown error: %v%v\n",
				utils.Colors.FAIL, utils.Colors.ENDC, utils.Colors.WARNING, ip, err, utils.Colors.ENDC)
		}
		return false
	}

	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			fmt.Errorf("error occured when closing fronting body %v", err)
		}
	}(resp.Body)

	if resp.StatusCode != http.StatusOK {
		log.Printf("%vFAIL%v %v%s Fronting test error : %d%v\n",
			utils.Colors.FAIL, utils.Colors.ENDC, utils.Colors.WARNING, ip, resp.StatusCode, utils.Colors.ENDC)
	} else {
		success = true
	}

	return success
}
