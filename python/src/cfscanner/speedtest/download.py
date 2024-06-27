import logging
import re
import time
from typing import Tuple

import requests

log = logging.getLogger(__name__)


def download_speed_test(
    n_bytes: int, proxies: dict, timeout: int
) -> Tuple[float, float]:
    """tests the download speed using cloudflare servers

    Args:
        n_bytes (int): size of file to download in bytes
        proxies (dict): the proxies to use for ``requests.get``
        timeout (int): the timeout for the download request

    Returns:
        download_speed (float): the download speed in megabit per second
        latency (float): the round trip time latency in seconds
    """
    start_time = time.perf_counter()
    r = requests.get(
        url="https://speed.cloudflare.com/__down",
        params={"bytes": n_bytes},
        timeout=timeout,
        proxies=proxies,
    )
    total_time = time.perf_counter() - start_time

    server_timing_header = r.headers.get("Server-Timing")
    pattern = r"dur=(\d*\.\d+)"
    match = re.search(pattern, server_timing_header)
    if match:
        cf_time = float(match.group(1)) / 1000
    else:
        msg = f"Cannot parse CF header: {server_timing_header}"
        log.error(
            msg,
            extra={"header": server_timing_header},
        )
        raise ValueError(msg)

    latency = r.elapsed.total_seconds() - cf_time
    download_time = total_time - latency

    mb = n_bytes * 8 / (10**6)
    download_speed = mb / download_time

    return download_speed, latency
