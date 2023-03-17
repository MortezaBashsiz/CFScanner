import os
import zipfile

import requests
from utils.systemtools import create_dir
import os

from . import SUPPORTED, CLogger
from cfFindIP import BINDIR
from cfFindIP import SCRIPTDIR

logger = CLogger("xraybinary")


def download_binary(system_info: tuple, bin_dir: str = None, url: str = None) -> None:
    """Download a binary from a url to a path.

    Args:
        system_info (tuple): (system, arch, abi)
        binary_path (str, optional): path to the binary to be downloaded
        url (str, optional): url to download the binary from. Defaults to None.
    """
    if system_info not in SUPPORTED:
        raise OSError(f"System {system_info} not supported")
    
    if bin_dir is None:
        bin_dir = BINDIR

    if url is None:
        logger.debug("Getting latest release info")
        release_info = get_latest_release()
        assets = release_info["assets"]
        compatible_asset = next(
            a for a in assets if "Xray-" + "-".join(system_info) + ".zip" == a["name"])
        zip_url = compatible_asset["browser_download_url"]
        logger.debug(zip_url)
        zipdir = os.path.join(SCRIPTDIR, ".tmp")
        logger.debug(zipdir)
        os.makedirs(zipdir, exist_ok=True)
        zip_path = os.path.join(zipdir, f"{compatible_asset['name']}")
        logger.debug(zip_path)
        try:
            r = requests.get(zip_url, stream=True)
            with open(zip_path, "wb") as zipout:
                for chunk in r.iter_content(chunk_size=1024):
                    if chunk:
                        zipout.write(chunk)

            with zipfile.ZipFile(zip_path, "r") as archive:
                xray_file = archive.read("xray")
            logger.debug("here")
            bin_fname = f"xray-{'-'.join(system_info)}"
            logger.debug(bin_fname)
            logger.debug(os.path.join(bin_dir, bin_fname))
            with open(os.path.join(bin_dir, bin_fname), "wb") as binfile:
                binfile.write(xray_file)
            os.chmod(os.path.join(bin_dir, bin_fname), 0o775)
        except Exception as e:
            logger.exception(e)
    else:  
        #TOTO: download from url
        pass 
        

       


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
