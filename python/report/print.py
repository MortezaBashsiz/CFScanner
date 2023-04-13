from . import Colors
from subprocess import Popen
from speedtest.tools import mean_jitter
from statistics import mean


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
    return f"[bold red]NO[/bold red] [orange]{ip:15s}[/orange] [yellow]{message}[/yellow]"


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
    return f"[green]"\
        f"OK [green][blue_violet]{scan_result['ip']:15s}[/blue_violet][blue] "\
        f"avg_down_speed: {mean_down_speed:7.4f}mbps "\
        f"avg_up_speed: {mean_up_speed:7.4f}mbps "\
        f"avg_down_latency: {mean_down_latency:7.2f}ms "\
        f"avg_up_latency: {mean_up_latency:7.2f}ms "\
        f"avg_down_jitter: {down_mean_jitter:7.2f}ms "\
        f"avg_up_jitter: {up_mean_jitter:4.2f}ms"\
        f"[/blue]"
    
    
    
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
