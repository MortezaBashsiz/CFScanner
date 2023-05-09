import socket
import time
from typing import Tuple

import requests

from .tools import override_addrinfo

ORG_ADDR_INFO = socket.getaddrinfo


def upload_speed_test(
    ip: str,
    n_bytes: int,
    proxies: dict,
    timeout: int,
) -> Tuple[float, float]:
    """tests the upload speed using cloudflare servers

    Args:
        ip (str): The edge server ip address to be tested
        n_bytes (int): size of file to upload in bytes
        proxies (dict): the proxies to use for ``requests.post``
        timeout (int): the timeout for the download ``requests.post``   

    Returns:
        upload_speed (float): the upload speed in mbps
        latency (float): the rount trip time latency in seconds
    """
    socket.getaddrinfo = override_addrinfo(ip, ORG_ADDR_INFO)
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
