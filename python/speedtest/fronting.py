import requests

from report import Colors
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

    success = False
    try:
        compatible_ip = f"[{ip}]" if ":" in ip else ip
        r = s.get(
            f"https://{compatible_ip}",
            timeout=timeout,
            headers={"Host": "speed.cloudflare.com"}
        )
        if r.status_code != 200:
            print(
                f"{Colors.FAIL}NO {Colors.WARNING}{ip:15s} fronting error {r.status_code} {Colors.ENDC}")
        else:
            success = True
    except requests.exceptions.ConnectTimeout as e:
        print(
            f"{Colors.FAIL}NO {Colors.WARNING}{ip:15s} fronting connect timeout{Colors.ENDC}"
        )
    except requests.exceptions.ReadTimeout as e:
        print(
            f"{Colors.FAIL}NO {Colors.WARNING}{ip:15s} fronting read timeout{Colors.ENDC}"
        )
    except requests.exceptions.ConnectionError as e:
        print(
            f"{Colors.FAIL}NO {Colors.WARNING}{ip:15s} fronting connection error{Colors.ENDC}"
        )
    except Exception as e:
        f"{Colors.FAIL}NO {Colors.WARNING}{ip:15s}fronting Unknown error{Colors.ENDC}"
        logger.error(f"Fronting test Unknown error {ip:15}")
        logger.exception(e)

    return success
