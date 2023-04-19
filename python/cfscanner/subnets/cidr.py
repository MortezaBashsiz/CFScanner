import ipaddress
import math
import os
import random
import re
from typing import Union
from urllib.parse import urlparse

import requests
from rich.console import Console
from ..utils.exceptions import *

console = Console()


def cidr_to_ip_list(
    cidr: str,
    sample_size: Union[int, float, None] = None
) -> list:
    """converts a subnet to a list of ips

    Args:
        cidr (str): the cidr in the form of "ip/subnet"
        sample_size (Union[int, float, None], optional): The number of ips to sample from the subnet or
        the ratio of ips to sample from the subnet. If None, all ips will be returned Defaults to None.

    Returns:
        list: a list of ips associated with the subnet
    """
    ip_list = list(map(str, ipaddress.ip_network(cidr, strict=False)))
    if sample_size is None or sample_size >= len(ip_list):
        return ip_list
    elif 1 <= sample_size < len(ip_list):
        return random.sample(ip_list, round(sample_size))
    elif 0 < sample_size < 1:
        return random.sample(ip_list, math.ceil(len(ip_list) * sample_size))
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


def read_cidrs_from_asnlookup(
    asn_list: list = ["AS13335", "AS209242"]
) -> list:
    """reads cidrs from asn lookup 

    Args:
        asn_list (list, optional): a list of ASN codes to read from asn lookup. Defaults to ["AS13335", "AS209242"].

    Returns:
        list: The list of cidrs associated with ``asn_list``
    """
    cidrs = []
    for asn in asn_list:
        url = f"https://asnlookup.com/asn/{asn}/"
        this_cidrs = read_cidrs_from_url(url)
        cidrs.extend(this_cidrs)

    return cidrs


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
        cidr_regex = r"(?:[0-9]{1,3}\.){3}[0-9]{1,3}\/[\d]+"
        cidrs = re.findall(cidr_regex, r.text)
        if len(cidrs) == 0:
            raise SubnetsReadError(
                f"Could not find any cidr in url {url}"
            )
    except Exception as e:
        raise SubnetsReadError(f"Could not read cidrs from url \"{url}\"")

    return cidrs


def read_cidrs_from_file(
    filepath: str
) -> list:
    """reads cidrs from a file

    Args:
        filepath (str): The path to the file to read the cidrs from

    Returns:
        list: The list of cidrs found in the file
    """
    try:
        with open(filepath, "r") as f:
            cidrs = f.read().splitlines()
        if len(cidrs) == 0:
            raise SubnetsReadError(
                f"Could not find any cidr in file {filepath}")
    except Exception as e:
        raise SubnetsReadError(f"Could not read cidrs from file {filepath}")
    return cidrs


def read_cidrs(
    url_or_path: str,
    timeout: float = 10
):
    """reads cidrs from a url or file

    Args:
        url_or_path (str): The url or path to the file to read the cidrs from

    Returns:
        list: The list of cidrs found in the file
    """
    if urlparse(url_or_path).scheme:
        cidrs = read_cidrs_from_url(url_or_path, timeout)
    elif os.path.isfile(url_or_path):
        cidrs = read_cidrs_from_file(url_or_path)
    else:
        raise SubnetsReadError(
            f"\"{url_or_path}\" is neither a valid url nor a file path."
        )
    return cidrs
