#!/usr/bin/env python

import argparse
import ipaddress
import json
import multiprocessing
import os
import platform
import re
import signal
import socket
import socketserver
import statistics
import subprocess
import sys
import time
import traceback
from datetime import datetime
from functools import partial
from typing import Tuple

import requests
from clog import CLogger

log = CLogger("CFScanner-python")


SCRIPTDIR = os.path.dirname(os.path.realpath(__file__))
BINDIR = f"{SCRIPTDIR}/../bin"
CONFIGDIR = f"{SCRIPTDIR}/../config"
RESULTDIR = f"{SCRIPTDIR}/../result"
START_DT_STR = datetime.now().strftime(r"%Y%m%d_%H%M%S")
INTERIM_RESULTS_PATH = os.path.join(RESULTDIR, f'{START_DT_STR}_result.csv')


class TestConfig:
    local_port = 0
    address_port = 0
    user_id = ""
    ws_header_host = ""
    ws_header_path = ""
    sni = ""
    do_upload_test = False
    min_dl_speed = 99999.0  # kilobytes per second
    min_ul_speed = 99999.0  # kilobytes per second
    max_dl_time = -2.0  # seconds
    max_ul_time = -2.0  # seconds
    max_dl_latency = -1.0  # seconds
    max_ul_latency = -1.0  # seconds
    fronting_timeout = -1.0  # seconds
    startprocess_timeout = -1.0  # seconds
    n_tries = -1
    no_vpn = False
    use_xray = False
    proxy_config_template = ""


class _COLORS:
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'


def get_free_port():
    with socketserver.TCPServer(("localhost", 0), None) as s:
        free_port = s.server_address[1]
    return free_port


def create_proxy_config(
    edge_ip,
    test_config: TestConfig
) -> str:
    """creates proxy (v2ray/xray) config json file based on ``TestConfig`` instance

    Args:
        edge_ip (str): Cloudflare edge ip to use for the config
        test_config (TestConfig): instance of ``TestConfig`` containing information about the setting of the test

    Returns:
        config_path (str): the path to the json file created
    """
    test_config.local_port = get_free_port()
    local_port_str = str(test_config.local_port)
    config = test_config.proxy_config_template.replace(
        "PORTPORT", local_port_str)
    config = config.replace("IP.IP.IP.IP", edge_ip)
    config = config.replace("CFPORTCFPORT", str(test_config.address_port))
    config = config.replace("IDID", test_config.user_id)
    config = config.replace("HOSTHOST", test_config.ws_header_host)
    config = config.replace("ENDPOINTENDPOINT", test_config.ws_header_path)
    config = config.replace("RANDOMHOST", test_config.sni)

    config_path = f"{CONFIGDIR}/config-{edge_ip}.json"
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
                raise TimeoutError(
                    f'Timeout exceeded for the port {port} on host {host} to start accepting connections.'
                ) from ex


def fronting_test(
    ip: str,
    timeout: float
) -> bool:
    """conducts a fronting test on an ip and return true if status 200 is received

    Args:
        ip (str): ip for testing
        timeout (float): the timeout to wait for ``requests.get`` result

    Returns:
        bool: True if ``status_code`` is 200, False otherwise
    """
    s = requests.Session()
    s.get_adapter(
        'https://').poolmanager.connection_pool_kw['server_hostname'] = "speed.cloudflare.com"
    s.get_adapter(
        'https://').poolmanager.connection_pool_kw['assert_hostname'] = "speed.cloudflare.com"

    success = False
    try:
        compatible_ip = f"[{ip}]" if ":" in ip else ip
        r = s.get(
            f"https://{compatible_ip}",
            timeout=timeout,
            headers={"Host": "speed.cloudflare.com"}
        )
        if r.status_code != 200:
            print(
                f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} fronting error {r.status_code} {_COLORS.ENDC}")
        else:
            success = True
    except requests.exceptions.ConnectTimeout as e:
        print(
            f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} fronting connect timeout{_COLORS.ENDC}"
        )
    except requests.exceptions.ReadTimeout as e:
        print(
            f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} fronting read timeout{_COLORS.ENDC}"
        )
    except requests.exceptions.ConnectionError as e:
        print(
            f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} fronting connection error{_COLORS.ENDC}"
        )
    except Exception as e:
        f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s}fronting Unknown error{_COLORS.ENDC}"
        log.error(f"Fronting test Unknown error {ip:15}")
        log.exception(e)

    return success


