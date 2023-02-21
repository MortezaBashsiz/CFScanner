#!/usr/bin/env python

import sys
import os
import traceback
import time
import ipaddress
import http.client
import requests
import urllib3
import multiprocessing
import subprocess
import signal
from requests.adapters import HTTPAdapter

v2rayConfigTemplate = """
{
  "inbounds": [{
    "port": PORTPORT,
    "listen": "127.0.0.1",
    "tag": "socks-inbound",
    "protocol": "socks",
    "settings": {
      "auth": "noauth",
      "udp": false,
      "ip": "127.0.0.1"
    },
    "sniffing": {
      "enabled": true,
      "destOverride": ["http", "tls"]
    }
  }],
  "outbounds": [
    {
		"protocol": "vmess",
    "settings": {
      "vnext": [{
        "address": "IP.IP.IP.IP", 
        "port": CFPORTCFPORT,
        "users": [{"id": "IDID" }]
      }]
    },
		"streamSettings": {
        "network": "ws",
        "security": "tls",
        "wsSettings": {
            "headers": {
                "Host": "HOSTHOST"
            },
            "path": "ENDPOINTENDPOINT"
        },
        "tlsSettings": {
            "serverName": "RANDOMHOST",
            "allowInsecure": false
        }
    }
	}],
  "other": {}
}
"""

class clsV2rayConfig:
    localPort = ""
    addressIP = ""
    addressPort = ""
    userId = ""
    wsHeaderHost = ""
    wsHeaderPath = ""
    tlsServerName = ""
    confDir = ""
    resultDir = ""
    binDir = ""

class clsColors:
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'

class clsFrontingAdapter(HTTPAdapter):
    """"Transport adapter" that allows us to use SSLv3."""

    def __init__(self, fronted_domain=None, **kwargs):
        self.fronted_domain = fronted_domain
        super(clsFrontingAdapter, self).__init__(**kwargs)

    def send(self, request, **kwargs):
        connection_pool_kwargs = self.poolmanager.connection_pool_kw
        if self.fronted_domain:
            connection_pool_kwargs["assert_hostname"] = self.fronted_domain
        elif "assert_hostname" in connection_pool_kwargs:
            connection_pool_kwargs.pop("assert_hostname", None)
        return super(clsFrontingAdapter, self).send(request, **kwargs)

    def init_poolmanager(self, *args, **kwargs):
        server_hostname = None
        if self.fronted_domain:
            server_hostname = self.fronted_domain
        super(clsFrontingAdapter, self).init_poolmanager(server_hostname=server_hostname, *args, **kwargs)

def fncGenPort(ip):
    octetList = ip.split(".")
    octetSum = 0
    for octet in octetList:
        octetSum += int(octet)
    port = f"3{octetSum}"
    return port

def fncV2rayCheck(v2rayConfig):
    v2rayConfig.localPort = fncGenPort(v2rayConfig.addressIP)
    config = v2rayConfigTemplate.replace("PORTPORT", v2rayConfig.localPort)
    config = config.replace("IP.IP.IP.IP", v2rayConfig.addressIP)
    config = config.replace("CFPORTCFPORT", v2rayConfig.addressPort)
    config = config.replace("IDID", v2rayConfig.userId)
    config = config.replace("HOSTHOST", v2rayConfig.wsHeaderHost)
    config = config.replace("ENDPOINTENDPOINT", v2rayConfig.wsHeaderPath)
    config = config.replace("RANDOMHOST", v2rayConfig.tlsServerName)
    configPath = f"{v2rayConfig.configDir}/config.json.{v2rayConfig.addressIP}"
    configFile = open(configPath, "w")
    configFile.write(config)
    configFile.close()
    #v2rayProcess = subprocess.Popen([f"{v2rayConfig.binDir}/v2ray","-c",f"{configPath}"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 
    time.sleep(2)
    #v2rayProcess.kill()
    print(f"{clsColors.OKGREEN} OK {clsColors.OKBLUE} {v2rayConfig.addressIP} {clsColors.ENDC}")

def fncDomainCheck(ipList, v2rayConfig):
    for ip in ipList:
        realIP=str(ip).replace('/32', '')
        realUrl=f"https://{realIP}/"
        session = requests.Session()
        session.mount('https://', clsFrontingAdapter(fronted_domain="fronting.sudoer.net"))
        try:
            response = session.get(realUrl, headers={"Host": "fronting.sudoer.net"})
            if response.status_code == 200:
                v2rayConfig.addressIP = realIP
                fncV2rayCheck(v2rayConfig)
            else:
                print(f"{clsColors.FAIL} NO {clsColors.WARNNING} {realIP} {clsColors.ENDC}")
        except Exception:
            traceback.print_exc()
            print(f"{clsColors.FAIL} NO {clsColors.FAIL} {realIP} {clsColors.ENDC}")

def fncSplit(listInput, chunk_size):
  for i in range(0, len(listInput), chunk_size):
    yield listInput[i:i + chunk_size]

def fncCreateDir(dirPath):
    isExist = os.path.exists(dirPath)
    if not isExist:
        os.makedirs(dirPath)
        print(f"Directory created : {dirPath}")

def fncReadConfig(configPath):
    v2rayConfig = clsV2rayConfig()
    configFile = open(str(configPath), 'r')
    configList = configFile.readlines()
    v2rayConfig.addressPort="443"
    v2rayConfig.userId="0aa7d9c9-1a5e-5834-82e6-42a77b0b2fbb"
    v2rayConfig.wsHeaderHost="scherehtzflk01.schere.net"
    v2rayConfig.wsHeaderPath="/api09"
    v2rayConfig.tlsServerName="7f133f40-ae96-11ed-9820-0bc4a655c0b2.schere.net"
    return v2rayConfig

if __name__ == "__main__":
    scriptDir = os.path.dirname(os.path.realpath(__file__))
    configDir = f"{scriptDir}/../config" 
    resultDir = f"{scriptDir}/../result" 
    binDir = f"{scriptDir}/../bin" 
    fncCreateDir(configDir)
    fncCreateDir(resultDir)
    configFilePath = sys.argv[1]
    threadsCount=sys.argv[2]
    subnetFilePath = sys.argv[3]
    v2rayConfig = fncReadConfig(configFilePath)
    v2rayConfig.configDir = configDir
    v2rayConfig.resultDir = resultDir
    v2rayConfig.binDir = binDir
    subnetFile = open(str(subnetFilePath), 'r')
    subnetList = subnetFile.readlines()
    jobs = []
    bigIPList = []
    for subnet in subnetList:
        breakedSubnets = list(ipaddress.ip_network(subnet.strip()).subnets(new_prefix=24))
        for subnet in breakedSubnets:
            ipList = list(ipaddress.ip_network(subnet).subnets(new_prefix=32))
            for ip in ipList:
                realIP = str(ip).replace('/32', '')
                bigIPList.append(realIP)

    chunkedList = list(fncSplit(bigIPList, int(threadsCount)))
    for chunkIP in chunkedList:
        process = multiprocessing.Process(target=fncDomainCheck, args=(chunkedIP, v2rayConfig, ))
        jobs.append(process)
        process.start()
    for job in jobs:
        job.join()


