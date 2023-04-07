package config

type Configuration struct {
	Config    ConfigStruct
	Worker    Worker
	Shuffling bool
}

type Worker struct {
	Download            Download
	Upload              Upload
	StartProcessTimeout float64 // seconds
	Threads             int
	Vpn                 bool
}

type ConfigStruct struct {
	LocalPort       int
	AddressPort     string
	UserId          string
	WsHeaderHost    string
	WsHeaderPath    string
	Sni             string
	FrontingTimeout float64 // seconds
	NTries          int
	Writer          string
	TestBool
	Download
}

type TestBool struct {
	DoUploadTest   bool
	DoFrontingTest bool
}

type Download struct {
	MinDlSpeed   float64 // kilobytes per second
	MaxDlTime    float64 // seconds
	MaxDlLatency float64 // seconds

}

type Upload struct {
	MinUlSpeed   float64 // kilobytes per second
	MaxUlTime    float64 // seconds
	MaxUlLatency float64 // seconds

}
