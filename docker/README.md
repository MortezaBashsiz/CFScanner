# CloudFlare Scanner
This script scans Millions of Cloudflare IP addresses and generates a result file containing the IPs which are work with CDN.

This script uses v2ray+vmess+websocket+tls by default and if you want to use it behind your Cloudflare proxy then you have to set up a vmess account, otherwise, it will use the default configuration.

Docker repository page [HERE](https://hub.docker.com/r/bashsiz/cfscanner "HERE")

## How to run
### 1. pull image

```shell
[~]>$ sudo docker pull bashsiz/cfscanner:latest
```

### 2. make directories

```shell
[~]>$ mkdir -p /tmp/cfscanner/config /tmp/cfscanner/result
[~]>$ cd /tmp/cfscanner/
[/tmp/cfscanner]>$
```

### 3. Run docker image

```shell
[/tmp/cfscanner]>$ sudo docker run -v /tmp/cfscanner/config:/CFSCANNER/CFScanner/config -v /tmp/cfscanner/result:/CFSCANNER/CFScanner/result -it bashsiz/cfscanner:latest bash
root@1b2f73d5988c:/CFSCANNER# ls
CFScanner
root@1b2f73d5988c:/CFSCANNER/CFScanner# git pull
root@1b2f73d5988c:/CFSCANNER/CFScanner# cd bash/
root@1b2f73d5988c:/CFSCANNER/CFScanner/bash# ls
cf.local.iplist  cfScanner.sh  config.json.temp  config.script	custom.ips  custom.subnets
root@1b2f73d5988c:/CFSCANNER/CFScanner/bash#
```

### 4. Execute it

This step is same as before, you can see in the main [README](https://github.com/MortezaBashsiz/CFScanner/tree/main/bash/README.md "README"). of project

### 5. Result

You can find the result and config files in the directories that you mounted as volume to the container

```shell
[/tmp/cfscanner]>$ ls *
config:
config.json.104.16.0.1   config.json.104.16.0.17  config.json.104.16.0.24  config.json.104.16.0.31  config.json.104.16.0.8   config.json.104.16.1.15  config.json.104.16.1.8
config.json.104.16.0.11  config.json.104.16.0.18  config.json.104.16.0.25  config.json.104.16.0.32  config.json.104.16.0.9   config.json.104.16.1.16  config.json.104.16.1.9
config.json.104.16.0.12  config.json.104.16.0.19  config.json.104.16.0.27  config.json.104.16.0.33  config.json.104.16.1.1   config.json.104.16.1.2
config.json.104.16.0.13  config.json.104.16.0.2   config.json.104.16.0.28  config.json.104.16.0.34  config.json.104.16.1.11  config.json.104.16.1.3
config.json.104.16.0.14  config.json.104.16.0.21  config.json.104.16.0.29  config.json.104.16.0.4   config.json.104.16.1.12  config.json.104.16.1.4
config.json.104.16.0.15  config.json.104.16.0.22  config.json.104.16.0.3   config.json.104.16.0.5   config.json.104.16.1.13  config.json.104.16.1.5
config.json.104.16.0.16  config.json.104.16.0.23  config.json.104.16.0.30  config.json.104.16.0.6   config.json.104.16.1.14  config.json.104.16.1.6

result:
20230222-065934-result.cf
[/tmp/cfscanner]>$
```
