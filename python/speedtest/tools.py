import statistics


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
