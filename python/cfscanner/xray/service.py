import json
import subprocess
from typing import Tuple

from ..utils.socket import wait_for_port


def start_proxy_service(
    proxy_conf_path: str,
    binary_path: str,
    timeout=5,
) -> Tuple[subprocess.Popen, dict]:
    """starts the proxy (v2ray/xray) service and waits for the respective port to open

    Args:
        proxy_conf_path (str): the path to the proxy (v2ray or xray) config json file
        binary_path (str): the path to the xray binary file. Defaults to None.
        timeout (int, optional): total time in seconds to wait for the proxy service to start. Defaults to 5.

    Returns:
        Tuple[subprocess.Popen, dict]: the v2ray process object and a dictionary containing the proxies to use with ``requests.get`` 
    """
    with open(proxy_conf_path, "r") as infile:
        proxy_conf = json.load(infile)

    proxy_listen = proxy_conf["inbounds"][0]["listen"]
    proxy_port = proxy_conf["inbounds"][0]["port"]
    proxy_process = subprocess.Popen(
        [binary_path, "-c", proxy_conf_path],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )

    wait_for_port(host=proxy_listen, port=proxy_port, timeout=timeout)

    proxies = dict(
        http=f"socks5://{proxy_listen}:{proxy_port}",
        https=f"socks5://{proxy_listen}:{proxy_port}"
    )

    return proxy_process, proxies
