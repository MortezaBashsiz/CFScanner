#!/usr/bin/env python

import ipaddress
import json
import multiprocessing
import os
import signal
import statistics
import sys
from datetime import datetime
from functools import partial

import requests
from args.parser import parse_args
from args.testconfig import TestConfig
from report.clog import CLogger
from report.print import print_and_kill, print_ok
from speedtest import download_speed_test, fronting_test, upload_speed_test
from speedtest.tools import mean_jitter
from subnets import read_cidrs
from utils.decorators import timeout_fun
from utils.exceptions import BinaryNotFoundError, TemplateReadError
from utils.os import create_dir
from utils.socket import get_free_port
from xray.service import start_proxy_service

log = CLogger("cfscanner-python")

SCRIPTDIR = os.path.dirname(os.path.realpath(__file__))
CONFIGDIR = f"{SCRIPTDIR}/../config"
RESULTDIR = f"{SCRIPTDIR}/../result"
START_DT_STR = datetime.now().strftime(r"%Y%m%d_%H%M%S")
INTERIM_RESULTS_PATH = os.path.join(RESULTDIR, f'{START_DT_STR}_result.csv')


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

    if not test_config.novpn:
        try:
            proxy_config_path = create_proxy_config(ip, test_config)
        except Exception as e:
            log.error("Could not save proxy (xray/v2ray) config to file", ip)
            log.exception(e)
            return print_and_kill(
                ip=ip,
                message="Could not save proxy (xray/v2ray) config to file",
                process=process
            )

    if not test_config.novpn:
        try:
            process, proxies = start_proxy_service(
                proxy_conf_path=proxy_config_path,
                binarypath=test_config.binpath,
                timeout=test_config.startprocess_timeout
            )
        except Exception as e:
            message = "Could not start proxy (v2ray/xray) service"
            log.error(message, ip)
            log.exception(e)
            print_and_kill(ip=ip, message=message, process=process)
    else:
        process = _FakeProcess()
        proxies = None

    @timeout_fun(test_config.max_dl_latency + test_config.max_dl_time)
    def timeout_download_fun():
        return download_speed_test(
            n_bytes=n_bytes,
            proxies=proxies,
            timeout=test_config.max_dl_latency
        )

    for try_idx in range(test_config.n_tries):
        # check download speed
        n_bytes = test_config.min_dl_speed * 1000 * test_config.max_dl_time
        try:
            dl_speed, dl_latency = timeout_download_fun()
        except TimeoutError as e:
            return print_and_kill(ip=ip, message="download timeout exceeded", process=process)
        except (requests.exceptions.ReadTimeout, requests.exceptions.ConnectionError, requests.ConnectTimeout) as e:
            return print_and_kill(ip=ip, message="download error", process=process)
        except Exception as e:
            log.error("Download - unknown error", ip)
            log.exception(e)
            return print_and_kill(ip=ip, message="download unknown error", process=process)

        if dl_latency <= test_config.max_dl_latency:
            dl_speed_kBps = dl_speed / 8 * 1000
            if dl_speed_kBps >= test_config.min_dl_speed:
                result["download"]["speed"][try_idx] = dl_speed
                result["download"]["latency"][try_idx] = round(
                    dl_latency * 1000)
            else:
                message = f"download too slow {dl_speed_kBps:.2f} < {test_config.min_dl_speed:.2f} kBps"
                return print_and_kill(ip=ip, message=message, process=process)
        else:
            message = f"high download latency {dl_latency:.4f} s > {test_config.max_dl_latency:.4f} s"
            return print_and_kill(ip=ip, message=message, process=process)

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
                return print_and_kill(ip, 'upload read timeout', process)
            except requests.exceptions.ConnectTimeout:
                return print_and_kill(ip, 'upload connect timeout', process)
            except requests.exceptions.ConnectionError:
                return print_and_kill(ip, 'upload connection error', process)
            except Exception as e:
                log.error("Upload - unknown error", ip)
                log.exception(e)
                return print_and_kill(ip, 'upload unknown error', process)

            if up_latency > test_config.max_ul_latency:
                return print_and_kill(ip, 'upload latency too high', process)
            up_speed_kBps = up_speed / 8 * 1000
            if up_speed_kBps >= test_config.min_ul_speed:
                result["upload"]["speed"][try_idx] = up_speed
                result["upload"]["latency"][try_idx] = round(up_latency * 1000)
            else:
                message = f"upload too slow {up_speed_kBps:.2f} kBps < {test_config.min_ul_speed:.2f} kBps"
                return print_and_kill(ip, message, process)

    process.kill()
    return result


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
        subnet_mask = 128 if ":" in cidr else 32
    n_ips = 2**(128 - subnet_mask) if ":" in cidr else 2**(32 - subnet_mask)

    return n_ips


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

    if args.subnets:
        cidr_list = read_cidrs(args.subnets)
    else:
        cidr_list = read_cidrs(
            "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/bash/cf.local.iplist"
        )

    try:
        test_config = TestConfig.from_args(args)
    except TemplateReadError:
        log.error("Could not read template from file.")
        exit(1)
    except BinaryNotFoundError:
        log.error("Could not find xray/v2ray binary.", args.binpath)
        exit(1)
    except Exception as e:
        log.error("Unknown error while reading template.")
        log.exception(e)
        exit(1)

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

                print_ok(scan_result=res)

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