def current_platform() -> str:
    """Get current platform name by short string."""
    if sys.platform.startswith('linux'):
        return 'linux'
    elif sys.platform.startswith('darwin'):
        if "arm" in platform.processor().lower():
            return 'mac-arm'
        else:
            return 'mac'
    elif (
        sys.platform.startswith('win')
        or sys.platform.startswith('msys')
        or sys.platform.startswith('cyg')
    ):
        if sys.maxsize > 2 ** 31 - 1:
            return 'win64'
        return 'win32'
    raise OSError('Unsupported platform: ' + sys.platform)


def start_proxy_service(
    proxy_conf_path: str,
    timeout=5,
    use_xray=False
) -> Tuple[subprocess.Popen, dict]:
    """starts the proxy (v2ray/xray) service and waits for the respective port to open

    Args:
        proxy_conf_path (str): the path to the proxy (v2ray or xray) config json file
        timeout (int, optional): total time in seconds to wait for the proxy service to start. Defaults to 5.
        use_xray (bool, optional): if true, xray will be used, otherwise v2ray

    Returns:
        Tuple[subprocess.Popen, dict]: the v2 ray process object and a dictionary containing the proxies to use with ``requests.get`` 
    """
    if use_xray:
        if current_platform().startswith("mac"):
            binaryfile = os.path.join(BINDIR, "xray-mac")
        elif current_platform() == "linux":
            binaryfile = os.path.join(BINDIR, "xray")
    else:
        if current_platform().startswith("mac"):
            binaryfile = os.path.join(BINDIR, "v2ray-mac")
            v2ctl_binary = os.path.join(BINDIR, "v2ctl-mac")
        elif current_platform() == "linux":
            binaryfile = os.path.join(BINDIR, "v2ray")
            v2ctl_binary = os.path.join(BINDIR, "v2ctl")
        else:
            log.error(f"Platform {current_platform()} is not supported yet.")
            return None

    with open(proxy_conf_path, "r") as infile:
        proxy_conf = json.load(infile)

    proxy_listen = proxy_conf["inbounds"][0]["listen"]
    proxy_port = proxy_conf["inbounds"][0]["port"]

    if use_xray:
        proxy_process = subprocess.Popen(
            [binaryfile, "-c", proxy_conf_path],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )
    else:
        v2ctl_process = subprocess.Popen(
            [v2ctl_binary, "config", proxy_conf_path],
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL
        )
        proxy_process = subprocess.Popen(
            [binaryfile, "-config=stdin:", "-format=pb"],
            stdin=v2ctl_process.stdout,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )

    wait_for_port(host=proxy_listen, port=proxy_port, timeout=timeout)

    proxies = dict(
        http=f"socks5://{proxy_listen}:{proxy_port}",
        https=f"socks5://{proxy_listen}:{proxy_port}"
    )

    return proxy_process, proxies


def download_speed_test(
    n_bytes: int,
    proxies: dict,
    timeout: int
) -> Tuple[float, float]:
    """tests the download speed using cloudflare servers

    Args:
        n_bytes (int): size of file to download in bytes
        proxies (dict): the proxies to use for ``requests.get``
        timeout (int): the timeout for the download request        

    Returns:
        download_speed (float): the download speed in mbps
        latency (float): the round trip time latency in seconds
    """
    start_time = time.perf_counter()
    r = requests.get(
        url="https://speed.cloudflare.com/__down",
        params={"bytes": n_bytes},
        timeout=timeout,
        proxies=proxies
    )
    total_time = time.perf_counter() - start_time
    cf_time = float(r.headers.get("Server-Timing").split("=")[1]) / 1000
    latency = r.elapsed.total_seconds() - cf_time
    download_time = total_time - latency

    mb = n_bytes * 8 / (10 ** 6)
    download_speed = mb / download_time

    return download_speed, latency


