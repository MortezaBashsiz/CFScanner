# CloudFlare Scanner
This script scans Millions of Cloudflare IP addresses and generates a result file containing the IPs which are work with CDN.

This script uses v2ray+vmess+websocket+tls by default and if you want to use it behind your Cloudflare proxy then you have to set up a vmess account, otherwise, it will use the default configuration.

## Requirements
You have to install the following packages:

[git](https://git-scm.com/)<br>
[tput](https://command-not-found.com/tput)<br>
[bc](https://www.gnu.org/software/bc/)<br>
[curl](https://curl.se/download.html)<br>
[nmap](https://nmap.org/book/install.html)<br>
[parallel (version > 20220515)](https://www.gnu.org/software/parallel/)

## How to run
### 1. clone

```shell
[~]>$ git clone https://github.com/MortezaBashsiz/CFScanner.git
```

### 2. Change directory and make them executable

```shell
[~]>$ cd CFScanner/scripts
[~/CFScanner/scripts]> chmod +x v2ctl v2ctl-mac v2ray v2ray-mac
```

### 3. Get config.real

```shell
[~/CFScanner/scripts]>$ curl -s http://bot.sudoer.net/config.real -o ./config.real
```

In the config file the variables are
```shell
id:         User's UUID
Host:       Host address which is behind Cloudflare
Port:       Port which you are using behind Cloudflare on your origin server
path:       Websocket endpoint like api20
serverName: SNI
```

### 4. Execute it

At following command pay attention to the numbers **threads**, **speed** and **Custom Subnet File**.

Threads: This is an integer number that defines the parallel threads count

Speed: This is the filter that you can define to list the IPs based on download speed. The values must be one of [25 50 100 150 200 250 500], and all values are in KBPS (Kilo Bytes Per Second). For example, if you set it to 50, it means that you will only list the IPs which have a download speed of more than 50 KB/S.

Custom Subnet File: This is an optional argument which is a file address if you want to execute only some specific subnets. Then put your subnets in a file and pass the file as an argument to the command.

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh threads ./config.real speed [Custome Subnet File]
```

#### EXAMPLE: without custom subnet

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 8 ./config.real 100
```

or you can run  

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 
```
the above command is equal to 

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 8 ./config.real 25
```


#### EXAMPLE: with custom subnet

```shell
[~/CFScanner/scripts]>$ bash cfFindIP.sh 8 ./config.real 100 ./custom.subnets
```

Which the `custom.subnets` is like as follows. You can edit this file and add your subnets in each line.

```shell
[~/CFScanner/scripts]>$ cat custom.subnets 
5.226.179.0/24
203.89.5.0/24
[~/CFScanner/scripts]>$
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
