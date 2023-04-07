package scanner

import (
	config "CFScanner/configuration"
	"CFScanner/speedtest"
	"CFScanner/utils"
	"CFScanner/v2raysvc"
	"fmt"
	"github.com/eiannone/keyboard"
	"log"
	"math"
	"os"
	"os/exec"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"
)

var results [][]string

var (
	downloadSpeed   float64
	downloadLatency float64
	uploadSpeed     float64
	uploadLatency   float64
)

type Result struct {
	IP       string
	Download struct {
		Speed   []float64
		Latency []int
	}
	Upload struct {
		Speed   []float64
		Latency []int
	}
}

// Running Possible worker state.
var (
	Running bool
	MaxProc = runtime.NumCPU() * 2 // Max CPU + Thread * 2
)

// const WorkerCount = 48

func scanner(ip string, C config.Configuration, Worker config.Worker) *Result {

	result := &Result{
		IP: ip,
	}

	var Upload = &Worker.Upload
	var Download = &Worker.Download

	var proxies map[string]string = nil
	var process *exec.Cmd = nil

	if Worker.Vpn {
		v2rayConfigPath := v2raysvc.CreateV2rayConfig(ip, &C)
		var err error
		process, proxies, err = v2raysvc.StartV2RayService(v2rayConfigPath, time.Duration(Worker.StartProcessTimeout))
		if err != nil {
			log.Printf("%vERROR - %vCould not start v2ray service%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, utils.Colors.ENDC)
			log.Fatal(err)
			return nil
		}

		defer func(Process *os.Process) {
			err := Process.Kill()
			if err != nil {
				_ = fmt.Errorf("could not kill the process %v", process.Process.Pid)
			}
		}(process.Process)
		defer func() {
			if r := recover(); r != nil {
				log.Printf("%sFAIL %v%15s Panic: %v%v\n", utils.Colors.FAIL, utils.Colors.WARNING, ip, r, utils.Colors.ENDC)
			}
		}()
	}

	for tryIdx := 0; tryIdx < C.Config.NTries; tryIdx++ {
		// Fronting test
		if C.Config.DoFrontingTest {
			fronting := speedtest.FrontingTest(ip, time.Duration(C.Config.FrontingTimeout))

			if !fronting {
				return nil
			}
		}

		// Check download speed
		if m, done := downloader(ip, Download, proxies, result); done {
			return m
		}

		// upload speed test
		if C.Config.DoUploadTest {
			if m2, done2 := uploader(ip, Upload, proxies, result); done2 {
				return m2
			}
		}

		dlTimeLatency := math.Round(downloadLatency * 1000)
		upTimeLatency := math.Round(uploadLatency * 1000)

		log.Printf("%vOK IP: %v , Download: %7.4fmbps , Upload: %7.4fmbps , UP_Latency: %vms , DL_Latency: %vms%v\n",
			utils.Colors.OKGREEN, ip, downloadSpeed, uploadSpeed, upTimeLatency, dlTimeLatency, utils.Colors.ENDC)
	}

	return result
}
func uploader(ip string, Upload *config.Upload, proxies map[string]string, result *Result) (*Result, bool) {
	var err error
	nBytes := Upload.MinUlSpeed * 1000 * Upload.MaxUlTime
	uploadSpeed, uploadLatency, err = speedtest.UploadSpeedTest(int(nBytes), proxies,
		time.Duration(Upload.MaxUlLatency))

	if err != nil {
		log.Printf("%sFAIL %v%15s Upload error : %v%v\n", utils.Colors.FAIL, utils.Colors.WARNING, ip, err, utils.Colors.ENDC)

		return nil, true
	}
	if uploadLatency <= Upload.MaxUlLatency {
		uploadSpeedKbps := uploadSpeed / 8 * 1000
		if uploadSpeedKbps >= Upload.MinUlSpeed {
			result.Upload.Speed = append(result.Upload.Speed, uploadSpeed)
			result.Upload.Latency = append(result.Upload.Latency, int(math.Round(uploadLatency*1000)))
		} else {
			log.Printf("%sFAIL %v%15s Upload too slow %f kBps < %f kBps%s\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, uploadSpeedKbps, Upload.MinUlSpeed, utils.Colors.ENDC)

			return nil, true
		}
	} else {
		log.Printf("%sFAIL %v%15s Upload latency too high  %s\n",
			utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)

		return nil, true
	}
	return nil, false
}

func downloader(ip string, Download *config.Download, proxies map[string]string, result *Result) (*Result, bool) {
	nBytes := Download.MinDlSpeed * 1000 * Download.MaxDlTime
	var err error
	downloadSpeed, downloadLatency, err = speedtest.DownloadSpeedTest(int(nBytes), proxies,
		time.Duration(Download.MaxDlLatency))
	if err != nil {
		if strings.Contains(strings.ToLower(err.Error()), "download/upload too slow") {
			log.Printf("%vFAIL %v%15s Download too slow\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip)
		} else {
			log.Printf("%vFAIL %v%15s Download error%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, utils.Colors.ENDC)
		}
		return nil, true
	}
	if downloadLatency <= Download.MaxDlLatency {
		downloadSpeedKBps := downloadSpeed / 8 * 1000
		if downloadSpeedKBps >= Download.MinDlSpeed {
			result.Download.Speed = append(result.Download.Speed, downloadSpeed)
			result.Download.Latency = append(result.Download.Latency, int(math.Round(downloadLatency*1000)))
		} else {
			log.Printf("%vFAIL %v%15s Download too slow %.4f kBps < %.4f kBps%v\n",
				utils.Colors.FAIL, utils.Colors.WARNING, ip, downloadSpeedKBps, Download.MinDlSpeed, utils.Colors.ENDC)
			return nil, true
		}
	} else {
		log.Printf("%vFAIL %v%15s High Download latency %.4f s > %.4f s%v\n",
			utils.Colors.FAIL, utils.Colors.WARNING, ip, downloadLatency, Download.MaxDlLatency, utils.Colors.ENDC)
		return nil, true
	}
	return result, false
}

func scan(C *config.Configuration, worker *config.Worker, ip string) {
	res := scanner(ip, *C, *worker)
	if res == nil {
		return
	}

	// make downLatencyInt to float64
	downLatencyInt := res.Download.Latency
	downLatency := make([]float64, len(downLatencyInt))
	for i, v := range downLatencyInt {
		downLatency[i] = float64(v)
	}
	downMeanJitter := utils.MeanJitter(downLatency)

	// make uploadLatencyInt to float64
	uploadLatencyInt := res.Upload.Latency
	uploadLatency := make([]float64, len(uploadLatencyInt))
	for i, v := range uploadLatencyInt {
		uploadLatency[i] = float64(v)
	}
	upMeanJitter := -1.0

	if C.Config.DoUploadTest {
		upMeanJitter = utils.MeanJitter(uploadLatency)
	}

	downSpeed := res.Download.Speed
	meanDownSpeed := utils.Mean(downSpeed)
	meanUploadSpeed := -1.0

	uploadSpeed := res.Upload.Speed
	if C.Config.DoUploadTest {
		meanUploadSpeed = utils.Mean(uploadSpeed)
	}

	meanDownLatency := utils.Mean(downLatency)
	meanUploadLatency := -1.0
	if C.Config.DoUploadTest {
		meanUploadLatency = utils.Mean(uploadLatency)
	}

	// change download latency to string type for using it with saveResults func
	var latencyDownloadString string
	for _, f := range downLatencyInt {
		latencyDownloadString = fmt.Sprintf("%d", f)
	}

	results = append(results, []string{latencyDownloadString, ip})

	var Writer Writer
	switch C.Config.Writer {
	case "csv":
		Writer = CSV{
			res:                 res,
			IP:                  ip,
			DownloadMeanJitter:  downMeanJitter,
			UploadMeanJitter:    upMeanJitter,
			MeanDownloadSpeed:   meanDownSpeed,
			MeanDownloadLatency: meanDownLatency,
			MeanUploadSpeed:     meanUploadSpeed,
			MeanUploadLatency:   meanUploadLatency,
		}
	case "json":
		Writer = JSON{
			res:                 res,
			IP:                  ip,
			DownloadMeanJitter:  downMeanJitter,
			UploadMeanJitter:    upMeanJitter,
			MeanDownloadSpeed:   meanDownSpeed,
			MeanDownloadLatency: meanDownLatency,
			MeanUploadSpeed:     meanUploadSpeed,
			MeanUploadLatency:   meanUploadLatency,
		}
	default:
		log.Fatalf("Invalid writer type: %s", C.Config.Writer)
	}

	Writer.Output()
	Writer.Write()

}
func Start(C config.Configuration, Worker config.Worker, ipList []string, threadsCount int) {
	var (
		wg         sync.WaitGroup
		pauseChan  = make(chan struct{})
		resumeChan = make(chan struct{})
		quitChan   = make(chan struct{})
	)

	// limit the thread execution if it was higher than current cpu num * 2
	if threadsCount > MaxProc {
		fmt.Println("Max Thread limit setting thread to :", MaxProc)
		threadsCount = MaxProc
	}

	// get the key events
	keysEvents, err := keyboard.GetKeys(10)
	if err != nil {
		fmt.Println(err)
		return
	}

	// Create batches
	n := len(ipList)
	batchSize := len(ipList) / threadsCount
	batches := make([][]string, threadsCount)

	for i := range batches {
		start := i * batchSize
		end := (i + 1) * batchSize
		if i == threadsCount-1 {
			end = n
		}
		batches[i] = ipList[start:end]
	}

	// Start workers
	Running = true
	for i := 0; i < threadsCount; i++ {
		wg.Add(1)
		go func(batch []string) {
			defer wg.Done()
			for _, ip := range batch {
				select {
				case <-pauseChan:
					// wait for resume signal
					<-resumeChan
				case <-quitChan:
					// quit the function
					return
				default:
					scan(&C, &Worker, ip)
				}
			}
		}(batches[i])
	}

	// Handle user input in a separate goroutine
	go func() {
		pauseChan, resumeChan = controller(keysEvents, threadsCount, pauseChan, resumeChan)
		// Wait for quit signal
		<-quitChan

		// close the state listener channel
		close(pauseChan)
		close(resumeChan)
	}()

	wg.Wait()

	// close key event listener
	defer func() {
		_ = keyboard.Close()
	}()

	// Save results
	err = saveResults(results, config.FinalResultsPathSorted, true)
	if err != nil {
		fmt.Println(err)
		return
	}
}

// controller is a event listener for pausing or running workers
func controller(keysEvents <-chan keyboard.KeyEvent,
	threadsCount int, pauseChan chan struct{}, resumeChan chan struct{}) (chan struct{}, chan struct{}) {

	for {
		event := <-keysEvents

		// exit program with event.key listener
		if event.Key == keyboard.KeyEsc || event.Key == keyboard.KeyCtrlC {
			_ = keyboard.Close()
			os.Exit(1)
		}

		if event.Rune == 'p' || event.Rune == 'P' {
			if !Running {
				fmt.Println("Channel is currently Paused")
				continue
			}

			for x := 0; x < threadsCount; x++ {
				pauseChan <- struct{}{}
			}
			// set runner state to false
			Running = false
			fmt.Println("Paused")
			time.Sleep(100 * time.Millisecond) // Add a small delay to prevent CPU usage

		}
		if event.Rune == 'r' || event.Rune == 'R' {
			if Running {
				fmt.Println("Channel is currently Running")
				continue
			}

			for x := 0; x < threadsCount; x++ {
				resumeChan <- struct{}{}
			}
			// set runner state to true
			Running = true
			fmt.Println("Resumed")
			time.Sleep(100 * time.Millisecond) // Add a small delay to prevent CPU usage

		}

	}

	return pauseChan, resumeChan
}

func saveResults(values [][]string, savePath string, sort bool) error {
	// clean the values and make sure the first element is integer
	for i := 0; i < len(values); i++ {
		ms, err := strconv.Atoi(strings.TrimSuffix(values[i][0], " ms"))
		if err != nil {
			return err
		}
		values[i][0] = strconv.Itoa(ms)
	}

	if sort {
		// sort the values based on response time using bubble sort
		for i := 0; i < len(values); i++ {
			for j := 0; j < len(values)-1; j++ {
				ms1, _ := strconv.Atoi(values[j][0])
				ms2, _ := strconv.Atoi(values[j+1][0])
				if ms1 > ms2 {
					values[j], values[j+1] = values[j+1], values[j]
				}
			}
		}
	}

	// write the values to file
	var lines []string
	for _, res := range values {
		lines = append(lines, strings.Join(res, " "))
	}
	data := []byte(strings.Join(lines, "\n") + "\n")
	err := os.WriteFile(savePath, data, 0644)
	if err != nil {
		return err
	}

	return nil
}
