# CFScanner GoLang

![go]
![version]

### CFScanner is a tool written in Golang that scans Cloudflare's edge IPs and locates ones that are viable for use with V2Ray/Xray. It aims to identify edge IPs that are accessible and not blocked.

### This program is similar to the bash version, utilizing v2ray+vmess+websocket+tls by default when the VPN flag is enabled. If you wish to use it behind your Cloudflare proxy, you will need to set up a vmess account. Otherwise, the program will use the default configuration.

# Requirements

- Golang v1.18 or higher

# Installation

To install CFScanner, clone the repository and build the binary using the following commands:

```bash
git clone https://github.com/MortezaBashsiz/CFScanner.git
cd CFScanner/golang
go build
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
  "serverName": "SNI",
  "subnetsList": "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/bash/cf.local.iplist"
}
```

- NOTE: If you want to use your custom config DO NOT use it as config.real since script will update this file. Store your config in another file and pass it as an argument to script instead of config.real

- The configuration file are similer to the bash version

# Usage

To see CFScanner help , run the following command:

```bash
./CFScanner -h
```

CFScanner takes several arguments:

| Arguments              | Descriptions                                                                                     |
| ---------------------- | ------------------------------------------------------------------------------------------------ |
| --threads -t           | Number of threads to use for parallel scanning. Default is 1.                                    |
| --config -c            | The path to the config file.                                                                     |
| --vpn                  | If passed, test without creating VPN connections.                                                |
| --subnets -s           | The file or subnet. Each line should be in the form of ip.ip.ip.ip/subnet_mask or ip.ip.ip.ip.   |
| --upload               | If True, upload test will be conducted.                                                          |
| --fronting             | If True, fronting request test will be conducted.                                                |
| --tries                | Number of times to try each IP. An IP is marked as OK if all tries are successful. Default is 1. |
| --download-speed       | Minimum acceptable download speed in kilobytes per second. Default is 50.                        |
| --upload-speed         | Maximum acceptable upload speed in kilobytes per second. Default is 50.                          |
| --download-time        | Maximum (effective, excluding http time) time to spend for each download. Default is 2.          |
| --upload-time          | Maximum (effective, excluding http time) time to spend for each upload. Default is 2.            |
| --fronting-timeout     | Maximum time to wait for fronting response. Default is 1.0.                                      |
| --download-latency     | Maximum allowed latency for download. Default is 2.0.                                            |
| --upload-latency       | Maximum allowed latency for upload. Default is 2.0.                                              |
| --startprocess-timeout | Process timeout for v2ray. Default is 10.                                                        |
| --v2ray-path           | Custom V2Ray path for using v2ray binary on another directory.                                   |

# Examples

### Load configuration file and load subnet file for scanning

```bash
./CFScanner --config config.real --subnets ips.txt
```

### Load configuration file and use input cidr and begin scanning ips w

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

## Output

Two files are stored for each (complete) run of the program

- interim results file (e.g., `2023-03-10_20:49:30_result.csv`)
  - Includes the unsorted intermediate results in csv format. Useful in case if the scanning process is not complete.
- final results file (e.g., `2023-03-10_20:49:30_final.txt`)
  - Includes the final sorted results. The results are sorted ascendingly based on the download latency time.

# License

CFScanner is released under the [GPL-3](../LICENSE) license.

# Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](../CONTRIBUTING.md) for more information.

[go]: https://img.shields.io/badge/Go-cyan?logo=go
[version]: https://img.shields.io/badge/Version-1.0-blue