def upload_speed_test(
    n_bytes: int,
    proxies: dict,
    timeout: int,
) -> Tuple[float, float]:
    """tests the upload speed using cloudflare servers

    Args:
        n_bytes (int): size of file to upload in bytes
        proxies (dict): the proxies to use for ``requests.post``
        timeout (int): the timeout for the download ``requests.post``   

    Returns:
        upload_speed (float): the upload speed in mbps
        latency (float): the rount trip time latency in seconds
    """

    start_time = time.perf_counter()
    r = requests.post(
        url="https://speed.cloudflare.com/__up",
        data="0" * n_bytes,
        timeout=timeout,
        proxies=proxies
    )
    total_time = time.perf_counter() - start_time
    cf_time = float(r.headers.get("Server-Timing").split("=")[1]) / 1000
    latency = total_time - cf_time

    mb = n_bytes * 8 / (10 ** 6)
    upload_speed = mb / cf_time

    return upload_speed, latency


def _raise_speed_timeout(signum, frame):
    raise TimeoutError("Download/upload too slow!")


class _FakeProcess:
    def __init__(self):
        pass

    def kill(self):
        pass


def check_ip(
    ip: str,
    test_config: TestConfig
):
    result = dict(
        ip=ip,
        download=dict(
            speed=[-1] * test_config.n_tries,
            latency=[-1] * test_config.n_tries
        ),
        upload=dict(
            speed=[-1] * test_config.n_tries,
            latency=[-1] * test_config.n_tries
        ),
    )

    for try_idx in range(test_config.n_tries):
        if not fronting_test(ip, timeout=test_config.fronting_timeout):
            return False

    try:
        proxy_config_path = create_proxy_config(ip, test_config)
    except Exception as e:
        log.error("Could not save proxy (xray/v2ray) config to file", ip)
        log.exception(e)
        return False

    if not test_config.no_vpn:
        try:
            process, proxies = start_proxy_service(
                proxy_conf_path=proxy_config_path,
                timeout=test_config.startprocess_timeout,
                use_xray=test_config.use_xray
            )
        except Exception as e:
            print(
                f"{_COLORS.FAIL}ERROR - {_COLORS.WARNING}Could not start proxy (v2ray/xray) service{_COLORS.ENDC}")
            log.exception(e)
            return False
    else:
        process = _FakeProcess()
        proxies = None

    for try_idx in range(test_config.n_tries):
        # check download speed
        n_bytes = test_config.min_dl_speed * 1000 * test_config.max_dl_time
        try:
            signal.signal(signal.SIGALRM, _raise_speed_timeout)
            signal.setitimer(signal.ITIMER_REAL,
                             test_config.max_dl_latency + test_config.max_dl_time)
            dl_speed, dl_latency = download_speed_test(
                n_bytes=n_bytes,
                proxies=proxies,
                timeout=test_config.max_dl_latency  # not sure if this is too generous or not
            )
        except (requests.exceptions.ReadTimeout, requests.exceptions.ConnectionError, requests.ConnectTimeout, TimeoutError) as e:
            if "Download/upload too slow".lower() in traceback.format_exc().lower():
                print(
                    f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} download too slow")
            else:
                print(
                    f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} download error{_COLORS.ENDC}")
            process.kill()
            return False
        except Exception as e:
            print(
                f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} download unknown error{_COLORS.ENDC}")
            log.error("Download - unknown error", ip)
            log.exception(e)
            process.kill()
            return False
        finally:
            signal.setitimer(signal.ITIMER_REAL, 0)

        if dl_latency <= test_config.max_dl_latency:
            dl_speed_kBps = dl_speed / 8 * 1000
            if dl_speed_kBps >= test_config.min_dl_speed:
                result["download"]["speed"][try_idx] = dl_speed
                result["download"]["latency"][try_idx] = round(
                    dl_latency * 1000)
            else:
                print(
                    f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} download too slow {dl_speed_kBps:.4f} kBps < {test_config.min_dl_speed:.4f} kBps{_COLORS.ENDC}")
                process.kill()
                return False
        else:
            print(f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} high download latency {dl_latency:.4f} s > {test_config.max_dl_latency:.4f} s{_COLORS.ENDC}")
            process.kill()
            return False

        # upload speed test
        if test_config.do_upload_test:
            n_bytes = test_config.min_ul_speed * 1000 * test_config.max_ul_time
            try:
                up_speed, up_latency = upload_speed_test(
                    n_bytes=n_bytes,
                    proxies=proxies,
                    timeout=test_config.max_ul_latency + test_config.max_ul_time
                )
            except requests.exceptions.ReadTimeout:
                return _extracted_from_check_ip_(ip, ' upload read timeout', process)
            except requests.exceptions.ConnectTimeout:
                return _extracted_from_check_ip_(ip, ' upload connect timeout', process)
            except requests.exceptions.ConnectionError:
                return _extracted_from_check_ip_(ip, ' upload connection error', process)
            except Exception as e:
                print(
                    f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s}upload unknown error{_COLORS.ENDC}")
                log.error("Upload - unknown error", ip)
                log.exception(e)
                process.kill()
                return False

            if up_latency > test_config.max_ul_latency:
                return _extracted_from_check_ip_(ip, ' upload latency too high', process)
            up_speed_kBps = up_speed / 8 * 1000
            if up_speed_kBps >= test_config.min_ul_speed:
                result["upload"]["speed"][try_idx] = up_speed
                result["upload"]["latency"][try_idx] = round(up_latency * 1000)
            else:
                print(
                    f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s} download too slow {dl_speed_kBps:.4f} kBps < {test_config.min_dl_speed:.4f} kBps{_COLORS.ENDC}")
                process.kill()
                return False
    process.kill()
    return result


