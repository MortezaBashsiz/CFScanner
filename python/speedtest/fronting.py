import requests

from report.clog import CLogger

logger = CLogger("fronting")


def fronting_test(
    ip: str,
    timeout: float
) -> bool:
    """conducts a fronting test on an ip and return true if status 200 is received

    Args:
        ip (str): ip for testing
        timeout (float): the timeout to wait for ``requests.get`` result

    Returns:
        bool: True if ``status_code`` is 200, False otherwise
    """
    s = requests.Session()
    s.get_adapter(
        'https://').poolmanager.connection_pool_kw['server_hostname'] = "speed.cloudflare.com"
    s.get_adapter(
        'https://').poolmanager.connection_pool_kw['assert_hostname'] = "speed.cloudflare.com"

    try:
        compatible_ip = f"[{ip}]" if ":" in ip else ip
        r = s.get(
            f"https://{compatible_ip}",
            timeout=timeout,
            headers={"Host": "speed.cloudflare.com"}
        )
        if r.status_code != 200:
            return f"[red]NO[/red] [dark_orange3]{ip:15s}[/dark_orange3][yellow] fronting error {r.status_code} [/yellow]"
        else:
            success = True
    except requests.exceptions.ConnectTimeout as e:
        return f"[red]NO[/red] [dark_orange3]{ip:15s}[/dark_orange3][yellow] fronting connect timeout[/yellow]"
    except requests.exceptions.ReadTimeout as e:
        return f"[red]NO[/red] [dark_orange3]{ip:15s}[/dark_orange3][yellow] fronting read timeout[/yellow]"
    except requests.exceptions.ConnectionError as e:
        return f"[red]NO[/red] [dark_orange3]{ip:15s}[/dark_orange3][yellow] fronting connection error[/yellow]"
    except Exception as e:
        logger.error(f"Fronting test Unknown error {ip:15}")
        logger.exception(e)
        return f"[red]NO[/red] [dark_orange3]{ip:15s}[/dark_orange3][yellow] fronting Unknown error[/yellow]"
    

    return "OK"
