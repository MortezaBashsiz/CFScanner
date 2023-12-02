import re

import requests


def fronting_test(ip: str, timeout: float) -> bool:
    """conducts a fronting test on an ip and return true if status 200 is received

    Args:
        ip (str): ip for testing
        timeout (float): the timeout to wait for ``requests.get`` result

    Returns:
        bool: True if ``status_code`` is 200, False otherwise
    """
    s = requests.Session()
    s.get_adapter("https://").poolmanager.connection_pool_kw[
        "server_hostname"
    ] = "speed.jafar.beauty"
    s.get_adapter("https://").poolmanager.connection_pool_kw[
        "assert_hostname"
    ] = "speed.jafar.beauty"

    try:
        compatible_ip = f"[{ip}]" if ":" in ip else ip
        r = s.get(
            f"https://{compatible_ip}/__down?bytes=10",
            timeout=timeout,
            headers={"Host": "speed.jafar.beauty"},
        )
    except requests.exceptions.Timeout as e:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting timeout[/yellow1]"
    except requests.exceptions.ConnectionError as e:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting connection error[/yellow1]"

    try:
        regex = r"^<title>(.+)<\/title>$"
        re_match = re.findall(regex, r.text, re.MULTILINE)[0]
        if "CNAME Cross-User Banned" in re_match:
            return "OK"
        else:
            return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting Unknown error[/yellow1]"
    except Exception as e:
        return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3][yellow1] fronting Unknown error[/yellow1]"
