import logging

import requests

from ..args.testconfig import TestConfig
from ..report.print import no_and_kill, ok_message
from ..utils.decorators import timeout_fun
from ..utils.exceptions import StartProxyServiceError
from ..xray.config import create_proxy_config
from ..xray.service import start_proxy_service
from .download import download_speed_test
from .fronting import fronting_test
from .upload import upload_speed_test

log = logging.getLogger(__name__)


class TestResult:
    """class to store test results"""

    def __init__(self, ip, cidr, n_tries):
        self.ip = ip
        self.cidr = cidr

        self.is_ok = False
        self.n_tries = n_tries
        self.message = ""

        self.result = dict(
            ip=ip,
            success=False,
            download=dict(
                speed=[-1] * self.n_tries, latency=[-1] * self.n_tries
            ),
            upload=dict(
                speed=[-1] * self.n_tries, latency=[-1] * self.n_tries
            ),
        )

    def __bool__(self):
        return self.is_ok


class _FakeProcess:
    def __init__(self):
        pass

    def kill(self):
        pass


def test_ip(ip_cidr: tuple, test_config: TestConfig, config_dir: str):
    ip, cidr = ip_cidr
    test_result = TestResult(ip=ip, cidr=cidr, n_tries=test_config.n_tries)

    if not test_config.no_fronting:
        for try_idx in range(test_config.n_tries):
            fronting_result_msg = fronting_test(
                ip, timeout=test_config.fronting_timeout
            )
            if "NO" in fronting_result_msg:
                test_result.message = fronting_result_msg
                test_result.is_ok = False
                return test_result

    if not test_config.novpn:
        try:
            proxy_config_path = create_proxy_config(
                edge_ip=ip, test_config=test_config, config_dir=config_dir
            )
        except Exception:
            test_result.message = no_and_kill(
                ip=ip,
                message="Could not save proxy (xray/v2ray) config to file",
                process=_FakeProcess(),
            )
            test_result.is_ok = False
            return test_result

    if not test_config.novpn:
        try:
            process, proxies = start_proxy_service(
                proxy_conf_path=proxy_config_path,
                binary_path=test_config.binpath,
                timeout=test_config.startprocess_timeout,
            )
        except Exception:
            test_result.is_ok = False
            raise StartProxyServiceError(
                f"Could not start xray service - {ip}"
            )

    else:
        process = _FakeProcess()
        proxies = None

    @timeout_fun(test_config.max_dl_latency + test_config.max_dl_time)
    def timeout_download_fun():
        return download_speed_test(
            n_bytes=n_bytes,
            proxies=proxies,
            timeout=test_config.max_dl_latency,
        )

    for try_idx in range(test_config.n_tries):
        # check download speed
        n_bytes = test_config.min_dl_speed * 1000 * test_config.max_dl_time
        try:
            dl_speed, dl_latency = timeout_download_fun()
        except TimeoutError:
            fail_msg = no_and_kill(
                ip=ip, message="download timeout exceeded", process=process
            )
            test_result.message = fail_msg
            test_result.is_ok = False
            return test_result
        except (
            requests.exceptions.ReadTimeout,
            requests.exceptions.ConnectionError,
            requests.ConnectTimeout,
        ):
            fail_msg = no_and_kill(
                ip=ip, message="download error", process=process
            )
            test_result.message = fail_msg
            test_result.is_ok = False
            return test_result
        except Exception as e:
            log.exception(e)
            log.error(
                f"Download unknown error: {e}",
                extra={
                    "ip": ip,
                },
            )
            fail_msg = no_and_kill(
                ip=ip, message="download unknown error", process=process
            )
            test_result.message = fail_msg
            test_result.is_ok = False
            return test_result

        if dl_latency <= test_config.max_dl_latency:
            dl_speed_kBps = dl_speed / 8 * 1000
            if dl_speed_kBps >= test_config.min_dl_speed:
                test_result.result["download"]["speed"][try_idx] = dl_speed
                test_result.result["download"]["latency"][try_idx] = round(
                    dl_latency * 1000
                )
            else:
                message = f"download too slow {dl_speed_kBps:.2f} < {test_config.min_dl_speed:.2f} kBps"
                fail_msg = no_and_kill(ip=ip, message=message, process=process)
                test_result.message = fail_msg
                test_result.is_ok = False
                return test_result
        else:
            message = f"high download latency {dl_latency:.4f} s > {test_config.max_dl_latency:.4f} s"
            fail_msg = no_and_kill(ip=ip, message=message, process=process)
            test_result.message = fail_msg
            test_result.is_ok = False
            return test_result

        # upload speed test
        if test_config.do_upload_test:
            n_bytes = test_config.min_ul_speed * 1000 * test_config.max_ul_time
            try:
                up_speed, up_latency = upload_speed_test(
                    n_bytes=n_bytes,
                    proxies=proxies,
                    timeout=test_config.max_ul_latency
                    + test_config.max_ul_time,
                )
            except requests.exceptions.ReadTimeout:
                fail_msg = no_and_kill(ip, "upload read timeout", process)
                test_result.message = fail_msg
                test_result.is_ok = False
                return test_result
            except requests.exceptions.ConnectTimeout:
                fail_msg = no_and_kill(ip, "upload connect timeout", process)
                test_result.message = fail_msg
                test_result.is_ok = False
                return test_result
            except requests.exceptions.ConnectionError:
                fail_msg = no_and_kill(ip, "upload connection error", process)
                test_result.message = fail_msg
                test_result.is_ok = False
                return test_result
            except Exception as e:
                log.exception(e)
                log.error(
                    f"Upload unknown error: {e}",
                    extra={
                        "ip": ip,
                    },
                )
                fail_msg = no_and_kill(ip, "Upload unknown error", process)
                test_result.message = fail_msg
                test_result.is_ok = False
                return test_result

            if up_latency > test_config.max_ul_latency:
                fail_msg = no_and_kill(ip, "upload latency too high", process)
                test_result.message = fail_msg
                test_result.is_ok = False
                return test_result
            up_speed_kBps = up_speed / 8 * 1000
            if up_speed_kBps >= test_config.min_ul_speed:
                test_result.result["upload"]["speed"][try_idx] = up_speed
                test_result.result["upload"]["latency"][try_idx] = round(
                    up_latency * 1000
                )
            else:
                message = f"upload too slow {up_speed_kBps:.2f} kBps < {test_config.min_ul_speed:.2f} kBps"
                fail_msg = no_and_kill(ip, message, process)
                test_result.message = fail_msg
                test_result.is_ok = False
                return test_result

    process.kill()

    test_ok_msg = ok_message(test_result.result)

    test_result.is_ok = True
    test_result.message = test_ok_msg
    return test_result