# TODO Rename this here and in `check_ip`
def _extracted_from_check_ip_(ip, arg1, process):
    print(f"{_COLORS.FAIL}NO {_COLORS.WARNING}{ip:15s}{arg1}{_COLORS.ENDC}")
    process.kill()
    return False


def create_dir(dir_path):
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)
        print(f"Directory created : {dir_path}")


def create_test_config(args):
    with open(args.config_path, 'r') as infile:
        jsonfilecontent = json.load(infile)

    test_config = TestConfig()

    # proxy related config
    test_config.user_id = jsonfilecontent["id"]
    test_config.ws_header_host = jsonfilecontent["host"]
    test_config.address_port = int(jsonfilecontent["port"])
    test_config.sni = jsonfilecontent["serverName"]
    test_config.user_id = jsonfilecontent["id"]
    test_config.ws_header_path = "/" + (jsonfilecontent["path"].lstrip("/"))
    test_config.use_xray = args.use_xray
    with open(args.template_path, "r") as infile:
        test_config.proxy_config_template = infile.read()

    # speed related config
    test_config.startprocess_timeout = args.startprocess_timeout
    test_config.do_upload_test = args.do_upload_test
    test_config.min_dl_speed = args.min_dl_speed
    test_config.min_ul_speed = args.min_ul_speed
    test_config.max_dl_time = args.max_dl_time
    test_config.max_ul_time = args.max_ul_time
    test_config.fronting_timeout = args.fronting_timeout
    test_config.max_dl_latency = args.max_dl_latency
    test_config.max_ul_latency = args.max_ul_latency
    test_config.n_tries = args.n_tries
    test_config.no_vpn = args.no_vpn

    return test_config


