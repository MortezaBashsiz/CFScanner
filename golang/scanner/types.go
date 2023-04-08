package scanner

type Writer interface {
	Write()
	Output()
}

type CSV struct {
	res                 *Result
	IP                  string
	DownloadMeanJitter  float64
	UploadMeanJitter    float64
	MeanDownloadSpeed   float64
	MeanUploadSpeed     float64
	MeanDownloadLatency float64
	MeanUploadLatency   float64
}

type JSON struct {
	res                 *Result
	IP                  string  `json:"ip"`
	DownloadMeanJitter  float64 `json:"downloadMeanJitter"`
	UploadMeanJitter    float64 `json:"uploadMeanJitter"`
	MeanDownloadSpeed   float64 `json:"meanDownloadSpeed"`
	MeanUploadSpeed     float64 `json:"meanUploadSpeed"`
	MeanDownloadLatency float64 `json:"meanDownloadLatency"`
	MeanUploadLatency   float64 `json:"meanUploadLatency"`
}
