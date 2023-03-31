package scanner

import (
	config "CFScanner/configuration"
	utils "CFScanner/utils"
	"encoding/csv"
	"fmt"
	"log"
	"os"
)

func (c CSV) CSVWriter() {
	resParts := []interface{}{
		c.ip,
		c.meanDownloadLatency, c.meanUploadSpeed,
		c.meanDownloadLatency, c.meanUploadLatency,
	}

	/////////////////////////////////////////////////////
	ip, ok := c.res["ip"].(string)
	if ok {
		for _, ip := range ip {
			resParts = append(resParts, ip)
		}
	}

	downSpeed, ok := c.res["download"].(map[string]interface{})["speed"].([]float64)
	if ok {
		for _, speed := range downSpeed {
			resParts = append(resParts, speed)
		}
	}

	uploadSpeed, ok := c.res["upload"].(map[string]interface{})["speed"].([]float64)
	if ok {
		for _, speed := range uploadSpeed {
			resParts = append(resParts, speed)
		}
	}
	downLatency, ok := c.res["download"].(map[string]interface{})["latency"].([]float64)
	if ok {
		for _, latency := range downLatency {
			resParts = append(resParts, latency)
		}
	}

	uploadLatency, ok := c.res["upload"].(map[string]interface{})["latency"].([]float64)
	if ok {
		for _, latency := range uploadLatency {
			resParts = append(resParts, latency)
		}
	}
	/////////////////////////////////////////////////////

	WriteCSV(config.INTERIM_RESULTS_PATH, resParts)
}

func WriteCSV(file string, result []interface{}) {
	// Open the file for appending the results
	f, err := os.OpenFile(file, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		fmt.Printf("Failed to open file: %s\n", err)
		return
	}
	defer f.Close()

	// Write the result parts to the file
	w := csv.NewWriter(f)
	if err := w.Write(utils.StringifySlice(result)); err != nil {
		fmt.Printf("Failed to write to file: %s\n", err)
	}
	w.Flush()

}

func (c CSV) Output() {

	log.Printf("%sOK %-15s %s avg_down_speed: %7.2fmbps avg_up_speed: %7.4fmbps avg_down_latency: %6.2fms avg_up_latency: %6.2fms avg_down_jitter: %6.2fms avg_up_jitter: %4.2fms%s\n",
		utils.Colors.OKGREEN,
		c.res["ip"].(string),
		utils.Colors.OKBLUE,
		c.meanDownloadSpeed,
		c.meanUploadSpeed,
		c.meanDownloadLatency,
		c.meanUploadLatency,
		c.downloadMeanJitter,
		c.uploadMeanJitter,
		utils.Colors.ENDC,
	)
}
