import statistics

def override_addrinfo(ip: str, org_addr_info):
    def _custom_addrinfo(*args, **kwargs):
        if args[0] == "speed.cloudflare.com":
            return org_addr_info(ip, *args[1:], **kwargs)
        else:
            return org_addr_info(*args, **kwargs)
    return _custom_addrinfo

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
