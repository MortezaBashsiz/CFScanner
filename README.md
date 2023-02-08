# Sudoer Telegram Bot
This script scans all 1.5 Million cloudflare IP addresses and generate a result file contains the IPs which are work with CDN

## Requirements
You have to install following packages
```
bc
curl
nmap
parallel
```

## How to run
1. clone

```shell
[~]>$ git clone https://github.com/MortezaBashsiz/CFScanner.git
```

2. Change direcotry

```shell
[~]>$ cd CFScanner/scripts
```

3. Get config.real

```shell
[~/CFScanner/scripts]>$ curl -s http://bot.sudoer.net/config.real -o ./config.real
```

4. Execute it

You must specify the parallel process count. In this example I execute it in 16 simultanious processes

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 16 ./config.real
```

5. Result
It will generate a file by datetime in result direcotry

```shell
[~/CFScanner]>$ ls result/
20230120-203358-result.cf
[~/CFScanner]>$
```

## Video guide
A video guide usage can be found in [youtube](https://youtu.be/xzuMnxEw97U "youtube").
