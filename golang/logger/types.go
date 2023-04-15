package logger

// ScannerManage is a logger for logging scanning process
type ScannerManage struct {
	IP      string
	Status  LogStatus
	Message interface{}
	Cause   string
}
