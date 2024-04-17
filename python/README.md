# CFScanner - Python

![PyPI - Version](https://img.shields.io/pypi/v/cfscanner)

1. [Introduction](#introduction)
2. [Dependencies](#dependencies)
3. [Installing (and upgrading)](#installing-and-upgrading)
   1. [Creating a custom config file (optional)](#creating-a-custom-config-file-optional)
4. [Executing program](#executing-program)
   1. [**How to run**](#how-to-run)
   2. [Arguments](#arguments)
      1. [Help](#help)
      2. [General Options](#general-options)
      3. [Random Scan Options](#random-scan-options)
      4. [Xray Config Options](#xray-config-options)
      5. [Fronting Speed Test Options](#fronting-speed-test-options)
      6. [Download Speed Test Options](#download-speed-test-options)
      7. [Upload Speed Test Options](#upload-speed-test-options)
5. [Results](#results)
6. [Remarks](#remarks)
7. [Authors](#authors)
8. [Version History](#version-history)

## Introduction

The script is designed to scan Cloudflare's edge IPs and locate ones that are viable for use with v2ray/xray. It aims to identify edge IPs that are accessible and not blocked.

CFScanner runs on different operating systems including and not limited to:

- Linux
- MacOS
- Windows
- Android (termux, UserLAnd, etc.)

## Dependencies

- Python (>=3.6)
- Libraries
  - requests
  - pysocks
  - rich
  - fancylogging

## Installing (and upgrading)

```bash
pip install cfscanner --upgrade
```

### Creating a custom config file (optional)

- If you want to use the default sudoer config, you can skip this step

- Create a config json file (e.g., myconfig.json) with the following content. Replace the values with your own!

```json
{
  "id": "248ecb72-89cf-5be7-923f-b790fca681c5",
  "host": "scherehtzhel01.sudoer.net",
  "port": "443",
  "path": "api01",
  "serverName": "248ecb72-89cf-5be7-923f-b790fca681c5.sudoer.net"
}
```

## Executing program

### **How to run**

In the following, you can find examples of running the script with and without custom config and subnets file. For more details on the arguments, please see [Arguments](#arguments)

- To run with sudoer default config and only one thread on the default subnets list:

  ```bash
  cfscanner.py
  ```

  alternatively, you could use the following command:

  ```bash
  python3 -m cfscanner
  ```

- To run with sudoer default config and 8 threads:

  ```bash
  cfscanner -t 8
  ```

- To run on a list of subnets:

  ```bash
  cfscanner -t 8 -c ./myconfig.json -s ./mysubnets.selection
  ```

  Each line of the file can be either a subnet (in CIDR notation) or a single IP (v4 or v6):

  ```txt
  1.0.0.0/24
  108.162.218.0/24
  108.162.236.0/22
  162.158.8.0/21
  162.158.60.0/24
  162.158.82.12
  2606:4700::/120
  2606:4700:3032::6815:3819
  ...
  ```

- To run with a minimum acceptable download speed of 100 kilobytes per second

  ```bash
  cfscanner -t 8 -c ./myconfig.json -s ./mysubnets.selection -DS 100
  ```

- To run with a minimum acceptable download and upload speed (in KBps)

  ```bash
  cfscanner -t 8 -c ./myconfig.json -s ./mysubnets.selection -DS 100 -US 25
  ```

- To run and try each IP multiple (in this case 3) times. An IP is marked ok if it passes all the tests.

  ```bash
  cfscanner --threads 8 --config ./myconfig.json --subnets ./mysubnets.selection --download-speed 100 --upload-speed 25 --tries 3
  ```

- To run on a random sample of size 20 of the subnets and minimum acceptable download and upload speed of 10 KBps with the default config

  ```bash
  cfscanner -t 8 -DS 10 -US 10 -r 20
  ```

---

### Arguments

To use this tool, you can specify various options as follows:

#### Help

To see the help message, use the `--help` or `-h` option.

#### General Options

- `--threads`, `-t`: Number of threads to use for parallel scanning. Default value is 1.
- `--tries`, `-n`: Number of times to try each IP. An IP is marked as OK if all tries are successful. Default value is 1.
- `--subnets`, `-s`: The path to the custom subnets file. Each line should be either a single ip (v4 or v6) or a
  subnet in cidr notation (v4 or v6). If not provided, the program will
  read the list of cidrs from [https://github.com/MortezaBashsiz/CFScanner/blob/main/config/cf.local.iplist](https://github.com/MortezaBashsiz/CFScanner/blob/main/config/cf.local.iplist).

#### Random Scan Options

- `--sample`, `-r`: Size of the random sample to take from each subnet. The sample size can either
  be a float between 0 and 1 ($0 < s < 1$) or an integer ($ s \ge 1$). If it is a float, it will be
  interpreted as a percentage of the subnet size. If it is an integer, it
  will be interpreted as the number of ips to take from each subnet. If
  not provided, the program will take all ips from each subnet
- `--shuffle-subnets`: If passed, the subnets will be shuffled before scanning.

#### Xray Config Options

- `--config`, `-c`: The path to the config file. For config file example, see [sudoer default config](https://github.com/MortezaBashsiz/CFScanner/blob/main/config/ClientConfig.json). If not provided, the program will read the [default sudoer config](https://github.com/MortezaBashsiz/CFScanner/blob/main/config/ClientConfig.json) file.
- `--template`: Path to the proxy (v2ray/xray) client file template. By default vmess_ws_tls is used.
- `--binpath`, `-b`: Path to the v2ray/xray binary file. If not provided, will use the latest compatible version of xray.
- `--novpn`: If passed, xray/v2ray service will not be started and the program will not use vpn.
- `--startprocess-timeout`: Maximum time (in seconds) to wait for xray/v2ray process to start. Default value is 5.

#### Fronting Speed Test Options

- `--fronting-timeout`, `-FT`: Maximum time to wait for fronting response. Default value is 1.
- `--no-fronting`: If passed, fronting speed test will not be performed.
- `--fronting-domain`, `-FD`: CNAME to speed.cloudflare.com (use only if speed.cloudflare.com is blocked by your ISP)  

#### Download Speed Test Options

- `--download-speed`, `-DS`: Minimum acceptable download speed in kilobytes per second. Default value is 50.
- `--download-latency`, `-DL`: Maximum allowed latency (seconds) for download. Default value is 2.
- `--download-time`, `-DT`: Maximum (effective, excluding http time) time to spend for each download. Default value is 2.

#### Upload Speed Test Options

- `--upload-test`, `-U`: If passed, upload test will be conducted. If not passed, only download and fronting test will be conducted.
- `--upload-speed`, `-US`: Minimum acceptable upload speed in kilobytes per second. Default value is 50.
- `--upload-latency`, `-UL`: Maximum allowed latency (seconds) for upload. Default value is 2.
- `--upload-time`, `-UT`: Maximum (effective, excluding http time) time (in seconds) to spend for each upload. Default value is 2.

## Results

The results will be stored in the `results` directory. Each line of the generated **csv** file includes a Cloudflare edge ip together with the following values:

- `avg_download_speed`: Average download speed in mbps
- `avg_upload_speed`: Average upload speed in mbps
- `avg_download_latency`: Average download latency in ms
- `avg_upload_latency`: Average upload latency in ms
- `avg_download_jitter`: Average jitter during downloads in ms
- `avg_upload_jitter`: Average jitter during uploads in ms
- `download_speed_1,...,n_tries`: Values of download speeds in mbps for each download
- `upload_speed_1,...,n_ties`: Values of download speeds in mbps for each upload
- `download_latency_1,...,n_tries`: Values of download latencies in ms
- `download_latency_1,..._n_tries`: Values of upload latencies in ms

---

For each time running the code, a result file is generated in the result folder with the datetime string to avoid overwriting (e.g, `20230226_180502_result.csv`)

## Remarks

- In the current version, an IP is marked "OK", only if it passes all tries of the experiment
- The size of the file for download is determined based on the arguments `download-speed` and `download-time` (similar for upload as well). Therefore, it is recommended to choose these parameters carefully based on your expectations, internet speed and the number of threads being used

## Authors

Contributors names and contact info

- [Tempookian](https://github.com/tempookian)
- [Morteza Bashsiz](https://github.com/MortezaBashsiz/)

## Version History

- 0.1
  - Initial Releas
- 1.0.0
  - Automatic download of xray binary
  - Use xray by default
  - Arguments reorginazed
  - Changed default behavior when a subnet list is not provided. The list is read from the repo and not asnlookup anymore
- 1.0.1
  - Fixed a bug in detect system (issue [#385](https://github.com/MortezaBashsiz/CFScanner/issues/385))
- 1.0.2
  - Fixed a bug in the min UL speed, especially for `min_upload_speed = 0`
- 1.0.3
  - Fixed a bug in custom config template
- 1.1.0
  - Added random sampling
- 1.2.0
  - Added progress bar
  - Improved logging
- 1.2.1
  - Fixed the bug for keyboard interrupt in windows
  - improved the keyboard interrupt handling in general
- 1.2.2
  - Improved error logging
  - Errors are logged into a file in case of an unexpected error
- 1.3.0
  - Added subnets shuffling option
- 1.3.1
  - Improved colors in logging
- 1.3.9
  - Several rounds with pypi
- 1.3.10
  - Improved the progress bar
- 1.3.11
  - fixed multiple printing bug
- 1.3.12
  - added setuptools to the dependencies
- 1.3.13
  - added removal of duplicate subnets (issue [#490])
  - reduced the file size used for fronting test
- 1.3.14
  - Fixed a bug in reading ips from url. The regex now supports single ips as well
- 1.3.15
  - Fixed a bug in reading subnets from file with full address in windows
- 1.3.16
  - Added no-fronting option
- 1.3.17
  - Improved the fronting test method (jafar method)
- 1.3.18
  - changed fronting domain
- 1.3.19
  - fixed a bug in xray config saving in windows. ":" is not allowed in windows file names
- 1.4.0
  - Improved memory usage
    - The program now uses a generator to read ips from file/url
    - The program uses [reservoir sampling](https://en.wikipedia.org/wiki/Reservoir_sampling) to select ips from the list
    - Sampling till time out after `sample-timeout` seconds (input argument, default 1). In this case there is no guarantee
      that the probability of selecting the different ips are equal
  - The main progress bar is now based on the number of subnets (not the total ips)
  - The program does not remove the duplicate subnets anymore due to the new logic and in favor of memory usage
  - Added info about the number of scanned ips and ok ips to the progress bar
- 1.4.2
  - Changed fronting domain
- 1.4.3
  - Changed fronting domain
- 1.5.0
  - Add fronting-domain option
  - Default to fronting without fronting domain
  - Default xray core version changed to v1.8.10
