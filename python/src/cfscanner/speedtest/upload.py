import logging
import re
import time
from typing import Tuple

import requests

log = logging.getLogger(__name__)


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
        latency (float): the round trip time latency in seconds
    """

    start_time = time.perf_counter()
    r = requests.post(
        url="https://speed.cloudflare.com/__up",
        data="0" * n_bytes,
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

    latency = total_time - cf_time

    mb = n_bytes * 8 / (10**6)
    upload_speed = mb / cf_time

    return upload_speed, latency
