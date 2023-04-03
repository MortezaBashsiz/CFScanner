# CFScanner - Python

The script is designed to scan Cloudflare's edge IPs and locate ones that are viable for use with v2ray/xray. It aims to identify edge IPs that are accessible and not blocked.

# Dependencies

* Linux
* Python (>=3.6)
* Libraries 
    - requests
    - pysocks
	- random

# Installings

* Install prerequisites
```bash
sudo apt update && sudo apt install python3-pip git -y
```
* Clone the project
```bash
git clone https://github.com/MortezaBashsiz/CFScanner.git
```
* Change directory
```bash
cd CFScanner/python/
```
* Install required packages
```bash
pip install -r ./requirements.txt
```
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
* To run without providing a subnets (CIDRs) file and using 8 threads: 
```bash
python3 cfFindIP.py --threads 8 --config ./myconfig.json
```
* To run on a list of subnets (recommended). Each line
```bash
python3 cfFindIP.py --threads 8 --config ./myconfig.json --subnets ./mysubnets.selection
```
* To run with a minimum acceptable download speed (in KBps)
```bash
python3 cfFindIP.py --threads 8 --config ./myconfig.json --subnets ./mysubnets.selection --download-speed 100
```
* To run with a minimum acceptable download and upload speed (in KBps)
```bash
python3 cfFindIP.py --upload-test --threads 8 --config ./myconfig.json --subnets ./mysubnets.selection --download-speed 100 --upload-speed 25
```
* To run and try each IP multiple (3 in this case) times. An IP is marked ok if it passes all the tests.
```bash
python3 cfFindIP.py --upload-test --threads 8 --config ./myconfig.json --subnets ./mysubnets.selection --download-speed 100 --upload-speed 25 --tries 3
```
* To run and randomly test 12 IPs from every CIDR
```bash
python3 cfFindIP.py --upload-test --threads 8 --config ./myconfig.json --subnets ./mysubnets.selection --download-speed 100 --upload-speed 25 --tries 3 --random 12
```


Each line of the subnets file must be a Cloudflare subnet (IPv4 or IPv6) in CIDR notation or a single IP (IPv4 or IPv6):
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

## **Arguments:**
**--threads|-thr**: Number of threads to use for parallel computing<br>
**--config|-c**: The path to the config file. For confg file example, see [ClientConfig.json](https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/ClientConfig.json)<br>
**--subnets|-sn**: The path to the custom subnets file. each line<br> should be either a single ip (v4 or v6) or a subnet in cidr notation (v4 or v6). If not provided, the program will read the cidrs (v4 only) from asn lookup <br>
**--tries**: Number of times to try each IP. An IP is marked as OK if **all** tries are successful. <br>
**--download-speed**: Minimum acceptable download speed in kilobytes per second <br>
**--upload-test**: If True, upload test will be conducted as well <br>
**--upload-speed**: Mimimum Maximum acceptable upload speed in kilobytes per second <br>
**--download-time**: Maximum (effective, excluding latency) time to spend for each download. <br> 
**--upload-time**: Maximum (effective, excluding latency) time to spend for each upload <br>
**--download-latency**: Maximum allowed latency (seconds) for download <br>
**--upload-latency**: Maximum allowed latency (seconds) for upload <br>
**--use-xray**: If true, xray will be used, otherwise v2ray

---

## Remarks
* In the current version, an IP is marked "OK", only if it passes all tries of the experiment
* The size of the file for download is determined based on the arguments ``download-speed`` and ``download-time`` (similar for upload as well). Therefore, it is recommended to choose these parameters carefully based on your expectations, internet speed and the number of threads being used

# **Results**
The results will be stored in the ``results`` directory. Each line of the generated **csv** file includes a Cloudflare edge ip together with the following values:
* ``avg_download_speed
``: Average download speed in mbps
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
    * Initial Release

# License



