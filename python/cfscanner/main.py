#!/usr/bin/env python

import logging
import multiprocessing
import os
import signal
import statistics
from datetime import datetime
from functools import partial

import pkg_resources
from rich.console import Console

from .args.parser import parse_args
from .args.testconfig import TestConfig
from .report.print import TitledProgress
from .speedtest.conduct import test_ip
from .speedtest.tools import mean_jitter
from .subnets import cidr_to_ip_gen, get_num_ips_in_cidr, read_cidrs
from .utils.exceptions import *
from .utils.os import create_dir

console = Console()

SCRIPTDIR = os.getcwd()
CONFIGDIR = f"{SCRIPTDIR}/.xray-configs"
RESULTDIR = f"{SCRIPTDIR}/result"
START_DT_STR = datetime.now().strftime(r"%Y%m%d_%H%M%S")
INTERIM_RESULTS_PATH = os.path.join(RESULTDIR, f"{START_DT_STR}_result.csv")


def _prescan_sigint_handler(sig, frame):
    console.log(
        f"[yellow]KeyboardInterrupt detected (pre-scan phase) - {START_DT_STR}[/yellow]"
    )
    exit(1)


def _init_pool():
    signal.signal(signal.SIGINT, signal.SIG_IGN)


def main():
    logger = logging.getLogger(__name__)
    console = Console()

    logo = """                                                                                                                                        
____ ____ ____ ____ ____ _  _ _  _ ____ ____ 
|    |___ [__  |    |__| |\ | |\ | |___ |__/ 
|___ |    ___] |___ |  | | \| | \| |___ |  \ 
"""
    console.print(f"[bold green1]{logo}[/bold green1]")

    try:
        console.print(
            f"[bold green1]v{pkg_resources.get_distribution('cfscanner').version}[bold green1]\n\n"
        )
    except pkg_resources.DistributionNotFound:
        console.print(f"[bold green1]v0.0.0[bold green1]\n\n")

    log_dir = os.path.join(SCRIPTDIR, "log")
    os.makedirs(log_dir, exist_ok=True)
    logging.basicConfig(filename=os.path.join(log_dir, f"{START_DT_STR}.log"))

    console.log(f"[green]Scan started - {START_DT_STR}[/green]")

    original_sigint_handler = signal.signal(signal.SIGINT, _prescan_sigint_handler)

    args = parse_args()

    if not args.no_vpn:
        with console.status(f'[green]Creating config dir "{CONFIGDIR}"[/green]'):
            try:
                create_dir(CONFIGDIR)
            except Exception as e:
                console.log("[red1]Could not create config directory[/red1]")
                logger.exception("Could not create config directory")
                exit(1)
        console.log(
            f'[bright_blue]Config directory created "{CONFIGDIR}"[/bright_blue]'
        )

    with console.status(f'[green]Creating results directory "{RESULTDIR}"[/green]'):
        try:
            create_dir(RESULTDIR)
        except Exception as e:
            console.log("[red1]Could not create results directory[/red1]")
            logger.exception("Could not create results directory")
            exit(1)
    console.log(f'[bright_blue]Results directory created "{RESULTDIR}"[/bright_blue]')

    # create empty result file
    with console.status(
        f"[green]Creating empty result file {INTERIM_RESULTS_PATH}[/green]"
    ):
        try:
            with open(INTERIM_RESULTS_PATH, "w") as empty_file:
                titles = [
                    "ip",
                    "avg_download_speed",
                    "avg_upload_speed",
                    "avg_download_latency",
                    "avg_upload_latency",
                    "avg_download_jitter",
                    "avg_upload_jitter",
                ]
                titles += [f"download_speed_{i+1}" for i in range(args.n_tries)]
                titles += [f"upload_speed_{i+1}" for i in range(args.n_tries)]
                titles += [f"download_latency_{i+1}" for i in range(args.n_tries)]
                titles += [f"upload_latency_{i+1}" for i in range(args.n_tries)]
                empty_file.write(",".join(titles) + "\n")
        except Exception as e:
            console.log(
                f'[red1]Could not create empty result file:\n"{INTERIM_RESULTS_PATH}"[/red1]'
            )
            logger.exception("Could not create empty result file")
            exit(1)

    try:
        test_config = TestConfig.from_args(args)
    except TemplateReadError as e:
        console.log(
            f'[red1]Could not read template from file "{args.template_path}"[/red1]'
        )
        logger.exception(e)
        exit(1)
    except BinaryNotFoundError:
        console.log(
            f'[red1]Could not find xray/v2ray binary from path "{args.binpath}"[/red1]'
        )
        logger.exception(e)
        exit(1)
    except Exception as e:
        console.print_exception()
        logger.exception(e)
        exit(1)

    threadsCount = args.threads

    if args.subnets:
        with console.status('[green]Reading subnets from "{args.subnets}"[/green]'):
            try:
                cidr_generator, n_cidrs = read_cidrs(
                    args.subnets,
                    shuffle=args.shuffle_subnets,
                )
            except SubnetsReadError as e:
                console.log("[red1]Could not read subnets.[/red1]")
                logger.exception("Could not read subnets")
                exit(1)
            except Exception as e:
                console.log(f"Unknown error in reading subnets: {e}")
                logger.exception(f"Unknown error in reading subnets: {e}")
                exit(1)
        console.log(
            f'[bright_blue]Subnets successfully read from "{args.subnets}"[/bright_blue]'
        )
    else:
        subnets_default_address = "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/config/cf.local.iplist"
        console.log(
            f'[bright_blue]Subnets not provided. Default address will be used:\n"{subnets_default_address}"[/bright_blue]'
        )
        with console.status(
            f'[green]Retrieving subnets from "{subnets_default_address}"[/green]'
        ):
            try:
                cidr_generator, n_cidrs = read_cidrs(
                    "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/config/cf.local.iplist",
                    shuffle=args.shuffle_subnets,
                )
            except SubnetsReadError as e:
                console.log(f"[red1]Could not read subnets. {e}[/red1]")
                logger.exception(e)
                exit(1)
            except Exception as e:
                console.log(f"[red1]Unknown error in reading subnets: {e}[/red1]")
                logger.exception(e)
                exit(1)

    def ip_generator():
        for cidr in cidr_generator:
            for ip in cidr_to_ip_gen(
                cidr,
                sample_size=test_config.sample_size,
                sampling_timeout=test_config.sampling_timeout,
            ):
                yield ip, cidr

    cidr_scanned_ips = dict()
    cidr_prog_tasks = dict()

    with TitledProgress(title=f"start: [green]{START_DT_STR}[/green]") as progress:
        console = progress.console
        all_subnets_task = progress.add_task(
            f"all subnets - {n_cidrs} subnets", total=n_cidrs
        )
        with multiprocessing.Pool(
            processes=threadsCount, initializer=_init_pool
        ) as pool:
            signal.signal(signal.SIGINT, original_sigint_handler)
            iterator = pool.imap(
                partial(test_ip, test_config=test_config, config_dir=CONFIGDIR),
                ip_generator(),
            )
            while True:
                try:
                    res = next(iterator)
                    if res.cidr not in cidr_scanned_ips:
                        cidr_scanned_ips[res.cidr] = 0
                        n_ips_cidr = get_num_ips_in_cidr(
                            res.cidr, sample_size=test_config.sample_size
                        )
                        cidr_prog_tasks[res.cidr] = progress.add_task(
                            f"{res.cidr:17s} - {n_ips_cidr} ips", total=n_ips_cidr
                        )
                    progress.update(cidr_prog_tasks[res.cidr], advance=1)
                    progress.scanned_ips += 1

                    if res.is_ok:
                        progress.ok_ips += 1
                        down_mean_jitter = mean_jitter(
                            res.result["download"]["latency"]
                        )
                        up_mean_jitter = (
                            mean_jitter(res.result["upload"]["latency"])
                            if test_config.do_upload_test
                            else -1
                        )
                        mean_down_speed = statistics.mean(
                            res.result["download"]["speed"]
                        )
                        mean_up_speed = (
                            statistics.mean(res.result["upload"]["speed"])
                            if test_config.do_upload_test
                            else -1
                        )
                        mean_down_latency = statistics.mean(
                            res.result["download"]["latency"]
                        )
                        mean_up_latency = (
                            statistics.mean(res.result["upload"]["latency"])
                            if test_config.do_upload_test
                            else -1
                        )

                        console.print(res.message)

                        with open(INTERIM_RESULTS_PATH, "a") as outfile:
                            res_parts = [
                                res.ip,
                                mean_down_speed,
                                mean_up_speed,
                                mean_down_latency,
                                mean_up_latency,
                                down_mean_jitter,
                                up_mean_jitter,
                            ]
                            res_parts += res.result["download"]["speed"]
                            res_parts += res.result["upload"]["speed"]
                            res_parts += res.result["download"]["latency"]
                            res_parts += res.result["upload"]["latency"]

                            outfile.write(",".join(map(str, res_parts)) + "\n")
                    else:
                        console.print(res.message)

                    cidr_scanned_ips[res.cidr] += 1
                    if cidr_scanned_ips[res.cidr] == get_num_ips_in_cidr(
                        res.cidr, sample_size=test_config.sample_size
                    ):
                        progress.update(all_subnets_task, advance=1)
                        progress.remove_task(cidr_prog_tasks[res.cidr])
                        cidr_scanned_ips.pop(res.cidr)
                except StartProxyServiceError as e:
                    progress.stop()
                    console.log(f"[red1]{e}[/red1]")
                    pool.terminate()
                    logger.exception("Error in starting xray service.")
                    break
                except StopIteration as e:
                    for task in progress.tasks:
                        progress.stop_task(task.id)
                        progress.remove_task(task.id)
                    progress.stop()
                    progress.log(
                        f"Finished scanning ips. Start: [green]{START_DT_STR}[/green]"
                    )
                    break
                except KeyboardInterrupt as e:
                    for task_id in progress.task_ids:
                        progress.stop_task(task_id)
                        progress.remove_task(task_id)
                    progress.stop()
                    progress.log(
                        f"[yellow]KeyboardInterrupt detected (scan phase) - start: {START_DT_STR}[/yellow]"
                    )
                    pool.terminate()
                    break
                except Exception as e:
                    progress.log("[red1]Unknown error![/red1]")
                    console.print_exception()
                    logger.exception(e)
