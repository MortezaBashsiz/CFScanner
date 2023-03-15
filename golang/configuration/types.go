package config

type ConfigStruct struct {
	Local_port           int
	Address_port         string
	User_id              string
	Ws_header_host       string
	Ws_header_path       string
	Sni                  string
	Do_upload_test       bool
	Do_fronting_test     bool
	Min_dl_speed         float64 // kilobytes per second
	Min_ul_speed         float64 // kilobytes per second
	Max_dl_time          float64 // seconds
	Max_ul_time          float64 // seconds
	Max_dl_latency       float64 // seconds
	Max_ul_latency       float64 // seconds
	Fronting_timeout     float64 // seconds
	Startprocess_timeout float64 // seconds
	N_tries              int
	Vpn                  bool
}
