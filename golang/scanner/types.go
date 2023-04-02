package scanner

type Writer interface {
	CSVWriter()
	Output()
}

type CSV struct {
	res                 *Result
	ip                  string
	downloadMeanJitter  float64
	uploadMeanJitter    float64
	meanDownloadSpeed   float64
	meanUploadSpeed     float64
	meanDownloadLatency float64
	meanUploadLatency   float64
}
