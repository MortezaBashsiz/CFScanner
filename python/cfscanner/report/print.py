from statistics import mean
from subprocess import Popen
from typing import Iterable, Optional, Union

from rich.console import Console
from rich.progress import GetTimeCallable, Progress, ProgressColumn, Task
from rich.table import Column, Table

from ..speedtest.tools import mean_jitter


class TitledProgress(Progress):
    def __init__(
        self, 
        *columns: Union[str, ProgressColumn],
        title = None,
        console: Optional[Console] = None,
        auto_refresh: bool = True, 
        refresh_per_second: float = 10, 
        speed_estimate_period: float = 30, 
        transient: bool = False, 
        redirect_stdout: bool = True, 
        redirect_stderr: bool = True,
        get_time: Optional[GetTimeCallable] = None, 
        disable: bool = False, 
        expand: bool = False,
        scanned_ips: int = 0,
        ok_ips: int = 0
    ) -> None:
        self.title = title
        self.scanned_ips = scanned_ips
        self.ok_ips = ok_ips
        super().__init__(
            *columns, 
            console=console, 
            auto_refresh=auto_refresh, 
            refresh_per_second=refresh_per_second,
            speed_estimate_period=speed_estimate_period,
            transient=transient, redirect_stdout=redirect_stdout, 
            redirect_stderr=redirect_stderr, 
            get_time=get_time,
            disable=disable,
            expand=expand
        )
        
    def make_tasks_table(self, tasks: Iterable[Task]) -> Table:
        table_columns = (
            (
                Column(no_wrap=True)
                if isinstance(_column, str)
                else _column.get_table_column().copy()
            )
            for _column in self.columns
        )
        table = Table.grid(*table_columns, padding=(0, 1), expand=self.expand)

        if self.title is not None: 
            table.add_row(self.title)
        if self.scanned_ips is not None and self.ok_ips is not None:
            table.add_row(
                f"scanned ips:{str(self.scanned_ips).ljust(8)}" 
                f"ok ips: {self.ok_ips} "
                f"({self.ok_ips / self.scanned_ips * 100:.2f}%)" if self.scanned_ips > 0 else ""
            )
            
        for task in tasks:
            if task.visible:
                table.add_row(
                    *(
                        (
                            column.format(task=task)
                            if isinstance(column, str)
                            else column(task)
                        )
                        for column in self.columns
                    )
                )
        return table

def no_and_kill(
    ip: str,
    message: str,
    process: Popen
):
    """prints an error message together with the respective ip that causes the error message in a nice colored format

    Args:
        ip (str): the ip that causes the error
        message (str): the message related to the error
        process (Popen): the process (xray) to be killed
    """
    process.kill()
    return f"[bold red1]NO[/bold red1] [orange3]{ip:15s}[/orange3] [yellow1]{message}[/yellow1]"


def ok_message(
    scan_result: dict
) -> None:
    """prints the result if test is ok

    Args:
        scan_result (dict): the results of the scan
    """
    down_mean_jitter = mean_jitter(scan_result["download"]["latency"])
    up_mean_jitter = mean_jitter(scan_result["upload"]["latency"])
    mean_down_speed = mean(scan_result["download"]["speed"])
    mean_up_speed = mean(scan_result["upload"]["speed"])
    mean_down_latency = mean(scan_result["download"]["latency"])
    mean_up_latency = mean(scan_result["upload"]["latency"])
    return f"[bold green1]"\
        f"OK [/bold green1][cyan1]{scan_result['ip']:15s}[/cyan1][bright_blue] "\
        f"avg_down_speed: {mean_down_speed:7.4f}mbps "\
        f"avg_up_speed: {mean_up_speed:7.4f}mbps "\
        f"avg_down_latency: {mean_down_latency:7.2f}ms "\
        f"avg_up_latency: {mean_up_latency:7.2f}ms "\
        f"avg_down_jitter: {down_mean_jitter:7.2f}ms "\
        f"avg_up_jitter: {up_mean_jitter:4.2f}ms"\
        f"[/bright_blue]"


def color_text(text: str, rgb: tuple, bold: bool = False):
    """prints a colored text in the terminal using ANSI escape codes based on the rgb values

    Args:
        text (str): the text to be printed
        rgb (tuple): the rgb values of the color
    """
    if bold:
        return f"\033[1m\033[38;2;{rgb[0]};{rgb[1]};{rgb[2]}m{text}\033[m"
    else:
        return f"\033[38;2;{rgb[0]};{rgb[1]};{rgb[2]}m{text}\033[m"


def box_text(text: str):
    """prints a box around the text

    Args:
        text (str): the text to be printed in the box

    Returns:
        str: the text with the box around it 
    """
    lines = text.splitlines()
    max_width = max(len(line) for line in lines)

    # Define the ANSI escape codes for the header
    HEADER_COLOR = '\033[38;2;64;224;208m'
    BORDER_COLOR = '\033[38;2;100;100;100m'
    BOX_WIDTH = max_width + 10

    res_text = ""

    # Print the header with a colored border
    res_text += f"{BORDER_COLOR}{'+' + '-' * (BOX_WIDTH - 2) + '+'}\n"
    for line in lines:
        res_text += f"{BORDER_COLOR}|{HEADER_COLOR}{line.center(BOX_WIDTH - 2)}{BORDER_COLOR}|\n"
    res_text += f"{BORDER_COLOR}{'+' + '-' * (BOX_WIDTH - 2) + '+'}\033[m"

    return res_text
