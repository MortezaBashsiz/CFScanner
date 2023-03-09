import logging
import os
import sys
from logging import StreamHandler as _SH
from logging.handlers import TimedRotatingFileHandler as _TRFH

# windows only: ←[0m enfernen
if os.name == "nt":
    os.system("color")

PATH = os.path.dirname(os.path.realpath(__file__))

# basic logging configuration
_COLORS = dict(
    CRITICAL="\033[95m",
    FATAL="\033[95m",
    ERROR="\033[91m",
    WARNING="\033[33m",
    INFO="\x1b[0m",
    DEBUG="\033[94m",
    SUCCESS="\033[32m",
)

# add custom class
_SUCCESS = 45


class _CustomLogger(logging.Logger):
    """Custom Logger Class"""

    def success(self, msg, *args, **kwargs):
        if self.isEnabledFor(_SUCCESS):
            self._log(_SUCCESS, msg, args, **kwargs)


class _CustomFormat(object):
    DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

    @staticmethod
    def create(colored=True, show_task_name=False):
        task_name = "[%(threadName)-3s] " if show_task_name else ""
        color_start, color_end = "", ""
        if colored:
            color_start = "%(color)s"
            color_end = "\033[0m"

        return (
            "%(asctime)s.%(msecs)03d   %(name)s   {color_start}"
            "{task_name}[%(levelname).3s] [%(prefix)-3s]   "
            "%(message)s{color_end}".format(
                task_name=task_name, color_start=color_start, color_end=color_end
            )
        )


class _MessageFilter(logging.Filter):
    """Setting/Formatting all record arguments and adding color."""

    def filter(self, record):
        record.prefix = ""
        record.levelname = record.levelname.replace("Level ", "").lower()
        record.color = _COLORS[record.levelname.upper()]

        if record.threadName == "MainThread":
            record.threadName = 0

        if "prefix" in record.args:
            if prefix := record.args.get("prefix"):
                record.prefix = record.args.get("prefix")

        return True


logging.setLoggerClass(_CustomLogger)
logging.addLevelName(_SUCCESS, "SUCCESS")


class CLogger(object):
    """Logger to import in every project"""

    def __init__(
        self,
        module,
        colored=True,
        console_log_level=1,
        file_log_level=25,
        show_task_name=False,
    ):

        module = module.lower()

        self._logger = logging.getLogger(module)
        if not self._logger.hasHandlers():
            self.add_handlers(
                module, colored, show_task_name, console_log_level, file_log_level
            )

        self._prefix = None

    def add_handlers(
        self, module, colored, show_task_name, console_log_level, file_log_level
    ):
        # create a new logger and add filters and file handler

        console_fmt = _CustomFormat.create(colored, show_task_name)
        file_fmt = _CustomFormat.create(False, show_task_name)
        date_fmt = _CustomFormat.DATE_FORMAT

        # set console logging
        stream_fmt = logging.Formatter(console_fmt, datefmt=date_fmt)
        stream_handler = _SH(stream=sys.stdout)
        stream_handler.setFormatter(stream_fmt)
        stream_handler.setLevel(console_log_level)

        # set file handler and file logging
        file_fmt = logging.Formatter(file_fmt, datefmt=date_fmt)

        # create log folder if not exist already
        log_dir_path = os.path.join(PATH, "log")
        if not os.path.isdir(log_dir_path):
            os.mkdir(log_dir_path)

        filename = os.path.join(log_dir_path, f"{module}.log")
        file_handler = _TRFH(
            filename=filename,
            when="D",
            interval=1,
            backupCount=30,
            encoding="utf-8",
            delay=False,
        )
        file_handler.setFormatter(file_fmt)
        file_handler.setLevel(file_log_level)
        self._logger.addHandler(file_handler)
        self._logger.addHandler(stream_handler)
        self._logger.addFilter(_MessageFilter())
        self._logger.setLevel(console_log_level)

    def set_prefix(self, prefix):
        self._prefix = prefix

    def info(self, msg: str, prefix: str = None):
        if not prefix:
            prefix = self._prefix
        msg = msg.replace("%", "%%")
        self._logger.info(msg, {"prefix": prefix})

    def warn(self, msg: str, prefix: str = None):
        if not prefix:
            prefix = self._prefix
        msg = msg.replace("%", "%%")
        self._logger.warning(msg, {"prefix": prefix})

    def error(self, msg: str, prefix: str = None):
        if not prefix:
            prefix = self._prefix
        msg = msg.replace("%", "%%")
        self._logger.error(msg, {"prefix": prefix})

    def exception(self, e):
        self._logger.exception(e)

    def success(self, msg: str, prefix: str = None):
        if not prefix:
            prefix = self._prefix
        msg = msg.replace("%", "%%")
        self._logger.success(msg, {"prefix": prefix})

    def debug(self, msg: str, prefix: str = None):
        if not prefix:
            prefix = self._prefix
        msg = msg.replace("%", "%%")
        self._logger.debug(msg, {"prefix": prefix})


if __name__ == "__main__":
    for _ in range(2):
        log = CLogger("testinstance")

        log.info("INFO MESSAGE")
        log.warn("WARNING MESSAGE", "403")
        log.error("ERROR MESSAGE", "TimeoutError")
        log.success("SUCCESS MESSAGE", "jones@gmail.com")
        log.debug("DEBUG MESSAGE", "a == b")
