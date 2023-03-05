## Requirements
You have to install the following packages:

[jq](https://stedolan.github.io/jq/)<br>
[git](https://git-scm.com/)<br>
[tput](https://command-not-found.com/tput)<br>
[bc](https://www.gnu.org/software/bc/)<br>
[curl](https://curl.se/download.html)<br>
[parallel (version > 20220515)](https://www.gnu.org/software/parallel/)

## How to run
### 1. clone

```shell
[~]>$ git clone https://github.com/MortezaBashsiz/CFScanner.git
```

### 2. Change directory and make them executable

```shell
[~]>$ cd CFScanner/bash
[~/CFScanner/bash]> chmod +x ../bin/*
```

### 3. Get config.real

```shell
[~/CFScanner/bash]>$ curl -s https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/bash/ClientConfig.json -o config.real
```

In the config file the variables are
```shell
{
	"id": "User's UUID",
	"Host": "Host address which is behind Cloudflare",
	"Port": "Port which you are using behind Cloudflare on your origin server",
	"path": "Websocket endpoint like api20",
	"serverName": "SNI",
	"frontDomain": "fronting domain check",
	"scanDomain": "scan domain which is behind CF for download test"
}
```

NOTE: If you want to use your custom config DO NOT use it as config.real since script will update this file. Store your config in another file and pass it as an argument to script instead of config.real

### 4. Execute it

At following command pay attention to the arguments **SUBNET or IP** **DOWN or UP** **threads**, **tryCount**, **speed** and **Custom Subnet File**.

--mode: SUBNET or IP, Choose one of them for scanning subnets or single IPs

--test-type: DOWN or UP, Choos one of them for download and upload test

--thread: This is an integer number that defines the parallel threads count

--tryCount: This is an integer to define how many times you like to check an IP

--speed: This is the filter that you can define to list the IPs based on download speed. The value is in KBPS (Kilo Bytes Per Second). For example, if you set it to 50, it means that you will only list the IPs which have a download speed of more than 50 KB/S.

--file: This is an optional argument which is a file address if you want to execute only some specific subnets. Then put your subnets in a file and pass the file as an argument to the command.

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh  --mode <SUBNET/IP> --test-type <DOWN/UP> --thread <int> --tryCount <int> --config <config file> --speed <int> [--file <Custome Subnet File> ]
```

#### EXAMPLE: without custom subnet download

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh --mode SUBNET --test-type DOWN --thread 8 --tryCount 1 --config config.real --speed 100
```

#### EXAMPLE: without custom subnet upload

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh --mode SUBNET --test-type UP --thread 8 --tryCount 1 --config config.real --speed 100
```

#### EXAMPLE: with custom subnet

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh --mode SUBNET --test-type DOWN --thread 8 --tryCount 1 --config config.real --speed 100 --file custom.subnets
```

Which the `custom.subnets` is like as follows. You can edit this file and add your subnets in each line.

```shell
[~/CFScanner/bash]>$ cat custom.subnets 
5.226.179.0/24
203.89.5.0/24
[~/CFScanner/bash]>$
```

#### EXAMPLE: with custom ip file

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh --mode IP --test-type DOWN --thread 8 --tryCount 1 --config config.real --speed 100 --file ip.list
```

Which the `custom.subnets` is like as follows. You can edit this file and add your subnets in each line.

```shell
[~/CFScanner/bash]>$ cat ip.list
23.227.37.250 
23.227.37.252 
23.227.37.253 
23.227.37.254 
23.227.37.255 
23.227.38.1 
23.227.38.8 
23.227.38.2 
23.227.38.3 
23.227.38.6 
23.227.38.14 
23.227.38.11 
23.227.38.9 
23.227.38.4 
23.227.38.10 
23.227.38.7 
[~/CFScanner/bash]>$
```


### 5. Result

It will generate a file in datetime format in the result directory.

```shell
[~/CFScanner]>$ ls result/
20230120-203358-result.cf
[~/CFScanner]>$
```
## Video Guide
You can find a video guide for this script on [youtube](https://youtu.be/BKLRAHolhvM "youtube").
