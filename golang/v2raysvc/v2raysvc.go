package v2raysvc

import (
	configuration "CFScanner/configuration"
	utils "CFScanner/utils"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path"
	"strconv"
	"strings"
	"time"
)

var v2rayTemplate = `{
  "inbounds": [{
    "port": PORTPORT,
    "listen": "127.0.0.1",
    "tag": "socks-inbound",
    "protocol": "socks",
    "settings": {
      "auth": "noauth",
      "udp": false,
      "ip": "127.0.0.1"
    },
    "sniffing": {
      "enabled": true,
      "destOverride": ["http", "tls"]
    }
  }],
  "outbounds": [
    {
		"protocol": "vmess",
    "settings": {
      "vnext": [{
        "address": "IP.IP.IP.IP",
        "port": CFPORTCFPORT,
        "users": [{"id": "IDID" }]
      }]
    },
		"streamSettings": {
        "network": "ws",
        "security": "tls",
        "wsSettings": {
            "headers": {
                "Host": "HOSTHOST"
            },
            "path": "ENDPOINTENDPOINT"
        },
        "tlsSettings": {
            "serverName": "RANDOMHOST",
            "allowInsecure": false
        }
    }
	}],
  "other": {}
}`

// CreateV2rayConfig create VPN configuration
func CreateV2rayConfig(IP string, testConfig configuration.ConfigStruct) string {
	localPortStr := strconv.Itoa(utils.GetFreePort())
	config := strings.ReplaceAll(v2rayTemplate, "PORTPORT", localPortStr)
	config = strings.ReplaceAll(config, "IP.IP.IP.IP", IP)
	config = strings.ReplaceAll(config, "CFPORTCFPORT", testConfig.AddressPort)
	config = strings.ReplaceAll(config, "IDID", testConfig.UserId)
	config = strings.ReplaceAll(config, "HOSTHOST", testConfig.WsHeaderHost)
	config = strings.ReplaceAll(config, "ENDPOINTENDPOINT", testConfig.WsHeaderPath)
	config = strings.ReplaceAll(config, "RANDOMHOST", testConfig.Sni)

	configPath := fmt.Sprintf("%s/config-%s.json", configuration.DIR, IP)
	configFile, err := os.Create(configPath)
	if err != nil {
		log.Fatal(err)
	}
	defer func(configFile *os.File) {
		err := configFile.Close()
		if err != nil {

		}
	}(configFile)

	_, configErr := configFile.WriteString(config)
	if configErr != nil {
		return ""
	}

	return configPath
}

// StartV2RayService start VPN service based on bin path
func StartV2RayService(v2rayConfPath string, timeout time.Duration) (*exec.Cmd, map[string]string, error) {
	v2rayConfFile, err := os.Open(v2rayConfPath)
	if err != nil {
		return nil, nil, err
	}
	defer func(v2rayConfFile *os.File) {
		err := v2rayConfFile.Close()
		if err != nil {

		}
	}(v2rayConfFile)

	var v2rayConf map[string]interface{}
	err = json.NewDecoder(v2rayConfFile).Decode(&v2rayConf)
	if err != nil {
		return nil, nil, err
	}

	v2rayListen := v2rayConf["inbounds"].([]interface{})[0].(map[string]interface{})["listen"].(string)
	v2rayPort := int(v2rayConf["inbounds"].([]interface{})[0].(map[string]interface{})["port"].(float64))

	v2rayCmd := exec.Command(path.Join(configuration.BIN), "-c", v2rayConfPath)
	v2rayCmd.Stdout = nil
	v2rayCmd.Stderr = nil

	err = v2rayCmd.Start()
	if err != nil {
		return nil, nil, err
	}

	err = utils.WaitForPort(v2rayListen, v2rayPort, timeout)
	if err != nil {
		return nil, nil, err
	}

	proxies := map[string]string{
		"http":  fmt.Sprintf("socks5://%s:%d", v2rayListen, v2rayPort),
		"https": fmt.Sprintf("socks5://%s:%d", v2rayListen, v2rayPort),
	}

	return v2rayCmd, proxies, nil
}
