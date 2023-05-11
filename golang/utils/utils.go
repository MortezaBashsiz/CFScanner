package utils

import (
	"context"
	"fmt"
	"log"
	"math"
	"net"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

func Float64ToKBps(bytes float64) float64 {
	return bytes * 8 / (1000000.0)
}

func Round(val float64, roundOn float64, places int) (newVal float64) {
	var round float64
	pow := math.Pow(10, float64(places))
	digit := pow * val
	_, div := math.Modf(digit)
	if div >= roundOn {
		round = math.Ceil(digit)
	} else {
		round = math.Floor(digit)
	}
	newVal = round / pow
	return
}

func CreateDir(dirPath string) {
	dirPath = filepath.FromSlash(dirPath)
	if _, err := os.Stat(dirPath); os.IsNotExist(err) {
		err := os.MkdirAll(dirPath, 0755)
		if err != nil {
			return
		}
		fmt.Printf("Directory created: %s\n", dirPath)
	}
}

// StringifySlice Helper function to convert a slice of interfaces to a slice of strings
func StringifySlice(s []interface{}) []string {
	out := make([]string, len(s))
	for i, v := range s {
		out[i] = fmt.Sprintf("%v", v)
	}
	return out
}

func isDomainName(str string) (bool, error) {
	// Normalize input by adding "http://" if it is missing
	if !strings.HasPrefix(str, "http://") && !strings.HasPrefix(str, "https://") {
		str = "http://" + str
	}
	// Parse URL to extract host name
	u, err := url.Parse(str)
	if err != nil {
		return false, err
	}
	_, err = net.LookupHost(u.Hostname())

	if err != nil {
		return false, err
	}
	return true, nil
}

func GetIpFromDomain(domain string) (string, error) {
	var DomainError error
	var DomainName bool

	DomainName, DomainError = isDomainName(domain)
	if !DomainName {
		return "", DomainError
	}

	ip, err := getIPFromDomainTimeout(domain)
	if err != nil {
		log.Printf("%vFail Ns Lookup IP %v%15s%v\n",
			Colors.FAIL, Colors.OKBLUE, err.Error(), Colors.ENDC)
		return "", err
	}

	return ip, nil
}

func getIPFromDomainTimeout(domain string) (string, error) {
	resolver := net.Resolver{
		PreferGo: true,
	}
	var ips []net.IP
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	ips, err := resolver.LookupIP(ctx, "ip", domain)
	if err != nil {
		return "", err
	}
	return ips[0].String(), nil
}
func IPParser(list []string) []string {
	var IPList []string

	for _, ip := range list {
		// Parse CIDR
		if ips, err := cidrToIPList(ip); err == nil {
			IPList = append(IPList, ips...)
		} else if ipAddr := net.ParseIP(ip); ipAddr != nil { // Parse IP address
			IPList = append(IPList, ip)
		} else if domainIP, _ := GetIpFromDomain(ip); domainIP != "" { // Parse domain
			if ipAddr := net.ParseIP(domainIP); ipAddr != nil {
				IPList = append(IPList, domainIP)
			}
		}
	}

	return IPList
}

func GetNumIPs(cidrOrIP string) int {
	if ip := net.ParseIP(cidrOrIP); ip != nil {
		// input is an IP address
		return 1
	}

	// input is CIDR notation
	parts := strings.Split(cidrOrIP, "/")

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

// Validate IP Input
func IPValidator(ip string) string {
	var ipinput string
	if net.ParseIP(ip) != nil {
		ipinput = ip
	}
	return ipinput
}

func inc(ip net.IP) {
	for i := len(ip) - 1; i >= 0; i-- {
		if ip[i] == 255 {
			ip[i] = 0
			continue
		}
		ip[i]++
		break
	}
}

func GetFreePort() int {
	l, err := net.Listen("tcp", "localhost:0")
	if err != nil {
		log.Fatal(err)
	}
	defer func(l net.Listener) {
		err := l.Close()
		if err != nil {

		}
	}(l)

	addr := l.Addr().(*net.TCPAddr)
	return addr.Port
}

// func timeDurationToInt(n time.Duration) int64 {
// 	ms := int64(n / time.Millisecond)
// 	return ms
// }

func WaitForPort(host string, port int, timeout time.Duration) error {
	startTime := time.Now()
	timeDur := timeout * time.Second
	for {
		conn, err := net.DialTimeout("tcp", fmt.Sprintf("%s:%d", host, port), timeDur)
		if err == nil {
			err := conn.Close()
			if err != nil {
				return err
			}
			return nil
		}
		if time.Since(startTime) >= timeDur {
			return fmt.Errorf("waited too long for the port %d on host %s to start accepting connections", port, host)
		}
		time.Sleep(time.Millisecond * 10)
	}
}

func TotalIps(IPLIST []string) int {
	var nTotalIPs int

	for _, ips := range IPLIST {
		numIPs := GetNumIPs(ips)
		nTotalIPs += numIPs
	}
	return nTotalIPs
}
