import re

import requests
import logging

logger = logging.getLogger(__name__)


def fronting_test(ip: str, timeout: float, fronting_domain=None) -> bool:
    if not fronting_domain:
        logger.debug(f"Testing {ip} with direct fronting")
        return fronting_test_direct(ip, timeout)
    else:
        logger.debug(f"Testing {ip} with cname fronting")
        return fronting_test_cname(ip, timeout, fronting_domain)


def fronting_test_cname(ip: str, timeout: float, fronting_domain=None) -> bool:
    """conducts a fronting test on an ip and return true if ok

    Args:
        ip (str): ip for testing
        timeout (float): the timeout to wait for ``requests.get`` result

    Returns:
        bool: True if ``status_code`` is 200, False otherwise
    """
    s = requests.Session()

    s.get_adapter("https://").poolmanager.connection_pool_kw["server_hostname"] = (
        fronting_domain
    )
    s.get_adapter("https://").poolmanager.connection_pool_kw["assert_hostname"] = (
        fronting_domain
    )

    try:
        compatible_ip = f"[{ip}]" if ":" in ip else ip
        r = s.get(
            f"https://{compatible_ip}/__down?bytes=10",
            timeout=timeout,
            headers={"Host": fronting_domain},
        )
    except requests.exceptions.Timeout:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting timeout[/yellow1]"
    except requests.exceptions.ConnectionError:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting connection error[/yellow1]"

    try:
        regex = r"^<title>(.+)<\/title>$"
        re_match = re.findall(regex, r.text, re.MULTILINE)[0]
        if "CNAME Cross-User Banned" in re_match:
            return "OK"
        else:
            return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting Unknown error[/yellow1]"
    except Exception:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting Unknown error[/yellow1]"


def fronting_test_direct(ip: str, timeout: float) -> bool:
    """conducts a fronting test on an ip and return true if status 200 is received

    Args:
        ip (str): ip for testing
        timeout (float): the timeout to wait for ``requests.get`` result

    Returns:
        bool: True if ``status_code`` is 200, False otherwise
    """
    s = requests.Session()
    s.get_adapter("https://").poolmanager.connection_pool_kw["server_hostname"] = (
        "speed.cloudflare.com"
    )
    s.get_adapter("https://").poolmanager.connection_pool_kw["assert_hostname"] = (
        "speed.cloudflare.com"
    )

    try:
        compatible_ip = f"[{ip}]" if ":" in ip else ip
        r = s.get(
            f"https://{compatible_ip}/__down?bytes=10",
            timeout=timeout,
            headers={"Host": "speed.cloudflare.com"},
        )
        if r.status_code != 200:
            return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting error {r.status_code} [/yellow1]"
        elif r.content != b"0" * 10:
            return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting error - unexpected response [/yellow1]"
    except requests.exceptions.ConnectTimeout:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting connect timeout[/yellow1]"
    except requests.exceptions.ReadTimeout:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting read timeout[/yellow1]"
    except requests.exceptions.ConnectionError:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting connection error[/yellow1]"
    except Exception:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting Unknown error[/yellow1]"

    return "OK"
