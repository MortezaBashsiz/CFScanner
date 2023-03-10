package utils

import (
	"fmt"
	"log"
	"math"
	"net"
	"os"
	"strconv"
	"strings"

	"gonum.org/v1/gonum/stat"
)

func Mean(latencies []float64) float64 {
	if len(latencies) == 0 {
		return 0
	}
	var sum float64
	for _, x := range latencies {
		sum += x
	}
	return sum / float64(len(latencies))
}

func MeanJitter(latencies []float64) float64 {
	if len(latencies) <= 1 {
		return 0
	}
	jitters := make([]float64, len(latencies)-1)
	for i := 1; i < len(latencies); i++ {
		jitters[i-1] = math.Abs(latencies[i] - latencies[i-1])
	}
	return stat.Mean(jitters, nil)
}

func Float64ToKBps(bytes float64) float64 {
	return bytes / 8 * 1000
}

func CreateDir(dirPath string) {
	if _, err := os.Stat(dirPath); os.IsNotExist(err) {
		os.MkdirAll(dirPath, 0755)
		fmt.Printf("Directory created: %s\n", dirPath)
	}
}

// Helper function to convert a slice of interfaces to a slice of strings
func StringifySlice(s []interface{}) []string {
	out := make([]string, len(s))
	for i, v := range s {
		out[i] = fmt.Sprintf("%v", v)
	}
	return out
}

func GetNumIPsInCIDR(cidr string) int {
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

func IPParser(list []string) []string {
	var IPList []string
	for _, ip := range list {
		if strings.Contains(ip, "/") {
			ips, err := cidrToIPList(ip)
			if err != nil {
				log.Print("Error : ", err)
			}
			IPList = append(IPList, ips...)
		} else if net.ParseIP(ip) != nil {
			IPList = append(IPList, ip)
		}
	}
	return IPList
}

func inc(ip net.IP) {
	for j := len(ip) - 1; j >= 0; j-- {
		ip[j]++
		if ip[j] > 0 {
			break
		}
	}
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

// func timeDurationToInt(n time.Duration) int64 {
// 	ms := int64(n / time.Millisecond)
// 	return ms
// }

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
