package scanner

import (
	config "CFScanner/configuration"
	"CFScanner/utils"
	"encoding/csv"
	"fmt"
	"log"
	"os"
)

func (c CSV) Write() {
	resParts := []interface{}{
		c.IP,
		c.MeanDownloadLatency, c.MeanUploadSpeed,
		c.MeanDownloadLatency, c.MeanUploadLatency,
	}
	ip := c.res.IP
	for _, ip := range ip {
		resParts = append(resParts, ip)
	}

	downSpeed := c.res.Download.Speed
	for _, speed := range downSpeed {
		resParts = append(resParts, speed)
	}

	uploadSpeed := c.res.Upload.Speed
	for _, speed := range uploadSpeed {
		resParts = append(resParts, speed)
	}

	downLatency := c.res.Download.Latency
	for _, latency := range downLatency {
		resParts = append(resParts, latency)
	}

	uploadLatency := c.res.Upload.Latency
	for _, latency := range uploadLatency {
		resParts = append(resParts, latency)
	}

	WriteCSV(config.CSVInterimResultsPath, resParts)
}

func WriteCSV(file string, result []interface{}) {
	// Open the file for appending the results
	f, err := os.OpenFile(file, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		fmt.Printf("Failed to open file: %s\n", err)
		return
	}
	defer func(f *os.File) {
		err := f.Close()
		if err != nil {

		}
	}(f)

	// Write the result parts to the file
	w := csv.NewWriter(f)
	if err := w.Write(utils.StringifySlice(result)); err != nil {
		fmt.Printf("Failed to write to file: %s\n", err)
	}
	w.Flush()

}

func (c CSV) Output() {

	log.Printf("%s[OK] %-15s %s avg_down_speed: %7.2fmbps avg_up_speed: %7.4fmbps avg_down_latency: %6.2fms avg_up_latency: %6.2fms avg_down_jitter: %6.2fms avg_up_jitter: %4.2fms%s\n",
		utils.Colors.OKGREEN,
		c.res.IP,
		utils.Colors.OKBLUE,
		c.MeanDownloadSpeed,
		c.MeanUploadSpeed,
		c.MeanDownloadLatency,
		c.MeanUploadLatency,
		c.DownloadMeanJitter,
		c.UploadMeanJitter,
		utils.Colors.ENDC,
	)
}
