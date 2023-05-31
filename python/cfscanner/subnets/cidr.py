import ipaddress
import linecache
import math
import os
import random
import re
from typing import Generator, Tuple, Union
from urllib.parse import urlparse

import requests
from rich.console import Console

from ..utils.exceptions import *
from ..utils.os import get_n_lines
from ..utils.random import reservoir_sampling

PATH = os.getcwd()
console = Console()


def cidr_to_ip_gen(
    cidr: str,
    sample_size: Union[int, float, None] = None,
    sampling_timeout: float = None
) -> list:
    """converts a subnet to a list of ips

    Args:
        cidr (str): the cidr in the form of "ip/subnet"
        sample_size (Union[int, float, None], optional): The number of ips to sample from the subnet or
        the ratio of ips to sample from the subnet. If None, all ips will be returned Defaults to None.
        sampling_timeout (float, optional): The timeout for the sampling process. Defaults to None. If exceeded, 
        the sampling process will be terminated and the current sampled ips will be returned.

    Returns:
        list: a list of ips associated with the subnet
    """
    n_ips = get_num_ips_in_cidr(cidr)
    ip_generator = map(str, ipaddress.ip_network(cidr, strict=False))
    
    if sample_size is None or sample_size >= n_ips:
        return ip_generator
    elif 1 <= sample_size < n_ips:
        return reservoir_sampling(
            ip_generator, 
            round(sample_size),
            sampling_timeout=sampling_timeout
        )
    elif 0 < sample_size < 1:
        return reservoir_sampling(
            ip_generator, 
            math.ceil(n_ips * sample_size),
            sampling_timeout=sampling_timeout
        )
    else:
        raise ValueError(f"Invalid sample size: {sample_size}")


def get_num_ips_in_cidr(
    cidr: str,
    sample_size: Union[int, float, None] = None
):
    """
    Returns the number of IP addresses in a CIDR block.
    """
    parts = cidr.split('/')

    try:
        subnet_mask = int(parts[1])
    except IndexError as e:
        subnet_mask = 128 if ":" in cidr else 32

    n_ips = 2**(128 - subnet_mask) if ":" in cidr else 2**(32 - subnet_mask)

    if sample_size is None:
        return n_ips
    elif 1 <= sample_size:
        return min(n_ips, round(sample_size))
    elif 0 < sample_size < 1:
        return min(math.ceil(n_ips * sample_size), n_ips)
    else:
        raise ValueError(f"Invalid sample size: {sample_size}")


def read_cidrs_from_url(
    url: str,
    timeout: float = 10
) -> list:
    """reads cidrs from a url

    Args:
        url (str): The url to read the cidrs from

    Returns:
        list: The list of cidrs associated with ``asn_list``
    """
    try:
        r = requests.get(url, timeout=timeout)
        if r.status_code != 200:
            raise SubnetsReadError(
                f"Could not read cidrs from url - status code: {r.status_code}", url)
        cidr_regex = r"(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?:\/[\d]+)?"
        cidrs = re.findall(cidr_regex, r.text)
        if len(cidrs) == 0:
            raise SubnetsReadError(
                f"Could not find any cidr in url {url}"
            )
    except Exception as e:
        raise SubnetsReadError(f"Could not read cidrs from url \"{url}\"")

    return cidrs


def read_cidrs_from_file(
    filepath: str,
    shuffle: bool = False,
    n_lines: int = None
):
    """reads cidrs from a file

    Args:
        filepath (str): The path to the file to read the cidrs from
        shuffle (bool, optional): Whether to shuffle the cidrs. Defaults to False.
        n_lines (int): The number of lines in the file. If not provided, the function will try to get it from the file itself
    """
    try:
        if shuffle:
            if n_lines is None:
                n_lines = get_n_lines(filepath)
            shuf_order = list(range(n_lines))
            random.shuffle(shuf_order)

            for pos in shuf_order:
                line = linecache.getline(filepath, pos)
                if line.strip():
                    yield line.strip()
        else:
            with open(filepath, "r") as f:
                for line in f:
                    yield line.strip()

    except Exception as e:
        raise SubnetsReadError(f"Could not read cidrs from file {filepath}")


def read_cidrs(
    url_or_path: str,
    shuffle: bool = False,
    timeout: float = 10
) -> Tuple[Generator, int]:
    """reads cidrs from a url or file

    Args:
        url_or_path (str): The url or path to the file to read the cidrs from
        shuffle (bool, optional): Whether to shuffle the cidrs. Defaults to False.
        timeout (float, optional): The timeout for the request. Defaults to 10.

    Returns:
        Genrator: A generator of cidrs
    """
    if os.path.isfile(url_or_path):
        n_cidrs = get_n_lines(url_or_path)
        cidrs = read_cidrs_from_file(
            url_or_path,
            shuffle=shuffle, 
            n_lines=n_cidrs
        )
    elif urlparse(url_or_path).scheme:
        cidrs_list = read_cidrs_from_url(url_or_path, timeout)
        n_cidrs = len(cidrs_list)
        subnets_path = os.path.join(PATH, ".tmp")
        os.makedirs(subnets_path, exist_ok=True)
        with open(os.path.join(subnets_path, "cidrs.txt"), "w") as f:
            f.write("\n".join(cidrs_list))
        cidrs = read_cidrs_from_file(
            os.path.join(subnets_path, "cidrs.txt"),
            shuffle=shuffle,
            n_lines=len(cidrs_list)
        )
    else:
        raise SubnetsReadError(
            f"\"{url_or_path}\" is neither a valid url nor a file path."
        )
    return cidrs, n_cidrs
