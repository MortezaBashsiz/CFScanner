package speedtest

import (
	utils "CFScanner/utils"
	"crypto/tls"
	"fmt"
	"log"
	"net"
	"net/http"
	"strings"
	"time"
)

// conducts a fronting test on an ip and return true if status 200 is received
func FrontingTest(ip string, timeout time.Duration) bool {

	var success bool = false

	compatibleIP := ip
	if strings.Contains(ip, ":") {
		compatibleIP = fmt.Sprintf("[%s]", ip)
	} else {
		compatibleIP = ip
	}

	req, err := http.NewRequest("GET", fmt.Sprintf("https://%s", compatibleIP), nil)
	if err != nil {
		fmt.Printf("Error creating request for IP %s: %v\n", ip, err)
		return success
	}
	req.Host = "speed.cloudflare.com"
	client := &http.Client{
		Timeout: timeout,
		Transport: &http.Transport{
			Proxy: http.ProxyFromEnvironment,
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
		return success
	}

	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		log.Printf("%vFAIL%v %v%s Fronting test error : %d%v\n",
			utils.Colors.FAIL, utils.Colors.ENDC, utils.Colors.WARNING, ip, resp.StatusCode, utils.Colors.ENDC)
	} else {
		success = true
	}

	return success
}
