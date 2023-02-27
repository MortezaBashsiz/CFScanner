#!/usr/bin/env python

import argparse
import ipaddress
import json
import multiprocessing
import os
import re
import socket
import socketserver
import subprocess
import sys
import time
import traceback
from datetime import datetime
from functools import partial

import requests
from requests.adapters import HTTPAdapter

V2RAY_CONFIG_TEMPLATE = """
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

SCRIPTDIR = os.path.dirname(os.path.realpath(__file__))
CONFIGDIR = f"{SCRIPTDIR}/../config"
RESULTDIR = f"{SCRIPTDIR}/../result"
BINDIR = f"{SCRIPTDIR}/../bin"
START_DT_STR = datetime.now().strftime(r"%Y%m%d_%H%M%S")
INTERIM_RESULTS_PATH = os.path.join(
    RESULTDIR, START_DT_STR + '_interim_result.txt')


class clsV2rayConfig(dict):
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
        super(clsFrontingAdapter, self).init_poolmanager(
            server_hostname=server_hostname, *args, **kwargs)


def fncGenPort(ip):
    with socketserver.TCPServer(("localhost", 0), None) as s:
        free_port = s.server_address[1]
    return free_port


def create_v2ray_config(
    v2rayConfig: clsV2rayConfig
) -> str:
    """creates v2ray config json file based on ``clsV2rayConfig`` instance

    Args:
        v2rayConfig (clsV2rayConfig): contains information about the v2ray config

    Returns:
        str: the path to the json file created
    """
    v2rayConfig.localPort = fncGenPort(v2rayConfig.addressIP)
    local_port_str = str(v2rayConfig.localPort)
    config = V2RAY_CONFIG_TEMPLATE.replace("PORTPORT", local_port_str)
    config = config.replace("IP.IP.IP.IP", v2rayConfig.addressIP)
    config = config.replace("CFPORTCFPORT", v2rayConfig.addressPort)
    config = config.replace("IDID", v2rayConfig.userId)
    config = config.replace("HOSTHOST", v2rayConfig.wsHeaderHost)
    config = config.replace("ENDPOINTENDPOINT", v2rayConfig.wsHeaderPath)
    config = config.replace("RANDOMHOST", v2rayConfig.tlsServerName)

    config_path = f"{v2rayConfig.configDir}/config.json.{v2rayConfig.addressIP}"
    with open(config_path, "w") as configFile:
        configFile.write(config)

    return config_path


def wait_for_port(
    port: int,
    host: str = 'localhost',
    timeout: float = 5.0
) -> None:
    """Wait until a port starts accepting TCP connections.
    Args:
        port: Port number.
        host: Host address on which the port should exist.
        timeout: In seconds. How long to wait before raising errors.
    Raises:
        TimeoutError: The port isn't accepting connection after time specified in `timeout`.
    """
    start_time = time.perf_counter()
    while True:
        try:
            with socket.create_connection((host, port), timeout=timeout):
                break
        except OSError as ex:
            time.sleep(0.01)
            if time.perf_counter() - start_time >= timeout:
                raise TimeoutError('Waited too long for the port {} on host {} to start accepting '
                                   'connections.'.format(port, host)) from ex


def v2ray_speed_test(
    v2ray_conf_path: str,
    test_file: str = "data.100k",
    test_url: str = "https://scan.sudoer.net/"
) -> float:
    """tests the download speed of a v2ray config and returns the response time

    Args:
        v2ray_conf_path (str): the path to the v2ray config json file 
        test_file (str, optional): the name of the file to the file to download. Defaults to "data.100k".
        test_url (str, optional): the host to download the file from. Defaults to "https://scan.sudoer.net/".

    Returns:
        foat: total seconds took to download the file. -1 if unsuccessful
    """
    with open(v2ray_conf_path, "r") as infile:
        v2ray_conf = json.load(infile)

    v2ray_listen = v2ray_conf["inbounds"][0]["listen"]
    v2ray_port = v2ray_conf["inbounds"][0]["port"]

    outbound_address = v2ray_conf["outbounds"][0]["settings"]["vnext"][0]["address"]

    v2ray_process = subprocess.Popen(
        [os.path.join(BINDIR, "v2ray"), "-c", f"{v2ray_conf_path}"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )

    try:
        wait_for_port(host=v2ray_listen, port=v2ray_port, timeout=5)
    except Exception as e:
        # v2ray could not start
        print(
            f"{clsColors.FAIL} Could not start v2ray - {v2ray_listen}:{v2ray_port} {clsColors.ENDC}")
        traceback.print_exc()

    proxies = dict(
        http=f"socks5://{v2ray_listen}:{v2ray_port}",
        https=f"socks5://{v2ray_listen}:{v2ray_port}"
    )

    response_time = -1.0
    try:
        url = f"{test_url.strip('/')}/{test_file.strip('/')}"
        r = requests.get(
            url=url,
            proxies=proxies,
            allow_redirects=True,
            timeout=3
        )
        response_time = r.elapsed.total_seconds()
        print(f"{clsColors.OKGREEN} OK {clsColors.OKBLUE} {outbound_address:15s} - {response_time*1000:4.0f}ms {clsColors.ENDC}")

    except requests.exceptions.ReadTimeout as e:
        print(
            f"{clsColors.FAIL} NO {clsColors.WARNING} {outbound_address:15s} - v2ray timeout {clsColors.ENDC}"
        )
    except requests.exceptions.ConnectionError as e:
        # v2ray connection does not work. the ip is not ok
        print(
            f"{clsColors.FAIL} NO {clsColors.WARNING} {outbound_address:15s} - v2ray fail - Connection error {clsColors.ENDC}"
        )
        try:
            os.remove(v2ray_conf_path)
        except Exception as e:
            # file could not be deleted
            pass
        # traceback.print_exc()
    except Exception as e:
        print(type(e))
        traceback.print_exc()
        print(
            f"{clsColors.FAIL} NO {clsColors.WARNING} {outbound_address:15s} - v2ray fail - Unknown error {clsColors.ENDC}"
        )
        try:
            os.remove(v2ray_conf_path)
        except Exception as e:
            # file could not be deleted
            pass
    finally:
        v2ray_process.kill()

    return response_time


def check_domain(ip, v2rayConfig, max_allowed_response_time=2):
    realIP = str(ip).replace('/32', '')
    realUrl = f"https://{realIP}/"
    session = requests.Session()
    response_time = -1
    session.mount(
        'https://', clsFrontingAdapter(fronted_domain="fronting.sudoer.net"))
    try:
        response = session.get(
            realUrl, headers={"Host": "fronting.sudoer.net"})
        if response.status_code == 200:
            v2rayConfig.addressIP = realIP
            v2ray_config_path = create_v2ray_config(v2rayConfig)
            response_time = v2ray_speed_test(v2ray_conf_path=v2ray_config_path)
            if 0 < response_time < max_allowed_response_time:
                with open(INTERIM_RESULTS_PATH, "a") as outfile:
                    outfile.write(f"{response_time*1000:.0f} {realIP}\n")
        else:
            print(
                f"{clsColors.FAIL} NO {clsColors.WARNING} {realIP:15s} - fronting fail - status_code = {response.status_code} {clsColors.ENDC}")
            pass
    except Exception:
        traceback.print_exc()
        print(f"{clsColors.FAIL} NO {clsColors.FAIL} {realIP:15s} - fronting fail - Unknown error{clsColors.ENDC}")


def fncCreateDir(dirPath):
    isExist = os.path.exists(dirPath)
    if not isExist:
        os.makedirs(dirPath)
        print(f"Directory created : {dirPath}")


def read_config(configPath):
    v2rayConfig = clsV2rayConfig()

    properties = dict()
    with open(configPath, 'r') as file:
        for line in file:
            if line.strip():  # not empty lines
                key, value = line.strip().split(': ')
                properties[key] = value

    v2rayConfig.addressPort = properties["Port"]
    v2rayConfig.userId = properties["id"]
    v2rayConfig.wsHeaderHost = properties["Host"]
    v2rayConfig.wsHeaderPath = f"/{properties['path']}"
    v2rayConfig.tlsServerName = f"{properties['serverName']}"

    return v2rayConfig


def parse_args(args=sys.argv[1:]):
    parser = argparse.ArgumentParser(
        description='Cloudflare edge ips scanner to use with v2ray')
    parser.add_argument(
        "threads",
        help="Number of threads to use for parallel computing",
        type=int
    )
    parser.add_argument(
        "config_path",
        help="The path to the config file. For confg file example, see http://bot.sudoer.net/config.real",
        metavar="config-path",
        type=str
    )
    parser.add_argument(
        "subnets_path",
        help="(optional) The path to the custom subnets file. each line should be in the form of ip.ip.ip.ip/subnet_mask. If not provided, the program will read the cidrs from asn lookup",
        type=str,
        metavar="subnets-path",
        nargs="?",
    )
    return parser.parse_args(args)


def read_cidrs_from_asnlookup(
    asn_list: list = ["AS13335", "AS209242"]
) -> list:
    """reads cidrs from asn lookup 

    Args:
        asn_list (list, optional): a list of ASN codes to read from asn lookup. Defaults to ["AS13335", "AS209242"].

    Returns:
        list: The list of cidrs associated with ``asn_list``
    """
    cidrs = []
    for asn in asn_list:
        url = f"https://asnlookup.com/asn/{asn}/"

        try:
            r = requests.get(url)
            cidr_regex = r"(?:[0-9]{1,3}\.){3}[0-9]{1,3}\/[\d]+"
            this_cidrs = re.findall(cidr_regex, r.text)
            cidrs.extend(this_cidrs)
        except Exception as e:
            traceback.print_exc()
            print(
                f"{clsColors.FAIL}ERROR {clsColors.WARNING}Could not read asn {asn} from asnlookup{clsColors.ENDC}")

    return cidrs


def cidr_to_ip_list(
    cidr: str
) -> list:
    """converts a subnet to a list of ips

    Args:
        cidr (str): the cidr in the form of "ip/subnet"

    Returns:
        list: a list of ips associated with the subnet
    """
    ip_network = ipaddress.ip_network(cidr, strict=False)
    return (list(map(str, ip_network)))


def get_num_ips_in_cidr(cidr):
    """
    Returns the number of IP addresses in a CIDR block.
    """
    parts = cidr.split('/')

    try:
        subnet_mask = int(parts[1])
    except IndexError as e:
        subnet_mask = 32

    num_ips = (2**(32-subnet_mask))

    return num_ips


def save_results(
    results: list,
    save_path: str,
    sort=True
):
    """saves results to file

    Args:
        results (list): a list of (ms, ip) tuples
        save_path (str): the path to save the file
        sort (bool, optional): binary indicating if the results should be
        sorted based on the response time Defaults to True.
    """
    # clean the results and make sure the first element is integer
    results = [
        (int(float(l[0])), l[1])
        for l in results
    ]

    if sort:
        results.sort(key=lambda r: r[0])

    with open(save_path, "w") as outfile:
        outfile.write(
            "\n".join([
                " ".join(map(str, res)) for res in results
            ])
        )
        outfile.write("\n")


if __name__ == "__main__":
    fncCreateDir(CONFIGDIR)
    fncCreateDir(RESULTDIR)

    # create empty result file
    with open(INTERIM_RESULTS_PATH, "w") as emptyfile:
        pass

    args = parse_args()
    configFilePath = args.config_path
    threadsCount = args.threads

    if args.subnets_path:
        subnetFilePath = args.subnets_path
        with open(str(subnetFilePath), 'r') as subnetFile:
            cidr_list = [l.strip() for l in subnetFile.readlines()]
    else:
        cidr_list = read_cidrs_from_asnlookup()
    v2rayConfig = read_config(configFilePath)
    v2rayConfig.configDir = CONFIGDIR
    v2rayConfig.resultDir = RESULTDIR
    v2rayConfig.binDir = BINDIR

    n_total_ips = sum(get_num_ips_in_cidr(cidr) for cidr in cidr_list)
    print(f"Starting to scan {n_total_ips} ips...")

    big_ip_list = [ip for cidr in cidr_list for ip in cidr_to_ip_list(cidr)]
    with multiprocessing.Pool(processes=threadsCount) as pool:
        pool.map(partial(check_domain, v2rayConfig=v2rayConfig), big_ip_list)

    # store the final results
    with open(INTERIM_RESULTS_PATH, "r") as infile:
        results = [
            (int(float(l.strip().split()[0].strip())),
             l.strip().split()[1].strip())
            for l in infile.readlines()
        ]

    save_path = os.path.join(
        RESULTDIR,
        START_DT_STR + '_final_result.txt'
    )
    save_results(results, save_path, sort=True)
