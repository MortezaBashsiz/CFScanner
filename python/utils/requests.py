import requests
from report.clog import CLogger
from utils.exceptions import *


logger = CLogger("requests", file_log_level=1, console_log_level=1)


def download_file(
    url: str,
    save_path: str,
    timeout: float = 10,
    chunk_size: int = 1024

):
    """Download a file from a url to a path.

    Args:
        url (str): the url to download the file from
        savepath (str): the path to save the file to
        timeout (float, optional): timeout for ``requests.get`` note that this not limit the total download time, only the RTT. Defaults to 10.
    """
    r = requests.get(url, stream=True, timeout=timeout)
    try:
        with open(save_path, "wb") as zipout:
            for chunk in r.iter_content(chunk_size=chunk_size):
                if chunk:
                    zipout.write(chunk)
    except Exception as e:
        logger.exception(e)
        raise(FileDownloadError("Error downloading file from {url} to {savepath}"))
    return True
