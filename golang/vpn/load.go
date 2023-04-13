package vpn

import (
	"github.com/xtls/xray-core/core"
	"log"
	"os"
)

func LoadConfig(filename string) (*core.Config, error) {
	file, err := os.OpenFile(filename, os.O_RDONLY, 0)
	if err != nil {
		log.Fatal(err)
	}

	config, err := core.LoadConfig("json", file)
	if err != nil {
		log.Fatal(err)
	}

	return config, nil
}
