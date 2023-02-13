# CloudFlare Scanner
This script scans Millions of cloudflare IP addresses and generate a result file contains the IPs which are work with CDN

This script uses v2ray+vmess+websocket+tls by default and if you want to use it behind your Cloudflare proxy then you have to set up a vmess account, otherwise it will use the default config

## Requirements
You have to install following packages
```
git
bc
curl
nmap
parallel (version > 20220515)
```

## How to run
1. clone

```shell
[~]>$ git clone https://github.com/MortezaBashsiz/CFScanner.git
```

2. Change direcotry and make them executable

```shell
[~]>$ cd CFScanner/scripts
[~/CFScanner/scripts]> chmod +x v2ctl v2ctl-mac v2ray v2ray-mac
```

3. Get config.real

```shell
[~/CFScanner/scripts]>$ curl -s http://bot.sudoer.net/config.real -o ./config.real
```

In config file the variables are
```shell
id: UUID for user
Host: Host address which ic behind Cloudflare
Port: Port which you are using behind Cloudflare on your origin server
path: websocket endpoint like api20
serverName: SNI
```

4. Execute it

At following command pay attention to the numbers **threads** and **speed**

threads: This is an integer number which defines the parallel threads count

speed: This is the filter which you can define to list the IPs based on download speed. the values must be one of [25 50 100 150 200 250 500] and all values are in KBPS (Kilo Bytes Per Second). For example if you set it to 50 it means that you will only list the IPs which have download speed more than 50 KB/S.

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh [threads] ./config.real [speed]
```

EXAMPLE
```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 8 ./config.real 100
```

5. Result

It will generate a file by datetime in result direcotry

```shell
[~/CFScanner]>$ ls result/
20230120-203358-result.cf
[~/CFScanner]>$
```

## Video guide
A video guide usage can be found in [youtube](https://youtu.be/BKLRAHolhvM "youtube").
