![python][python]
![version][version]

# CFScanner - Python

The script is designed to scan Cloudflare's edge IPs and locate ones that are viable for use with v2ray/xray. It aims to identify edge IPs that are accessible and not blocked.

CFSCanner runs on different operating systems including and not limited to:

- Linux
- MacOS
- Windows
- Android (termux, UserLAnd, etc.)

# Dependencies

* Python (>=3.6)
* Libraries
  - requests
  - pysocks

# Installing

## Install git and pip

```bash
sudo apt update && sudo apt install python3-pip git -y
```

## Clone the project

```bash
git clone https://github.com/MortezaBashsiz/CFScanner.git
```

## Change directory

```bash
cd CFScanner/python/
```

## Install required python packages

```bash
pip install -r ./requirements.txt
```

## Creating a custom config file (optional)

- If you want to use the default sudoer config, you can skip this step

* create a config json file (e.g., myconfig.json) with the following content. replace the values with your own!

```json
{
	"id": "248ecb72-89cf-5be7-923f-b790fca681c5",
	"host": "scherehtzhel01.sudoer.net",
	"port": "443",
	"path": "api01",
	"serverName": "248ecb72-89cf-5be7-923f-b790fca681c5.sudoer.net"
}
```

# Executing program

## **How to run**

In the following, you can find examples of running the script with and without custom config and subnets file. For more details on the arguments, please see [Arguments](#anchor-args)

* To run with sudoer default config and only one thread on the default subnets list:
  ```bash
  python3 cfscanner.py 
  ```
* To run with sudoer default config and 8 threads:
  ```bash
  python3 cfscanner.py -t 8
  ```

* To run on a list of subnets:
  ```bash
  python3 cfscanner.py -t 8 -c ./myconfig.json -s ./mysubnets.selection
  ```

  Each line of the file can be either a subnet (in CIDR notation) or a single IP (v4 or v6):
  ```
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

* To run with a minimum acceptable download speed of 100 kilobytes per second
  ```bash
  python3 cfscanner.py -t 8 -c ./myconfig.json -s ./mysubnets.selection -DS 100
  ```

* To run with a minimum acceptable download and upload speed (in KBps)
  ```bash
  python3 cfscanner.py -t 8 -c ./myconfig.json -s ./mysubnets.selection -DS 100 -US 25
  ```

* To run and try each IP multiple (in this case 3) times. An IP is marked ok if it passes all the tests.

  ```bash
  python3 cfscanner.py --upload-test --threads 8 --config ./myconfig.json --subnets ./mysubnets.selection --download-speed 100 --upload-speed 25 --tries 3
  ```
---

## <a name="anchor-args"></a>Arguments

To use this tool, you can specify various options as follows:

#### Help

To see the help message, use the `--help` or `-h` option.

#### General Options

* `--threads`, `-t`: Number of threads to use for parallel scanning. Default value is 1.
* `--tries`, `-n`: Number of times to try each IP. An IP is marked as OK if all tries are successful. Default value is 1.
* `--subnets`, `-s`: The path to the custom subnets file. Each line should be either a single ip (v4 or v6) or a
  subnet in cidr notation (v4 or v6). If not provided, the program will
  read the list of cidrs from [https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/cf.local.iplist](https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/cf.local.iplist).

#### Xray Config Options

* `--config`, `-c`: The path to the config file. For config file example, see [sudoer default config](https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/ClientConfig.json). If not provided, the program will read the [default sudoer config](https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/ClientConfig.json) file.
* `--template`: Path to the proxy (v2ray/xray) client file template. By default vmess_ws_tls is used.
* `--binpath`, `-b`: Path to the v2ray/xray binary file. If not provided, will use the latest compatible version of xray.
* `--novpn`: If passed, xray/v2ray service will not be started and the program will not use vpn.
* `--startprocess-timeout`: Maximum time (in seconds) to wait for xray/v2ray process to start. Default value is 5.

#### Fronting Speed Test Options

* `--fronting-timeout`, `-FT`: Maximum time to wait for fronting response. Default value is 1.

#### Download Speed Test Options

* `--download-speed`, `-DS`: Minimum acceptable download speed in kilobytes per second. Default value is 50.
* `--download-latency`, `-DL`: Maximum allowed latency (seconds) for download. Default value is 2.
* `--download-time`, `-DT`: Maximum (effective, excluding http time) time to spend for each download. Default value is 2.

#### Upload Speed Test Options

* `--upload-test`, `-U`: If passed, upload test will be conducted. If not passed, only download and fronting test will be conducted.
* `--upload-speed`, `-US`: Minimum acceptable upload speed in kilobytes per second. Default value is 50.
* `--upload-latency`, `-UL`: Maximum allowed latency (seconds) for upload. Default value is 2.
* `--upload-time`, `-UT`: Maximum (effective, excluding http time) time (in seconds) to spend for each upload. Default value is 2.

## Remarks

* In the current version, an IP is marked "OK", only if it passes all tries of the experiment
* The size of the file for download is determined based on the arguments ``download-speed`` and ``download-time`` (similar for upload as well). Therefore, it is recommended to choose these parameters carefully based on your expectations, internet speed and the number of threads being used

# **Results**

The results will be stored in the ``results`` directory. Each line of the generated **csv** file includes a Cloudflare edge ip together with the following values:

* ``avg_download_speed ``: Average download speed in mbps
* ``avg_upload_speed``: Average upload speed in mbps
* ``avg_download_latency``: Average download latency in ms
* ``avg_upload_latency``: Average upload latency in ms
* ``avg_download_jitter``: Average jitter during downloads in ms
* ``avg_upload_jitter``: Average jitter during uploads in ms
* ``download_speed_1,...,n_tries``: Values of download speeds in mbps for each download
* ``upload_speed_1,...,n_ties``: Values of download speeds in mbps for each upload
* ``download_latency_1,...,n_tries``: Values of download latencies in ms
* ``download_latency_1,..._n_tries``: Values of upload latencies in ms

---

For each time running the code, a result file is generated in the result folder with the datetime string to avoid overwriting (e.g, ``20230226_180502_result.csv``)

# Authors

Contributors names and contact info

* [Tempookian](https://github.com/tempookian)
* [Morteza Bashsiz](https://github.com/MortezaBashsiz/)

# Version History

* 0.1
  * Initial Releas
* 1.0.0
  * Automatic download of xray binary
  * Use xray by default
  * Arguments reorginazed
  * Changed default behavior when a subnet list is not provided. The list is read from the repo and not asnlookup anymore
* 1.0.1
  * Fixed a bug in detect system (issue [#385](https://github.com/MortezaBashsiz/CFScanner/issues/385))

[python]: https://img.shields.io/badge/-Python-3776AB?logo=python&logoColor=white
[version]: https://img.shields.io/badge/Version-1.0.1-blue
