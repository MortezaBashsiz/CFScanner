import math
import random
import time
from typing import Generator


def reservoir_sampling(
    stream: Generator,
    sample_size: int,
    sampling_timeout: float = None
) -> list:
    """Reservoir sampling algorithm.
    This algorithm is used to sample from a stream of data without knowing the size of the stream.
    The algorithm is described in the following paper[1] and on wikipedia[2] (Algorithm L)
    [1] Vitter, Jeffrey S. "Random sampling with a reservoir." ACM Transactions on Mathematical Software (TOMS) 11.1 (1985): 37-57.
    [2] https://en.wikipedia.org/wiki/Reservoir_sampling


    Args:
        stream (generator): the stream of data
        sample_size (int): the size of the sample to be drawn
        sampling_timeout (float, optional): The timeout for the sampling process. Defaults to None. If exceeded, 
        the sampling process will be terminated and the current sampled ips will be returned.

    Returns:
        list: the sample of size `sample_size` drawn from the stream
    """
    start_time = time.time()
    sample = [next(stream) for _ in range(sample_size)]
    i = sample_size - 1
    w = math.exp(math.log(random.random()) / sample_size)
    while True:
        try:
            jump = math.floor(math.log(random.random())/math.log(1 - w))
            for _ in range(jump):
                next(stream)  # discard elements
                i += 1
            next_elm = next(stream)
            i += 1
            sample[random.randint(0, sample_size-1)] = next_elm
            w = w * math.exp(math.log(random.random())/sample_size)
            if sampling_timeout is not None and time.time() - start_time > sampling_timeout:
                break
        except StopIteration:
            break
    return sample
