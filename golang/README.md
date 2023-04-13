# CFScanner GoLang

![go]
![version]

CFScanner is a powerful tool written in Golang specifically designed to scan Cloudflare's edge IPs and identify viable options for use with V2Ray/Xray.

Its main objective is to locate edge IPs that are accessible and not blocked. With its built-in xray-core, CFScanner leverages xray+vmess+websocket+tls by default when the VPN flag is enabled.

If you prefer to use it behind your Cloudflare proxy, you will need to set up a vmess account. However, if no specific configuration is provided, the program will automatically use the default settings.
# Requirements

- Golang v1.20

# Installation

### Getting the latest version from release page
Latest release version of golang CFScanner are available in [releases](https://github.com/MortezaBashsiz/CFScanner/releases)
section 


### Build instructions

If you prefer to build CFScanner from source, you can follow these instructions:

Clone the repository by running the following command in your terminal:
```bash
git clone https://github.com/MortezaBashsiz/CFScanner.git
```
Navigate to the "golang" directory within the cloned repository:

```bash
cd CFScanner/golang
```

Build the binary using the "go build" command with additional flags for trimming the path and setting linker flags for smaller binary size:
```bash
go build -o CFScanner -trimpath -ldflags "-s -w -buildid=" .
```

## Get Configuration file

```bash
curl -s https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/bash/ClientConfig.json -o config.real
```

in the config file the variables are :

```json
{
  "id": "User's UUID",
  "Host": "Host address which is behind Cloudflare",
  "Port": "Port which you are using behind Cloudflare on your origin server",
  "path": "Websocket endpoint like api20",
  "serverName": "SNI"
}
```

- NOTE: If you want to use your custom config DO NOT use it as config.real since script will update this file. Store your config in another file and pass it as an argument to script instead of config.real

- The configuration file are similar to the bash version.

# Usage

To see CFScanner help , run the following command:

```bash
./CFScanner -h
```

CFScanner takes several arguments:

| Arguments              | Short Descriptions                                                                               |
|------------------------|--------------------------------------------------------------------------------------------------|
| --threads -t           | Number of threads to use for parallel scanning. Default is 1.                                    |
| --config -c            | The path to the config file. (Required)                                                          |
| --vpn                  | If passed, test with creating xray-core connections.                                             |
| --loglevel -l          | The log level for xray-core (default "none")                                                     |
| --subnets -s           | The file or subnet. Each line should be in the form of ip.ip.ip.ip/subnet_mask or ip.ip.ip.ip.   |
| --shuffle              | Shuffling given subnet file or input                                                             |
| --upload               | If passed, upload test will be conducted.                                                        |
| --fronting             | If passed, fronting request test will be conducted.                                              |
| --tries -n             | Number of times to try each IP. An IP is marked as OK if all tries are successful. Default is 1. |
| --download-speed       | Maximum download speed in kilobytes per second. Default is 50.                                   |
| --upload-speed         | Maximum upload speed in kilobytes per second. Default is 50.                                     |
| --download-time        | Maximum time to spend for each download. Default is 2.                                           |
| --upload-time          | Maximum time to spend for each upload. Default is 2.                                             |
| --fronting-timeout     | Maximum time to wait for fronting response. Default is 1.0.                                      |
| --download-latency     | Maximum allowed latency for download. Default is 2.0.                                            |
| --upload-latency       | Maximum allowed latency for upload. Default is 2.0.                                              |
| --writer               | Custom output writer for writing interim results. available writers : `csv`/`json`               |

# Features

### KeyEvent Listeners
CFScanner supports pause and resume progress 

- For Pausing current progress press `p`
  
- For Resuming current progress press `r`

### XRay Core
This Program has built-in Xray-core for testing connection with vpn

xray-core has 5 log levels

**Available log levels**
- debug
- info
- warning
- error
- none

default loglevel is none.

---

# Examples


### Load configuration file and load subnet file for scanning

```bash
./CFScanner --config config.real --subnets ips.txt
```

### Load configuration file and use input cidr and begin scanning ips with 4 threads

```bash
./CFScanner --config config.real --subnets 172.20.0.0/24 --threads 4
```

### Load configurations file with subnet file and doing upload test

```bash
./CFScanner --config config.real --subnets 172.20.0.0/24 --threads 4 --upload
```

### Load configurations file with subnet file and testing each ip 3 times

```bash
./CFScanner --config config.real --subnets 172.20.0.0/24 --threads 4 --tries 3
```

### Load configurations file with subnet file and using vpn mode

```bash
./CFScanner --config config.real --subnets 172.20.0.0/24 --vpn 
```

---

## Output

Two files are stored for each (complete) run of the program

#### Writer Results
- Interim results file (e.g., `2023-03-10_20:49:30_result.csv` or `2023-03-10_20:49:30_result.json`)
  - Includes the unsorted intermediate results in specified writer format. Useful in case the scanning process is not complete.
#### Sorted Results
- Final results file (e.g., `2023-03-10_20:49:30_final.txt`)
  - Includes the final sorted results. The results are sorted ascending ly based on the download latency time.

all results are stored in `result` folder

# License

CFScanner is released under the [GPL-3](../LICENSE) license.

# Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](../CONTRIBUTING.md) for more information.

[go]: https://img.shields.io/badge/Go-cyan?logo=go
[version]: https://img.shields.io/badge/Version-1.6-blue
