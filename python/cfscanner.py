#!/usr/bin/env python

import multiprocessing
import os
import statistics
from datetime import datetime
from functools import partial

from args.parser import parse_args
from args.testconfig import TestConfig
from rich import print as rprint
from rich.console import Console
from rich.progress import Progress
from speedtest.conduct import test_ip
from speedtest.tools import mean_jitter
from subnets import cidr_to_ip_list, get_num_ips_in_cidr, read_cidrs
from utils.exceptions import *
from utils.os import create_dir

console = Console()

SCRIPTDIR = os.path.dirname(os.path.realpath(__file__))
CONFIGDIR = f"{SCRIPTDIR}/../config"
RESULTDIR = f"{SCRIPTDIR}/../result"
START_DT_STR = datetime.now().strftime(r"%Y%m%d_%H%M%S")
INTERIM_RESULTS_PATH = os.path.join(RESULTDIR, f'{START_DT_STR}_result.csv')


if __name__ == "__main__":
    console = Console()

    args = parse_args()

    if not args.no_vpn:
        with console.status(f"[green]Creating config dir \"{CONFIGDIR}\"[/green]"):
            try:
                create_dir(CONFIGDIR)
            except Exception as e:
                console.log("[red]Could not created config directory[/red]")
                exit(1)
        console.log(f"[blue]Config directory created \"{CONFIGDIR}\"[/blue]")
        configFilePath = args.config_path

    with console.status(f"[green]Creating results directory \"{RESULTDIR}\"[/green]"):
        try:
            create_dir(RESULTDIR)
        except Exception as e:
            console.log("[red]Could not create results directory[/red]")
    console.log(f"[blue]Results directory created \"{RESULTDIR}\"[/blue]")

    # create empty result file
    with console.status(f"[green]Creating empty result file {INTERIM_RESULTS_PATH}[/green]"):
        try:
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
        except Exception as e:
            console.log(f"[red]Could not create empty result file:\n\"{INTERIM_RESULTS_PATH}\"[/red]")

    threadsCount = args.threads

    if args.subnets:
        with console.status("[green]Reading subnets from \"{args.subnets}\"[/green]"):
            try:
                cidr_list = read_cidrs(args.subnets)
            except SubnetsReadError as e:
                console.log(f"[red]Could not read subnets. {e}[/red]")
                exit(1)
            except Exception as e:
                console.log(f"Unknown error in reading subnets: {e}")
                exit(1)
        console.log(
            f"[blue]Subnets successfully read from \"{args.subnets}\"[/blue]")
    else:
        subnets_default_address = "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/config/cf.local.iplist"
        console.log(
            f"[blue]Subnets not provided. Default address will be used:\n\"{subnets_default_address}\"[/blue]"
        )
        with console.status(f"[green]Retrieving subnets from \"{subnets_default_address}\"[/green]"):
            try:
                cidr_list = read_cidrs(
                    "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/config/cf.local.iplist"
                )
            except SubnetsReadError as e:
                console.log(f"[red]Could not read subnets. {e}[/red]")
                exit(1)
            except Exception as e:
                console.log(f"Unknown error in reading subnets: {e}")
                exit(1) 
    try:
        test_config = TestConfig.from_args(args)
    except TemplateReadError:
        console.log(
            f"[red]Could not read template from file \"{args.template_path}\"[/red]")
        exit(1)
    except BinaryNotFoundError:
        console.log(
            f"[red]Could not find xray/v2ray binary from path \"{args.binpath}\"[/red]")
        exit(1)
    except Exception as e:
        console.print_exception()
        exit(1)

    n_total_ips = sum(get_num_ips_in_cidr(
        cidr,
        sample_size=test_config.sample_size
    ) for cidr in cidr_list)
    console.log(f"[blue]Starting to scan {n_total_ips} ips...[/blue]")

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
            try:
                for res in pool.imap(partial(test_ip, test_config=test_config, config_dir=CONFIGDIR), big_ip_list):
                    progress.update(all_ips_task, advance=1)
                    if cidr_scanned_ips[res.cidr] == 0:
                        n_ips_cidr = get_num_ips_in_cidr(
                            res.cidr, sample_size=test_config.sample_size)
                        cidr_prog_tasks[res.cidr] = progress.add_task(
                            f"{res.cidr} - {n_ips_cidr} ips", total=n_ips_cidr)
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
            except StartProxyServiceError as e:
                progress.stop()
                console.log(f"[red]{e}[/red]")
                pool.terminate()
