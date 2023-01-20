# Sudoer Telegram Bot
This script scans all 1.5 Million cloudflare IP addresses and generate a result file contains the IPs which are work with CDN

## Requirements
You have to install following packages
```
curl
nmap
parallel
```


## How to run
1. clone

```shell
[~]>$ git clone git@github.com:MortezaBashsiz/CFScanner.git
```

2. Change direcotry

```shell
[~]>$ cd CFScanner/scripts
```
3. Execute it

You must specify the parallel process count. In this example I execute it in 16 simultanious processes

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 16 
```

4. Result
It will generate a file by datetime in result direcotry

```shell
[~/CFScanner]>$ ls result/
20230120-203358-result.cf
[~/CFScanner]>$
```
