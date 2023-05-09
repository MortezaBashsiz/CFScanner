import socket
import time
from typing import Tuple

import requests

from .tools import override_addrinfo

ORG_ADDR_INFO = socket.getaddrinfo


def download_speed_test(
    ip: str,
    n_bytes: int,
    proxies: dict,
    timeout: int
) -> Tuple[float, float]:
    """tests the download speed using cloudflare servers

    Args:
        ip (str): The edge server ip address to be tested
        n_bytes (int): size of file to download in bytes
        proxies (dict): the proxies to use for ``requests.get``
        timeout (int): the timeout for the download request        

    Returns:
        download_speed (float): the download speed in megabit per second
        latency (float): the round trip time latency in seconds
    """
    socket.getaddrinfo = override_addrinfo(ip, ORG_ADDR_INFO)
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
