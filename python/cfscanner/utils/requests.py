import requests
from rich.console import Console
from ..utils.exceptions import *

console = Console()


def download_file(
    url: str,
    save_path: str,
    timeout: float = 10,
    chunk_size: int = 1024

):
    """Download a file from a url to a path.

    Args:
        url (str): the url to download the file from
        save_path (str): the path to save the file to
        timeout (float, optional): timeout for ``requests.get`` note that this not limit the total download time, only the RTT. Defaults to 10.
    """
    r = requests.get(url, stream=True, timeout=timeout)
    try:
        with open(save_path, "wb") as zip_out:
            for chunk in r.iter_content(chunk_size=chunk_size):
                if chunk:
                    zip_out.write(chunk)
    except Exception:
        console.print_exception()
        raise (
            FileDownloadError(
                f"Error downloading file from {url} to {save_path}"
            )
        )
    return True