def parse_args(args=sys.argv[1:]):
    parser = argparse.ArgumentParser(
        description='Cloudflare edge ips scanner to use with v2ray or xray')
    parser.add_argument(
        "--threads", "-thr",
        dest="threads",
        metavar="threads",
        help="Number of threads to use for parallel computing",
        type=int,
        required=True
    )
    parser.add_argument(
        "--config", "-c",
        help="The path to the config file. For confg file example, see https://github.com/MortezaBashsiz/CFScanner/blob/main/bash/ClientConfig.json",
        metavar="config-path",
        dest="config_path",
        type=str,
        required=False
    ),
    parser.add_argument(
        "--novpn",
        help="If passed, test without creating vpn connections",
        action="store_true",
        dest="no_vpn",
        default=False,
        required=False
    )
    parser.add_argument(
        "--subnets", "-sn",
        help="(optional) The path to the custom subnets file. each line should be in the form of ip.ip.ip.ip/subnet_mask or ip.ip.ip.ip. If not provided, the program will read the cidrs from asn lookup",
        type=str,
        metavar="subnets-path",
        dest="subnets_path",
        required=False
    ),
    parser.add_argument(
        "--upload-test",
        help="If True, upload test will be conducted",
        dest="do_upload_test",
        action="store_true",
        default=False,
        required=False
    )
    parser.add_argument(
        "--tries",
        metavar="n-tries",
        help="Number of times to try each IP. An IP is marked as OK if all tries are successful",
        dest="n_tries",
        default=1,
        type=int,
        required=False
    )

    parser.add_argument(
        "--download-speed", "-ds",
        help="Minimum acceptable download speed in kilobytes per second",
        type=int,
        dest="min_dl_speed",
        default=50,
        required=False
    )
    parser.add_argument(
        "--upload-speed", "-us",
        help="Maximum acceptable upload speed in kilobytes per second",
        type=int,
        dest="min_ul_speed",
        default=50,
        required=False
    )
    parser.add_argument(
        "--download-time", "-dt",
        help="Maximum (effective, excluding http time) time to spend for each download",
        type=int,
        dest="max_dl_time",
        default=2,
        required=False
    )
    parser.add_argument(
        "--upload-time", "-ut",
        metavar="max-upload-time",
        help="Maximum (effective, excluding http time) time to spend for each upload",
        type=int,
        dest="max_ul_time",
        default=2,
        required=False
    )
    parser.add_argument(
        "--fronting-timeout",
        metavar="fronting-timeout",
        help="Maximum time to wait for fronting response",
        type=float,
        dest="fronting_timeout",
        default=1,
        required=False
    )
    parser.add_argument(
        "--download-latency",
        help="Maximum allowed latency for download",
        metavar="max-upload-latency",
        type=int,
        dest="max_dl_latency",
        default=2,
        required=False
    )
    parser.add_argument(
        "--upload-latency",
        help="Maximum allowed latency for download",
        type=int,
        metavar="max-upload-latency",
        dest="max_ul_latency",
        default=2,
        required=False
    )
    parser.add_argument(
        "--startprocess-timeout",
        help=argparse.SUPPRESS,
        type=float,
        dest="startprocess_timeout",
        default=5
    )
    parser.add_argument(
        "--use-xray",
        help="Use xray instead of v2ray",
        action="store_true",
        dest="use_xray"
    )
    parser.add_argument(
        "--template",
        type=str,
        help="Path to the proxy (v2ray/xra) template file. By default vmess_ws_tls is used",
        required=False,
        dest="template_path",
        default=os.path.join(SCRIPTDIR, "config_templates",
                             "vmess_ws_tls_template.json")
    )

    parse_args = parser.parse_args()

    if not parse_args.no_vpn and parse_args.config_path is None:
        parser.error("Either use novpn mode or provide a config file")

    return parse_args


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
                f"{_COLORS.FAIL}ERROR {_COLORS.WARNING}Could not read asn {asn} from asnlookup{_COLORS.ENDC}")

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

    return (2**(32-subnet_mask))


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


