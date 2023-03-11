## Requirements
You have to install the following packages:

[getopt](https://linux.die.net/man/3/getopt)<br>
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
   	"subnetsList": "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/bash/cf.local.iplist"
}
```

NOTE: If you want to use your custom config DO NOT use it as config.real since script will update this file. Store your config in another file and pass it as an argument to script instead of config.real

### 4. Execute it



#### in Linux:

At following command pay attention to the arguments **vpn** **SUBNET or IP** **DOWN or UP or BOTH**, **threads**, **tryCount**, **speed** and **Custom Subnet File**.
You have following switches to define the arguments 



-vpn: YES or NO, you are able to define for script to test with your vmess or not

-m: SUBNET or IP, Choose one of them for scanning subnets or single IPs

-t: DOWN or UP or BOTH, Choos one of them for download and upload test

-thr: This is an integer number that defines the parallel threads count

-try: This is an integer to define how many times you like to check an IP

-s: This is the filter that you can define to list the IPs based on download speed. The value is in KBPS (Kilo Bytes Per Second). For example, if you set it to 50, it means that you will only list the IPs which have a download speed of more than 50 KB/S.

-f: This is an optional argument which is a file address if you want to execute only some specific subnets. Then put your subnets in a file and pass the file as an argument to the command.

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -vpn <YES/NO>  -m <SUBNET/IP> -t <DOWN/UP/BOTH> -thr <int> -try <int> -c <config file> -s <int> [-f <Custome Subnet File> ]
```

#### in MacOS:

-v: YES or NO, you are able to define for script to test with your vmess or not ( -v first character of vpn )

-m: SUBNET or IP, Choose one of them for scanning subnets or single IPs

-t: DOWN or UP or BOTH, Choos one of them for download and upload test

-p: This is an integer number that defines the parallel threads count ( -p first character of parallelism factor )

-r: This is an integer to define how many times you like to check an IP ( -r first character of retry )

-s: This is the filter that you can define to list the IPs based on download speed. The value is in KBPS (Kilo Bytes Per Second). For example, if you set it to 50, it means that you will only list the IPs which have a download speed of more than 50 KB/S.

-f: This is an optional argument which is a file address if you want to execute only some specific subnets. Then put your subnets in a file and pass the file as an argument to the command.

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -v <YES/NO>  -m <SUBNET/IP> -t <DOWN/UP/BOTH> -p <int> -r <int> -c <config file> -s <int> [-f <Custome Subnet File> ]
```

#### EXAMPLE: without custom subnet download

##### in Linux:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -vpn YES -m SUBNET -t DOWN -thr 8 -try 1 -c config.real -s 100
```

##### in MacOS:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -v YES -m SUBNET -t DOWN -p 8 -r 1 -c config.real -s 100
```

#### EXAMPLE: without custom subnet upload

##### in Linux:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -vpn YES -m SUBNET -t UP -thr 8 -try 1 -c config.real -s 100
```

##### in MacOS:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -v YES -m SUBNET -t UP -p 8 -r 1 -c config.real -s 100
```


#### EXAMPLE: without custom subnet upload and download

##### in Linux:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -vpn YES -m SUBNET -t BOTH -thr 8 -try 1 -c config.real -s 100
```

##### in MacOS:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -v YES -m SUBNET -t BOTH -p 8 -r 1 -c config.real -s 100
```


#### EXAMPLE: with custom subnet

##### in Linux:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -vpn YES -m SUBNET -t DOWN -thr 8 -try 1 -c config.real -s 100 -f custom.subnets
```

##### in MacOS:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -v YES -m SUBNET -t DOWN -p 8 -r 1 -c config.real -s 100 -f custom.subnets
```

Which the `custom.subnets` is like as follows. You can edit this file and add your subnets in each line.

```shell
[~/CFScanner/bash]>$ cat custom.subnets 
5.226.179.0/24
203.89.5.0/24
[~/CFScanner/bash]>$
```

#### EXAMPLE: with custom ip file

#### in Linux:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -vpn YES -m IP -t DOWN -thr 8 -try 1 -c config.real -s 100 -f ip.list
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

#### in MacOS:

```shell
[~/CFScanner/bash]>$ bash cfScanner.sh -v YES -m IP -t DOWN -p 8 -r 1 -c config.real -s 100 -f ip.list
```

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
You can find a video guide for this script on [youtube](https://youtu.be/BKLRAHolhvM "youtube") and [youtube](https://youtu.be/4xJvWYdGuV8 "youtube").

