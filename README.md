# CloudFlare Scanner
This script scans Millions of cloudflare IP addresses and generate a result file contains the IPs which are work with CDN

This script uses v2ray+vmess+websocket+tls by default and if you want to use it behind your Cloudflare proxy then you have to set up a vmess account, otherwise it will use the default config

## Requirements
You have to install following packages
```
git
tput
bc
curl
nmap
parallel (version > 20220515)
```

## How to run
### 1. clone

```shell
[~]>$ git clone https://github.com/MortezaBashsiz/CFScanner.git
```

### 2. Change direcotry and make them executable

```shell
[~]>$ cd CFScanner/scripts
[~/CFScanner/scripts]> chmod +x v2ctl v2ctl-mac v2ray v2ray-mac
```

### 3. Get config.real

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

### 4. Execute it

At following command pay attention to the numbers **threads** and **speed** **Custom Subnet File**

threads: This is an integer number which defines the parallel threads count

speed: This is the filter which you can define to list the IPs based on download speed. the values must be one of [25 50 100 150 200 250 500] and all values are in KBPS (Kilo Bytes Per Second). For example if you set it to 50 it means that you will only list the IPs which have download speed more than 50 KB/S.

Custom Subnet File: This is optional argument which is a file address if you want to execute only some specific subnets. Then put your subnets in a file and pass the file as argument to the command.

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh threads ./config.real speed [Custome Subnet File]
```

#### EXAMPLE: without costume subnet

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 8 ./config.real 100
```

#### EXAMPLE: with costume subnet

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 8 ./config.real 100 ./custome.subnets
```

Which the custome.subnets is like as following. you can edit this file and add your subnets in each line.

```shell
[~/CFScanner/scripts]>$ cat custome.subnets 
5.226.179.0/24
203.89.5.0/24
[~/CFScanner/scripts]>$
```

### 5. Result

It will generate a file by datetime in result direcotry

```shell
[~/CFScanner]>$ ls result/
20230120-203358-result.cf
[~/CFScanner]>$
```

## Video guide
A video guide usage can be found in [youtube](https://youtu.be/BKLRAHolhvM "youtube").
