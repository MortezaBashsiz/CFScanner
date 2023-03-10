package v2raysvc

// not implemented yet

// type fakeProcess struct{}

// func (fp fakeProcess) Kill() error {
// 	return syscall.Kill(syscall.Getpid(), syscall.SIGTERM)
// }

// var v2rayTemplate = `{
//   "inbounds": [{
//     "port": PORTPORT,
//     "listen": "127.0.0.1",
//     "tag": "socks-inbound",
//     "protocol": "socks",
//     "settings": {
//       "auth": "noauth",
//       "udp": false,
//       "ip": "127.0.0.1"
//     },
//     "sniffing": {
//       "enabled": true,
//       "destOverride": ["http", "tls"]
//     }
//   }],
//   "outbounds": [
//     {
// 		"protocol": "vmess",
//     "settings": {
//       "vnext": [{
//         "address": "IP.IP.IP.IP",
//         "port": CFPORTCFPORT,
//         "users": [{"id": "IDID" }]
//       }]
//     },
// 		"streamSettings": {
//         "network": "ws",
//         "security": "tls",
//         "wsSettings": {
//             "headers": {
//                 "Host": "HOSTHOST"
//             },
//             "path": "ENDPOINTENDPOINT"
//         },
//         "tlsSettings": {
//             "serverName": "RANDOMHOST",
//             "allowInsecure": false
//         }
//     }
// 	}],
//   "other": {}
// }`

// func CreateV2rayConfig(edgeIP string, testConfig ConfigStruct) string {
// 	localPortStr := strconv.Itoa(getFreePort())
// 	config := strings.ReplaceAll(v2rayTemplate, "PORTPORT", localPortStr)
// 	config = strings.ReplaceAll(config, "IP.IP.IP.IP", edgeIP)
// 	config = strings.ReplaceAll(config, "CFPORTCFPORT", testConfig.address_port)
// 	config = strings.ReplaceAll(config, "IDID", testConfig.user_id)
// 	config = strings.ReplaceAll(config, "HOSTHOST", testConfig.ws_header_host)
// 	config = strings.ReplaceAll(config, "ENDPOINTENDPOINT", testConfig.ws_header_path)
// 	config = strings.ReplaceAll(config, "RANDOMHOST", testConfig.sni)

// 	configPath := fmt.Sprintf("%s/config-%s.json", CONFIGDIR, edgeIP)
// 	configFile, err := os.Create(configPath)
// 	if err != nil {
// 		log.Fatal(err)
// 	}
// 	defer configFile.Close()

// 	configFile.WriteString(config)

// 	return configPath
// }

// func StartV2RayService(v2rayConfPath string, timeout time.Duration) (*exec.Cmd, map[string]string, error) {
// 	v2rayConfFile, err := os.Open(v2rayConfPath)
// 	if err != nil {
// 		return nil, nil, err
// 	}
// 	defer v2rayConfFile.Close()

// 	var v2rayConf map[string]interface{}
// 	err = json.NewDecoder(v2rayConfFile).Decode(&v2rayConf)
// 	if err != nil {
// 		return nil, nil, err
// 	}

// 	v2rayListen := v2rayConf["inbounds"].([]interface{})[0].(map[string]interface{})["listen"].(string)
// 	v2rayPort := int(v2rayConf["inbounds"].([]interface{})[0].(map[string]interface{})["port"].(float64))

// 	fmt.Println(v2rayListen, v2rayPort)

// 	v2rayCmd := exec.Command(path.Join(BINDIR, "v2ray"), "run", v2rayConfPath)
// 	v2rayCmd.Stdout = nil
// 	v2rayCmd.Stderr = nil

// 	err = v2rayCmd.Start()
// 	if err != nil {
// 		return nil, nil, err
// 	}

// 	err = waitForPort(v2rayListen, v2rayPort, timeout)
// 	if err != nil {
// 		return nil, nil, err
// 	}

// 	proxies := map[string]string{
// 		"http":  fmt.Sprintf("socks5://%s:%d", v2rayListen, v2rayPort),
// 		"https": fmt.Sprintf("socks5://%s:%d", v2rayListen, v2rayPort),
// 	}

// 	return v2rayCmd, proxies, nil
// }
