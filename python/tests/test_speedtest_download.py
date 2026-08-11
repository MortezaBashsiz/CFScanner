from datetime import timedelta

import pytest

from cfscanner.speedtest import download


def test_download_speed_accepts_integer_server_timing_duration(monkeypatch):
    """Cloudflare emits integer ``dur`` values, not only decimal ones."""

    class Response:
        headers = {
            "Server-Timing": (
                'cfSpeedEdge;dur=4, cfSpeedWorker;dur=28, '
                'cfL4;desc="?proto=TCP&rtt=0"'
            )
        }
        elapsed = timedelta(milliseconds=50)
        content = b"x"

    monkeypatch.setattr(download.requests, "get", lambda **kwargs: Response())
    monkeypatch.setattr(download.time, "perf_counter", iter([10.0, 10.1]).__next__)

    speed, latency = download.download_speed_test(
        n_bytes=1_000,
        proxies={"https": "socks5://127.0.0.1:12345"},
        timeout=8,
    )

    assert speed > 0
    assert latency == 0.046


def test_download_speed_test_raises_on_missing_server_timing_header(monkeypatch):
    """A missing ``Server-Timing`` header must raise ``ValueError``, not ``TypeError``."""

    class Response:
        headers = {}
        elapsed = timedelta(milliseconds=50)
        content = b"x"

    monkeypatch.setattr(download.requests, "get", lambda **kwargs: Response())
    monkeypatch.setattr(download.time, "perf_counter", iter([10.0, 10.1]).__next__)

    with pytest.raises(ValueError, match="Cannot parse CF header"):
        download.download_speed_test(
            n_bytes=1_000,
            proxies={"https": "socks5://127.0.0.1:12345"},
            timeout=8,
        )


def test_download_speed_test_raises_on_empty_server_timing_header(monkeypatch):
    """An empty ``Server-Timing`` header must also raise ``ValueError``."""

    class Response:
        headers = {"Server-Timing": ""}
        elapsed = timedelta(milliseconds=50)
        content = b"x"

    monkeypatch.setattr(download.requests, "get", lambda **kwargs: Response())
    monkeypatch.setattr(download.time, "perf_counter", iter([10.0, 10.1]).__next__)

    with pytest.raises(ValueError, match="Cannot parse CF header"):
        download.download_speed_test(
            n_bytes=1_000,
            proxies={"https": "socks5://127.0.0.1:12345"},
            timeout=8,
        )
