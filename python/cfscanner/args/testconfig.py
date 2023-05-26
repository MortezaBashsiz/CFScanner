import argparse
import json
import os

from ..utils.exceptions import *
from ..utils.exceptions import BinaryNotFoundError, TemplateReadError
from ..utils.os import detect_system
from ..utils.requests import download_file
from ..xray import templates
from ..xray.binary import download_binary

SCRIPTDIR = os.getcwd()

class TestConfig:
    @classmethod
    def from_args(cls, args: argparse.Namespace):
        """creates a TestConfig object from the arguments passed to the program

        Args:
            args (argparse.Namespace): arguments passed to the program
        """
        # create test config
        test_config = cls()

        # load config if need be
        if not args.no_vpn and args.template_path is None:
            if args.config_path is None:
                os.makedirs(os.path.join(SCRIPTDIR, ".tmp"), exist_ok=True)
                download_file(
                    url="https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/config/ClientConfig.json",
                    save_path=os.path.join(SCRIPTDIR, ".tmp", "sudoer_config.json")
                )
                args.config_path = os.path.join(
                    SCRIPTDIR, ".tmp", "sudoer_config.json")
            with open(args.config_path, "r") as infile:
                file_content = json.load(infile)
                test_config.user_id = file_content["id"]
                test_config.ws_header_host = file_content["host"]
                test_config.address_port = int(file_content["port"])
                test_config.user_id = file_content["id"]
                test_config.ws_header_path = "/" + \
                    (file_content["path"].lstrip("/"))

        if args.template_path is None:
            test_config.custom_template = False  # user did not provide a custom template
            test_config.proxy_config_template = templates.vmess_ws_tls
        else:
            test_config.custom_template = True  # user provided a custom template
            try:
                with open(args.template_path, "r") as infile:
                    test_config.proxy_config_template = infile.read()
            except FileNotFoundError:
                raise TemplateReadError("template file not found")
            except IsADirectoryError:
                raise TemplateReadError(
                    "template file is a directory. please provide the path to the file")
            except PermissionError:
                raise TemplateReadError(
                    "permission denied while reading template file")
            except Exception as e:
                raise TemplateReadError(
                    f"error while reading template file: {e}"
                )

        # speed related config
        test_config.startprocess_timeout = args.startprocess_timeout
        test_config.do_upload_test = args.do_upload_test or args.min_ul_speed is not None
        test_config.min_ul_speed = args.min_ul_speed if args.min_ul_speed is not None else 50
        test_config.min_dl_speed = args.min_dl_speed
        test_config.max_dl_time = args.max_dl_time
        test_config.max_ul_time = args.max_ul_time
        test_config.fronting_timeout = args.fronting_timeout
        test_config.max_dl_latency = args.max_dl_latency
        test_config.max_ul_latency = args.max_ul_latency
        test_config.n_tries = args.n_tries
        test_config.novpn = args.no_vpn
        test_config.no_fronting = args.no_fronting

        test_config.sample_size = args.sample_size
        test_config.sampling_timeout = args.sampling_timeout

        system_info = detect_system()
        if test_config.novpn:
            test_config.binpath = None
        elif args.binpath is not None:
            # Check if file exists
            if not os.path.isfile(args.binpath):
                raise BinaryNotFoundError(
                    "The binary path provided does not exist"
                )
            test_config.binpath = args.binpath
        else:
            try:
                test_config.binpath = download_binary(
                    system_info=system_info,
                    bin_dir=SCRIPTDIR
                )
            except Exception as e:
                raise BinaryDownloadError(str(e))

        return test_config
