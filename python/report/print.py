from . import Colors
from subprocess import Popen
from speedtest.tools import mean_jitter
from statistics import mean


def print_and_kill(
    ip: str,
    message: str,
    process: Popen
):
    """prints an error message together with the respective ip that causes the error message in a nice colored format

    Args:
        ip (str): the ip that causes the error
        message (str): the message related to the error
        process (Popen): the process (xray) to be killed
    """
    print(f"{Colors.FAIL}NO {Colors.WARNING}{ip:15s} {message}{Colors.ENDC}")
    process.kill()


def print_ok(
    scan_result: dict
) -> None:
    """prints the result if test is ok

    Args:
        scan_result (dict): the results of the scan
    """
    down_mean_jitter = mean_jitter(scan_result["download"]["latency"])
    up_mean_jitter = mean_jitter(scan_result["upload"]["latency"])
    mean_down_speed = mean(scan_result["download"]["speed"])
    mean_up_speed = mean(scan_result["upload"]["speed"])
    mean_down_latency = mean(scan_result["download"]["latency"])
    mean_up_latency = mean(scan_result["upload"]["latency"])
    print(
        f"{Colors.OKGREEN}"
        f"OK {scan_result['ip']:15s} "
        f"{Colors.OKBLUE}"
        f"avg_down_speed: {mean_down_speed:7.4f}mbps "
        f"avg_up_speed: {mean_up_speed:7.4f}mbps "
        f"avg_down_latency: {mean_down_latency:7.2f}ms "
        f"avg_up_latency: {mean_up_latency:7.2f}ms ",
        f"avg_down_jitter: {down_mean_jitter:7.2f}ms ",
        f"avg_up_jitter: {up_mean_jitter:4.2f}ms"
        f"{Colors.ENDC}"
    )
