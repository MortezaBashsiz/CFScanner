package vpn

import (
	"github.com/xtls/xray-core/core"
)

type ScanWorker struct {
	Instance *core.Instance
}

type XRay struct {
	Log struct {
		Loglevel string `json:"loglevel"`
	} `json:"log"`
	Inbounds []struct {
		Port     string `json:"port"`
		Listen   string `json:"listen"`
		Tag      string `json:"tag"`
		Protocol string `json:"protocol"`
		Settings struct {
			Auth string `json:"auth"`
			UDP  bool   `json:"udp"`
			IP   string `json:"ip"`
		} `json:"settings"`
		Sniffing struct {
			Enabled      bool     `json:"enabled"`
			DestOverride []string `json:"destOverride"`
		} `json:"sniffing"`
	} `json:"inbounds"`
	Outbounds []struct {
		Protocol string `json:"protocol"`
		Settings struct {
			Vnext []struct {
				Address string `json:"address"`
				Port    string `json:"port"`
				Users   []struct {
					ID string `json:"id"`
				} `json:"users"`
			} `json:"vnext"`
		} `json:"settings"`
		StreamSettings struct {
			Network    string `json:"network"`
			Security   string `json:"security"`
			WSSettings struct {
				Headers struct {
					Host string `json:"Host"`
				} `json:"headers"`
				Path string `json:"path"`
			} `json:"wsSettings"`
			TLSSettings struct {
				ServerName    string `json:"serverName"`
				AllowInsecure bool   `json:"allowInsecure"`
			} `json:"tlsSettings"`
		} `json:"streamSettings"`
	} `json:"outbounds"`
	Other struct{} `json:"other"`
}