def mean_jitter(latencies: list):
    if len(latencies) <= 1:
        return -1
    jitters = [abs(a - b) for a, b in zip(latencies[1:], latencies[:-1])]
    return statistics.mean(jitters)


if __name__ == "__main__":
    args = parse_args()

    if not args.no_vpn:
        create_dir(CONFIGDIR)
        configFilePath = args.config_path

    create_dir(RESULTDIR)

    # create empty result file
    with open(INTERIM_RESULTS_PATH, "w") as emptyfile:
        titles = [
            "ip", "avg_download_speed", "avg_upload_speed",
            "avg_download_latency", "avg_upload_latency",
            "avg_download_jitter", "avg_upload_jitter"
        ]
        titles += [f"download_speed_{i+1}" for i in range(args.n_tries)]
        titles += [f"upload_speed_{i+1}" for i in range(args.n_tries)]
        titles += [f"download_latency_{i+1}" for i in range(args.n_tries)]
        titles += [f"upload_latency_{i+1}" for i in range(args.n_tries)]
        emptyfile.write(",".join(titles) + "\n")

    threadsCount = args.threads

    if args.subnets_path:
        subnetFilePath = args.subnets_path
        with open(str(subnetFilePath), 'r') as subnetFile:
            cidr_list = [l.strip() for l in subnetFile.readlines()]
    else:
        cidr_list = read_cidrs_from_asnlookup()

    test_config = create_test_config(args)

    n_total_ips = sum(get_num_ips_in_cidr(cidr) for cidr in cidr_list)
    print(f"Starting to scan {n_total_ips} ips...")

    big_ip_list = [ip for cidr in cidr_list for ip in cidr_to_ip_list(cidr)]

    with multiprocessing.Pool(processes=threadsCount) as pool:
        for res in pool.imap(partial(check_ip, test_config=test_config), big_ip_list):
            if res:
                down_mean_jitter = mean_jitter(res["download"]["latency"])
                up_mean_jitter = mean_jitter(
                    res["upload"]["latency"]) if test_config.do_upload_test else -1
                mean_down_speed = statistics.mean(res["download"]["speed"])
                mean_up_speed = statistics.mean(
                    res["upload"]["speed"]) if test_config.do_upload_test else -1
                mean_down_latency = statistics.mean(res["download"]["latency"])
                mean_up_latency = statistics.mean(
                    res["upload"]["latency"]) if test_config.do_upload_test else -1

                print(
                    f"{_COLORS.OKGREEN}"
                    f"OK {res['ip']:15s} "
                    f"{_COLORS.OKBLUE}"
                    f"avg_down_speed: {mean_down_speed:7.4f}mbps "
                    f"avg_up_speed: {mean_up_speed:7.4f}mbps "
                    f"avg_down_latency: {mean_down_latency:6.2f}ms "
                    f"avg_up_latency: {mean_up_latency:6.2f}ms ",
                    f"avg_down_jitter: {down_mean_jitter:6.2f}ms ",
                    f"avg_up_jitter: {up_mean_jitter:4.2f}ms"
                    f"{_COLORS.ENDC}"
                )

                with open(INTERIM_RESULTS_PATH, "a") as outfile:
                    res_parts = [
                        res["ip"], mean_down_speed, mean_up_speed,
                        mean_down_latency, mean_up_latency,
                        down_mean_jitter, up_mean_jitter
                    ]
                    res_parts += res["download"]["speed"]
                    res_parts += res["upload"]["speed"]
                    res_parts += res["download"]["latency"]
                    res_parts += res["upload"]["latency"]

                    outfile.write(",".join(map(str, res_parts)) + "\n")
