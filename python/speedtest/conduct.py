import requests

from args.testconfig import TestConfig
from report.clog import CLogger
from report.print import print_and_kill
from utils.decorators import timeout_fun
from xray.config import create_proxy_config
from xray.service import start_proxy_service

from .download import download_speed_test
from .fronting import fronting_test
from .upload import upload_speed_test

log = CLogger("cfscanner-speedtest")



class _FakeProcess:
    def __init__(self):
        pass

    def kill(self):
        pass


def test_ip(
    ip: str,
    test_config: TestConfig,
    config_dir: str
):
    result = dict(
        ip=ip,
        download=dict(
            speed=[-1] * test_config.n_tries,
            latency=[-1] * test_config.n_tries
        ),
        upload=dict(
            speed=[-1] * test_config.n_tries,
            latency=[-1] * test_config.n_tries
        ),
    )

    for try_idx in range(test_config.n_tries):
        if not fronting_test(ip, timeout=test_config.fronting_timeout):
            return False

    if not test_config.novpn:
        try:
            proxy_config_path = create_proxy_config(
                edge_ip=ip,
                test_config=test_config,
                config_dir=config_dir
            )
        except Exception as e:
            log.error("Could not save proxy (xray/v2ray) config to file", ip)
            log.exception(e)
            return print_and_kill(
                ip=ip,
                message="Could not save proxy (xray/v2ray) config to file",
                process=process
            )

    if not test_config.novpn:
        try:
            process, proxies = start_proxy_service(
                proxy_conf_path=proxy_config_path,
                binary_path=test_config.binpath,
                timeout=test_config.startprocess_timeout
            )
        except Exception as e:
            message = "Could not start proxy (v2ray/xray) service"
            log.error(message, ip)
            log.exception(e)
            print_and_kill(ip=ip, message=message, process=process)
    else:
        process = _FakeProcess()
        proxies = None

    @timeout_fun(test_config.max_dl_latency + test_config.max_dl_time)
    def timeout_download_fun():
        return download_speed_test(
            n_bytes=n_bytes,
            proxies=proxies,
            timeout=test_config.max_dl_latency
        )

    for try_idx in range(test_config.n_tries):
        # check download speed
        n_bytes = test_config.min_dl_speed * 1000 * test_config.max_dl_time
        try:
            dl_speed, dl_latency = timeout_download_fun()
        except TimeoutError as e:
            return print_and_kill(ip=ip, message="download timeout exceeded", process=process)
        except (requests.exceptions.ReadTimeout, requests.exceptions.ConnectionError, requests.ConnectTimeout) as e:
            return print_and_kill(ip=ip, message="download error", process=process)
        except Exception as e:
            log.error("Download - unknown error", ip)
            log.exception(e)
            return print_and_kill(ip=ip, message="download unknown error", process=process)

        if dl_latency <= test_config.max_dl_latency:
            dl_speed_kBps = dl_speed / 8 * 1000
            if dl_speed_kBps >= test_config.min_dl_speed:
                result["download"]["speed"][try_idx] = dl_speed
                result["download"]["latency"][try_idx] = round(
                    dl_latency * 1000)
            else:
                message = f"download too slow {dl_speed_kBps:.2f} < {test_config.min_dl_speed:.2f} kBps"
                return print_and_kill(ip=ip, message=message, process=process)
        else:
            message = f"high download latency {dl_latency:.4f} s > {test_config.max_dl_latency:.4f} s"
            return print_and_kill(ip=ip, message=message, process=process)

        # upload speed test
        if test_config.do_upload_test:
            n_bytes = test_config.min_ul_speed * 1000 * test_config.max_ul_time
            try:
                up_speed, up_latency = upload_speed_test(
                    n_bytes=n_bytes,
                    proxies=proxies,
                    timeout=test_config.max_ul_latency + test_config.max_ul_time
                )
            except requests.exceptions.ReadTimeout:
                return print_and_kill(ip, 'upload read timeout', process)
            except requests.exceptions.ConnectTimeout:
                return print_and_kill(ip, 'upload connect timeout', process)
            except requests.exceptions.ConnectionError:
                return print_and_kill(ip, 'upload connection error', process)
            except Exception as e:
                log.error("Upload - unknown error", ip)
                log.exception(e)
                return print_and_kill(ip, 'upload unknown error', process)

            if up_latency > test_config.max_ul_latency:
                return print_and_kill(ip, 'upload latency too high', process)
            up_speed_kBps = up_speed / 8 * 1000
            if up_speed_kBps >= test_config.min_ul_speed:
                result["upload"]["speed"][try_idx] = up_speed
                result["upload"]["latency"][try_idx] = round(up_latency * 1000)
            else:
                message = f"upload too slow {up_speed_kBps:.2f} kBps < {test_config.min_ul_speed:.2f} kBps"
                return print_and_kill(ip, message, process)

    process.kill()
    return result
