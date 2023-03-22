import os
import zipfile

import requests

from cfscanner import BINDIR, SCRIPTDIR
from clog.clog import CLogger
from utils.decorators import timeout_fun
from utils.exceptions import *
from utils.requests import download_file

from . import LATEST_SUPPORTED_VERSION, SUPPORTED

logger = CLogger("xraybinary")


def download_binary(
    system_info: tuple,
    version: str = None,
    bin_dir: str = None,
    timeout: float = 10,
    max_latency: float = 1
) -> None:
    """Download a binary from a url to a path.

    Args:
        system_info (tuple): (system, arch, abi)
        version (str): the version of xray to download
        binary_savepath (str, optional): path to the binary to be downloaded to
        timeout (float, optional): total allowed time (including RTT) for the download in seconds. Defaults to 10.
        max_latency (float, optional): max allowed RTT for the download in seconds. Defaults to 1.
    """
    if system_info not in SUPPORTED:
        raise OSError(f"System {system_info} not supported")

    if bin_dir is None:
        bin_dir = BINDIR

    if version is None:
        version = LATEST_SUPPORTED_VERSION
    platform_str = "-".join(system_info)
    zip_url = f"https://github.com/XTLS/Xray-core/releases/download/v{version}/Xray-{platform_str}.zip"
    zipdir = os.path.join(SCRIPTDIR, ".tmp")
    os.makedirs(zipdir, exist_ok=True)
    zip_path = os.path.join(zipdir, f"{platform_str}.zip")
    try:
        timeout_fun(timeout=timeout)(download_file)(
            zip_url, zip_path, timeout=max_latency)
        with zipfile.ZipFile(zip_path, "r") as archive:
            xray_file = archive.read("xray")
        bin_fname = f"xray-{'-'.join(system_info)}"
        with open(os.path.join(bin_dir, bin_fname), "wb") as binoutfile:
            binoutfile.write(xray_file)
        # TODO check if change is required in windows or mac
        os.chmod(os.path.join(bin_dir, bin_fname), 0o775)
    except FileDownloadError as e:
        logger.error(
            "Failed to download the release zip file from xtls xray-core github repo", str(system_info))
        logger.exception(e)
        return False
    except KeyError as e:
        logger.error("Failed to get binary from zip file", zip_url)
        logger.exception(e)
        return False
    except Exception as e:
        logger.error("Unknown error", str(system_info))
        logger.exception(e)
        return False


def get_latest_release() -> dict:
    """Get the latest release info from github

    Returns:
        dict: release info including the download url
    """
    url = f"https://api.github.com/repos/XTLS/Xray-core/releases/latest"
    try:
        r = requests.get(url)
        release_info = r.json()
        if r.status_code != 200:
            raise Exception(f"Failed to get release info: {r.status_code}")
    except Exception as e:
        raise e

    return release_info
