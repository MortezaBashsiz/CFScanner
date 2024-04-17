from rich.console import Console
import logging
import fancylogging
import os
import pathlib
from datetime import datetime

SCRIPTDIR = os.getcwd()
CONFIGDIR = f"{SCRIPTDIR}/.xray-configs"
RESULTDIR = f"{SCRIPTDIR}/result"
START_DT_STR = datetime.now().strftime(r"%Y%m%d_%H%M%S")
INTERIM_RESULTS_PATH = os.path.join(RESULTDIR, f"{START_DT_STR}_result.csv")
CWD = pathlib.Path(os.getcwd())

console = Console()
fancylogging.setup_fancy_logging(
    "cfscanner",
    console_log_level=logging.INFO,
    file_log_level=logging.DEBUG,
    file_log_path=CWD / "log" / f"{START_DT_STR}.jsonl",
    console=console,
)
