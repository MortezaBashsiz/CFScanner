#!/usr/bin/env python

import itertools
import multiprocessing
import os
import statistics
from datetime import datetime
from functools import partial

from args.parser import parse_args
from args.testconfig import TestConfig
from report.clog import CLogger
from report.print import ok_message
from rich import print as rprint
from rich.progress import Progress
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

    n_total_ips = sum(get_num_ips_in_cidr(
        cidr,
        sample_size=test_config.sample_size
    ) for cidr in cidr_list)
    log.info(f"Starting to scan {n_total_ips} ips...")

    cidr_ip_lists = [
        cidr_to_ip_list(
            cidr,
            sample_size=test_config.sample_size)
        for cidr in cidr_list
    ]
    big_ip_list = [(ip, cidr) for cidr, ip_list in zip(
        cidr_list, cidr_ip_lists) for ip in ip_list]
    
    cidr_scanned_ips = {cidr: 0 for cidr in cidr_list}
    
    cidr_prog_tasks = dict()

    with Progress() as progress:
        all_ips_task = progress.add_task(
            f"all subnets - {n_total_ips} ips", total=n_total_ips)

        with multiprocessing.Pool(processes=threadsCount) as pool:
            for res in pool.imap(partial(test_ip, test_config=test_config, config_dir=CONFIGDIR), big_ip_list):
                progress.update(all_ips_task, advance=1)
                if cidr_scanned_ips[res.cidr] == 0:
                    n_ips_cidr = get_num_ips_in_cidr(res.cidr, sample_size=test_config.sample_size)
                    cidr_prog_tasks[res.cidr] = progress.add_task(f"{res.cidr} - {n_ips_cidr} ips", total=n_ips_cidr)
                progress.update(cidr_prog_tasks[res.cidr], advance=1)
                
                if res.is_ok:
                    down_mean_jitter = mean_jitter(
                        res.result["download"]["latency"])
                    up_mean_jitter = mean_jitter(
                        res.result["upload"]["latency"]) if test_config.do_upload_test else -1
                    mean_down_speed = statistics.mean(
                        res.result["download"]["speed"])
                    mean_up_speed = statistics.mean(
                        res.result["upload"]["speed"]) if test_config.do_upload_test else -1
                    mean_down_latency = statistics.mean(
                        res.result["download"]["latency"])
                    mean_up_latency = statistics.mean(
                        res.result["upload"]["latency"]) if test_config.do_upload_test else -1

                    rprint(res.message)

                    with open(INTERIM_RESULTS_PATH, "a") as outfile:
                        res_parts = [
                            res.ip, mean_down_speed, mean_up_speed,
                            mean_down_latency, mean_up_latency,
                            down_mean_jitter, up_mean_jitter
                        ]
                        res_parts += res.result["download"]["speed"]
                        res_parts += res.result["upload"]["speed"]
                        res_parts += res.result["download"]["latency"]
                        res_parts += res.result["upload"]["latency"]

                        outfile.write(",".join(map(str, res_parts)) + "\n")
                else:
                    rprint(res.message)
                    
                cidr_scanned_ips[res.cidr] += 1
                if cidr_scanned_ips[res.cidr] == get_num_ips_in_cidr(res.cidr, sample_size=test_config.sample_size):
                    progress.remove_task(cidr_prog_tasks[res.cidr])
                
