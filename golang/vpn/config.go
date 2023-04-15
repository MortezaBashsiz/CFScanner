package vpn

import (
	configuration "CFScanner/configuration"
	"CFScanner/utils"
	"encoding/json"
	"fmt"
	"github.com/xtls/xray-core/common/uuid"
	"log"
	"os"
	"strconv"
	"strings"
)

var logger = []string{"debug",
	"info",
	"warning",
	"error",
	"none"}

func loglevel(level string) string {
	for _, log := range logger {
		if strings.ToLower(level) == log {
			return level
		}
	}
	return logger[4]
}

func createInbound() []Inbound {
	localPortStr := utils.GetFreePort()

	config := Inbound{
		Port:     localPortStr,
		Listen:   "127.0.0.1",
		Tag:      "socks-inbound",
		Protocol: "socks",
		Settings: struct {
			Auth string `json:"auth"`
			UDP  bool   `json:"udp"`
			IP   string `json:"ip"`
		}{"noauth", false, "127.0.0.1"},
		Sniffing: struct {
			Enabled      bool     `json:"enabled"`
			DestOverride []string `json:"destOverride"`
		}{true, []string{"http", "tls"}},
	}
	configSlice := []Inbound{config}
	return configSlice
}

func createOutbound(C *configuration.Configuration, IP string) []Outbound {
	vnextIP, _ := strconv.Atoi(C.Config.AddressPort)
	streamUUID := uuid.New()

	substrings := strings.SplitN(C.Config.WsHeaderHost, ".", 2)
	hostname := substrings[1]

	C.Config.Sni = fmt.Sprintf("%s.%s", streamUUID.String(), hostname)

	config := Outbound{
		Protocol: "vmess",
		Settings: struct {
			VNext []VNext `json:"vnext"`
		}{
			VNext: []VNext{
				{
					Address: IP,
					Port:    vnextIP,
					Users: []User{
						{
							ID: C.Config.UserId,
						},
					},
				},
			},
		},
		StreamSettings: StreamSettings{
			Network:  "ws",
			Security: "tls",
			WSSettings: WSSettings{
				Headers: struct {
					Host string `json:"Host"`
				}{
					Host: C.Config.WsHeaderHost,
				},
				Path: C.Config.WsHeaderPath,
			},
			TLSSettings: TLSSettings{
				ServerName:    C.Config.Sni,
				AllowInsecure: false,
			},
		},
	}
	configSlice := []Outbound{config}
	return configSlice
}

// XRayConfig create VPN configuration
func XRayConfig(IP string, testConfig *configuration.Configuration) string {
	config := XRay{
		Log: Log{
			Loglevel: loglevel(testConfig.LogLevel),
		},
		Inbounds:  createInbound(),
		Outbounds: createOutbound(testConfig, IP),
		Other:     struct{}{},
	}

	configByte, err := json.Marshal(config)
	if err != nil {
		log.Fatal("Marshal error", err)
	}

	configPath := fmt.Sprintf("%s/config-%s.json", configuration.DIR, IP)

	err = writeJSONToFile(configByte, configPath)
	if err != nil {
		log.Fatal("Failed to write JSON to file", err)
		return ""
	}

	return configPath
}

func writeJSONToFile(jsonBytes []byte, filename string) error {
	file, err := os.Create(filename)
	if err != nil {
		return err
	}
	defer file.Close()

	_, err = file.Write(jsonBytes)
	if err != nil {
		return err
	}

	return nil
}
