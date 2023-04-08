package scanner

import (
	config "CFScanner/configuration"
	"encoding/json"
	"fmt"
	"log"
	"os"
)

func (c JSON) Write() {
	out, _ := json.Marshal(c)

	f, err := os.OpenFile(config.JSONInterimResultsPath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		fmt.Printf("Failed to open file: %s\n", err)
		return
	}
	defer func(f *os.File) {
		err := f.Close()
		if err != nil {
			fmt.Printf("Failed to close file: %s\n", err)
		}
	}(f)

	// Write the result parts to the file as JSON objects
	if _, err := f.WriteString(string(out) + "\n"); err != nil {
		fmt.Printf("Failed to write to file: %s\n", err)
	}

}

func (c JSON) Output() {
	out, _ := json.Marshal(c)
	log.Println(string(out))
}
