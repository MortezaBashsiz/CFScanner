package vpn

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/signal"
	"time"

	// The following are necessary as they register handlers in their init functions.

	_ "github.com/xtls/xray-core/app/dispatcher"
	_ "github.com/xtls/xray-core/app/proxyman/inbound"
	_ "github.com/xtls/xray-core/app/proxyman/outbound"
	"github.com/xtls/xray-core/core"

	// Default commander and all its services. This is an optional feature.
	//_ "github.com/xtls/xray-core/app/commander"w
	//_ "github.com/xtls/xray-core/app/log/command"
	//_ "github.com/xtls/xray-core/app/proxyman/command"
	//_ "github.com/xtls/xray-core/app/stats/command"

	// Developer preview services
	//_ "github.com/xtls/xray-core/app/observatory/command"

	// Other optional features.
	_ "github.com/xtls/xray-core/app/dns"
	_ "github.com/xtls/xray-core/app/dns/fakedns"
	//_ "github.com/xtls/xray-core/app/log"
	_ "github.com/xtls/xray-core/app/metrics"
	_ "github.com/xtls/xray-core/app/policy"

	//_ "github.com/xtls/xray-core/app/reverse"
	_ "github.com/xtls/xray-core/app/router"
	_ "github.com/xtls/xray-core/app/stats"

	// Fix dependency cycle caused by core import in internet package
	_ "github.com/xtls/xray-core/transport/internet/tagged/taggedimpl"

	// Developer preview features
	// _ "github.com/xtls/xray-core/app/observatory"

	// Inbound and outbound proxies.
	_ "github.com/xtls/xray-core/proxy/blackhole"
	_ "github.com/xtls/xray-core/proxy/dns"
	_ "github.com/xtls/xray-core/proxy/dokodemo"
	_ "github.com/xtls/xray-core/proxy/freedom"
	_ "github.com/xtls/xray-core/proxy/http"

	_ "github.com/xtls/xray-core/proxy/loopback"
	// _ "github.com/xtls/xray-core/proxy/mtproto"

	// _ "github.com/xtls/xray-core/proxy/shadowsocks"
	_ "github.com/xtls/xray-core/proxy/socks"
	// _ "github.com/xtls/xray-core/proxy/trojan"

	// _ "github.com/xtls/xray-core/proxy/vless/inbound"
	// _ "github.com/xtls/xray-core/proxy/vless/outbound"
	_ "github.com/xtls/xray-core/proxy/vmess/inbound"
	_ "github.com/xtls/xray-core/proxy/vmess/outbound"
	_ "github.com/xtls/xray-core/proxy/wireguard"

	// Transports
	_ "github.com/xtls/xray-core/transport/internet/domainsocket"
	_ "github.com/xtls/xray-core/transport/internet/grpc"
	_ "github.com/xtls/xray-core/transport/internet/http"

	// _ "github.com/xtls/xray-core/transport/internet/kcp"
	// _ "github.com/xtls/xray-core/transport/internet/quic"
	_ "github.com/xtls/xray-core/transport/internet/tcp"
	_ "github.com/xtls/xray-core/transport/internet/tls"
	_ "github.com/xtls/xray-core/transport/internet/udp"
	_ "github.com/xtls/xray-core/transport/internet/websocket"

	// Transport headers
	_ "github.com/xtls/xray-core/transport/internet/headers/http"
	// _ "github.com/xtls/xray-core/transport/internet/headers/noop"
	// _ "github.com/xtls/xray-core/transport/internet/headers/srtp"
	_ "github.com/xtls/xray-core/transport/internet/headers/tls"

	// _ "github.com/xtls/xray-core/transport/internet/headers/utp"
	// _ "github.com/xtls/xray-core/transport/internet/headers/wechat"
	// _ "github.com/xtls/xray-core/transport/internet/headers/wireguard"

	// JSON & TOML & YAML
	_ "github.com/xtls/xray-core/main/json"
	// _ "github.com/xtls/xray-core/main/toml"
	// _ "github.com/xtls/xray-core/main/yaml"

	// Load config from file or http(s)
	_ "github.com/xtls/xray-core/main/confloader/external"
	// Commands
	//_ "github.com/xtls/xray-core/main/commands/all"
)

func XRayInstance(configPath string, timeout time.Duration) ScanWorker {
	config, err := LoadConfig(configPath)
	if err != nil {
		log.Fatal(err)
	}

	instance, err := core.New(config)
	if err != nil {
		log.Fatal(err)
	}

	if err := instance.Start(); err != nil {
		log.Println("Failed to start XRay instance:", err)
	}

	go func() {
		osSignals := make(chan os.Signal, 1)
		signal.Notify(osSignals, os.Interrupt)
		<-osSignals
	}()

	return ScanWorker{Instance: instance}
}

func ProxyBind(listen string, port int) map[string]string {
	proxies := map[string]string{
		"http":  fmt.Sprintf("socks5://%s:%d", listen, port),
		"https": fmt.Sprintf("socks5://%s:%d", listen, port)}

	return proxies

}

func XRayVersion() {
	fmt.Println(core.VersionStatement())
}

func XRayReceiver(configPath string) (string, int, error) {
	xrayConfFile, err := os.Open(configPath)
	if err != nil {
		return "", 0, err
	}

	defer xrayConfFile.Close()

	var xrayConf map[string]interface{}
	err = json.NewDecoder(xrayConfFile).Decode(&xrayConf)
	if err != nil {
		return "", 0, err
	}

	xrayListen := xrayConf["inbounds"].([]interface{})[0].(map[string]interface{})["listen"].(string)
	xrayPort := int(xrayConf["inbounds"].([]interface{})[0].(map[string]interface{})["port"].(float64))

	return xrayListen, xrayPort, nil
}
