#!/usr/bin/env python

import multiprocessing
import os
import statistics
from datetime import datetime
from functools import partial

from args.parser import parse_args
from args.testconfig import TestConfig
from report.clog import CLogger
from report.print import print_ok
from speedtest.conduct import test_ip
from speedtest.tools import mean_jitter
from subnets import cidr_to_ip_list, get_num_ips_in_cidr, read_cidrs
from utils.exceptions import BinaryNotFoundError, TemplateReadError
from utils.os import create_dir

log = CLogger("cfscanner-python")

SCRIPTDIR = os.path.dirname(os.path.realpath(__file__))
CONFIGDIR = f"{SCRIPTDIR}/../config"
RESULTDIR = f"{SCRIPTDIR}/../result"
START_DT_STR = datetime.now().strftime(r"%Y%m%d_%H%M%S")
INTERIM_RESULTS_PATH = os.path.join(RESULTDIR, f'{START_DT_STR}_result.csv')


if __name__ == "__main__":
    args = parse_args()

    if not args.no_vpn:
        create_dir(CONFIGDIR)
        configFilePath = args.config_path

    create_dir(RESULTDIR)

    # create empty result file
    with open(INTERIM_RESULTS_PATH, "w") as empty_file:
        titles = [
            "ip", "avg_download_speed", "avg_upload_speed",
            "avg_download_latency", "avg_upload_latency",
            "avg_download_jitter", "avg_upload_jitter"
        ]
        titles += [f"download_speed_{i+1}" for i in range(args.n_tries)]
        titles += [f"upload_speed_{i+1}" for i in range(args.n_tries)]
        titles += [f"download_latency_{i+1}" for i in range(args.n_tries)]
        titles += [f"upload_latency_{i+1}" for i in range(args.n_tries)]
        empty_file.write(",".join(titles) + "\n")

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

    n_total_ips = sum(get_num_ips_in_cidr(cidr, sample_size=test_config.sample_size) for cidr in cidr_list)
    log.info(f"Starting to scan {n_total_ips} ips...")

    big_ip_list = [ip for cidr in cidr_list for ip in cidr_to_ip_list(cidr, sample_size=test_config.sample_size)]

    with multiprocessing.Pool(processes=threadsCount) as pool:
        for res in pool.imap(partial(test_ip, test_config=test_config, config_dir=CONFIGDIR), big_ip_list):
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
