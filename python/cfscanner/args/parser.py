import argparse

from rich.console import Console

from ..report.print import color_text

console = Console()


def _title(text):
    return color_text(text, (128, 128, 0), bold=True)


def parse_args():
    parser = argparse.ArgumentParser(
        description=color_text(
            'Cloudflare edge ips scanner to use with xray (or v2ray)',
            rgb=(76, 122, 164),
            bold=True
        ),
        add_help=False
    )

    def formatter(prog): return argparse.HelpFormatter(
        prog, width=100, max_help_position=64)
    parser.formatter_class = formatter

    ############################################################
    # Help options
    help_options = parser.add_argument_group(_title("Help"))
    help_options.add_argument(
        "--help", "-h",
        help="Show this help message and exit",
        action="help"
    )

    ############################################################
    # General options
    general_grp = parser.add_argument_group(_title("General options"))
    general_grp.add_argument(
        "--threads", "-t",
        dest="threads",
        metavar="",
        help="Number of threads to use for parallel scanning, default is 1",
        type=int,
        required=False,
        default=1
    )
    general_grp.add_argument(
        "--tries", "-n",
        metavar="",
        help="Number of times to try each IP. An IP is marked as OK if all tries are successful, default is 1",
        dest="n_tries",
        default=1,
        type=int,
        required=False
    )
    general_grp.add_argument(
        "--subnets", "-s",
        help="The path to the custom subnets file. Each line should be either a single ip (v4 or v6)"
        " or a subnet in cidr notation (v4 or v6). If not provided, the program will read the list of cidrs"
        " from https://github.com/MortezaBashsiz/CFScanner/blob/main/config/cf.local.iplist",
        type=str,
        metavar="",
        dest="subnets",
        required=False
    )
    ############################################################
    # Random scan options
    randomscan_grp = parser.add_argument_group(
        _title("Random scan options")
    )
    randomscan_grp.add_argument(
        "--sample", "-r",
        help="Size of the random sample to take from each subnet. The sample size can either be "
             "a float between 0 and 1 or an integer. If it is a float, it will be interpreted "
             "as a percentage of the subnet size. If it is an integer, it will be interpreted as "
             "the number of ips to take from each subnet. If not provided, the program will take "
             "all ips from each subnet",
        type=float,
        metavar="",
        dest="sample_size",
        required=False
    )   
    randomscan_grp.add_argument(
        "--shuffle-subnets",
        help="If passed, the subnets will be shuffled before scanning.",
        action="store_true",
        dest="shuffle_subnets",
        required=False,
        default=False        
    )
    randomscan_grp.add_argument(
        "--sampling-timeout",
        help="Maximum time (in seconds) to wait for a random sample to be taken from a subnet, default is 1",
        type=float,
        metavar="",
        dest="sampling_timeout",
        default=1,
        required=False
    )
    ############################################################
    # Xray config options
    config_options = parser.add_argument_group(_title("Xray config options"))
    config_or_template = config_options.add_mutually_exclusive_group(
        required=False
    )
    config_or_template.add_argument(
        "--config", "-c",
        help="The path to the config file. For config file example,"
        " see sudoer default config: https://github.com/MortezaBashsiz/CFScanner/blob/main/cofig/ClientConfig.json"
        " If not provided, the program will read the default sudoer config file",
        metavar="",
        dest="config_path",
        type=str,
        required=False
    )
    config_or_template.add_argument(
        "--template",
        type=str,
        help="Path to the proxy (v2ray/xray) client file template. By default vmess_ws_tls is used",
        metavar="",
        required=False,
        dest="template_path"
    )
    config_options.add_argument(
        "--binpath", "-b",
        help="Path to the v2ray/xray binary file. If not provided, will use the latest compatible version of xray",
        type=str,
        metavar="",
        dest="binpath",
        required=False
    )
    config_or_template.add_argument(
        "--novpn",
        help="If passed, xray/v2ray service will not be started and the program will not use vpn",
        action="store_true",
        dest="no_vpn",
        default=False,
        required=False
    )
    config_options.add_argument(
        "--startprocess-timeout",
        help="Maximum time (in seconds) to wait for xray/v2ray process to start, default is 5",
        type=float,
        metavar="",
        dest="startprocess_timeout",
        default=5
    )
    ############################################################
    # Fronting options
    fronting_test_grp = parser.add_argument_group(
        _title("Fronting speed test options")
    )
    ft_or_nofronting = fronting_test_grp.add_mutually_exclusive_group(
        required=False
    )
    ft_or_nofronting.add_argument(
        "--fronting-timeout", "-FT",
        metavar="",
        help="Maximum time to wait for fronting response, default is 1",
        type=float,
        dest="fronting_timeout",
        default=1,
        required=False
    )
    ft_or_nofronting.add_argument(
        "--no-fronting",
        help="If passed, fronting speed test will not be performed",
        action="store_true",
        dest="no_fronting",
        default=False,
        required=False
    )
    ############################################################
    # download options
    download_speed_grp = parser.add_argument_group(
        _title("Download speed test options"))
    download_speed_grp.add_argument(
        "--download-speed", "-DS",
        help="Minimum acceptable download speed in kilobytes per second, default is 50",
        metavar="",
        type=int,
        dest="min_dl_speed",
        default=50,
        required=False
    )
    download_speed_grp.add_argument(
        "--download-latency", "-DL",
        help="Maximum allowed latency (seconds) for download, default is 2",
        type=int,
        metavar="",
        dest="max_dl_latency",
        default=2,
        required=False
    )
    download_speed_grp.add_argument(
        "--download-time", "-DT",
        metavar="",
        help="Maximum (effective, excluding http time) time to spend for each download, default is 2",
        type=int,
        dest="max_dl_time",
        default=2,
        required=False
    )
    ############################################################
    # upload options
    upload_speed_grp = parser.add_argument_group(
        _title("Upload speed test options")
    )
    upload_speed_grp.add_argument(
        "--upload-test", "-U",
        help="If passed, upload test will be conducted. If not passed, only download and fronting test will be conducted",
        dest="do_upload_test",
        action="store_true",
        default=False,
        required=False
    )
    upload_speed_grp.add_argument(
        "--upload-speed", "-US",
        help="Minimum acceptable upload speed in kilobytes per second, default is 50",
        metavar="",
        type=int,
        dest="min_ul_speed",
        required=False
    )
    upload_speed_grp.add_argument(
        "--upload-latency", "-UL",
        help="Maximum allowed latency (seconds) for upload, default is 2",
        type=int,
        metavar="",
        dest="max_ul_latency",
        default=2,
        required=False
    )
    upload_speed_grp.add_argument(
        "--upload-time", "-UT",
        metavar="",
        help="Maximum (effective, excluding http time) time (in seconds) "
        "to spend for each upload, default is 2",
        type=int,
        dest="max_ul_time",
        default=2,
        required=False
    )
    ############################################################

    parsed_args = parser.parse_args()

    if parsed_args.sample_size is not None:
        if 0 < parsed_args.sample_size < 1:
            parsed_args.sample_size = float(parsed_args.sample_size)
        elif parsed_args.sample_size >= 1:
            if parsed_args.sample_size % 1 > 0.000001:
                console.log(
                    f"[yellow]Sample size rounded to integer value: {round(parsed_args.sample_size)}[/yellow]"
                )
            parsed_args.sample_size = round(parsed_args.sample_size)
        else:
            raise ValueError(color_text(
                "Sample size must be a positive number.", rgb=(255, 0, 0)))

    return parsed_args
