import logging
import socket
import statistics
from ipaddress import ip_address

logger = logging.getLogger(__name__)


def mean_jitter(latencies: list) -> float:
    """calculates the mean jitter of a list of latencies

    Args:
        latencies (list): the list of latencies

    Returns:
        float: the mean jitter
    """
    if len(latencies) <= 1:
        return -1
    jitters = [abs(a - b) for a, b in zip(latencies[1:], latencies[:-1])]
    return statistics.mean(jitters)


def is_blocked(hostname: str) -> bool:
    """Checks if a hostname is blocked

    Args:
        ip (str): The hostname to check.

    Returns:
        bool: True if the hostname is blocked
    """
    logger.debug(f"Checking if {hostname} is blocked")
    try:
        resolved_ip = ip_address(socket.gethostbyname(hostname))
        logger.debug(f"Resolved IP for {hostname} is {resolved_ip}")
        if resolved_ip.is_private:
            logger.warning(
                f"[bold yellow1]Private IP {resolved_ip} for hostname"
                f" {hostname}[/bold yellow1]"
            )
            return True
    except Exception as e:
        logger.info(f"[bold red1]Error resolving hostname {hostname}[/bold red1]")
        logger.exception(e)
        return True
    else:
        return False
