package logger

// errors const
const (
	DownloadError     = "Download Error"
	DownloadSpeedSlow = "Slow Download Speed "
	UploadErorr       = "Upload Error"
	UploadLatency     = "Upload Latency too high"
)

// status const
const (
	OKStatus    = LogStatus("[OK]")
	FailStatus  = LogStatus("[FAIL]")
	ErrorStatus = LogStatus("[ERROR]")
)
