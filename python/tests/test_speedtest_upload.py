import pytest

from cfscanner.speedtest import upload


def test_upload_speed_accepts_integer_server_timing_duration(monkeypatch):
    """Cloudflare emits integer ``dur`` values, not only decimal ones."""

    class Response:
        headers = {
            "Server-Timing": (
                'cfSpeedEdge;dur=4, cfSpeedWorker;dur=28, '
                'cfL4;desc="?proto=TCP&rtt=0"'
            )
        }

    monkeypatch.setattr(upload.requests, "post", lambda **kwargs: Response())
    monkeypatch.setattr(upload.time, "perf_counter", iter([10.0, 10.1]).__next__)

    speed, latency = upload.upload_speed_test(
        n_bytes=1_000,
        proxies={"https": "socks5://127.0.0.1:12345"},
        timeout=8,
    )

    assert speed > 0
    assert latency == pytest.approx(0.096)


def test_upload_speed_test_raises_on_missing_server_timing_header(monkeypatch):
    """A missing ``Server-Timing`` header must raise ``ValueError``, not ``TypeError``."""

    class Response:
        headers = {}

    monkeypatch.setattr(upload.requests, "post", lambda **kwargs: Response())
    monkeypatch.setattr(upload.time, "perf_counter", iter([10.0, 10.1]).__next__)

    with pytest.raises(ValueError, match="Cannot parse CF header"):
        upload.upload_speed_test(
            n_bytes=1_000,
            proxies={"https": "socks5://127.0.0.1:12345"},
            timeout=8,
        )


def test_upload_speed_test_raises_on_empty_server_timing_header(monkeypatch):
    """An empty ``Server-Timing`` header must also raise ``ValueError``."""

    class Response:
        headers = {"Server-Timing": ""}

    monkeypatch.setattr(upload.requests, "post", lambda **kwargs: Response())
    monkeypatch.setattr(upload.time, "perf_counter", iter([10.0, 10.1]).__next__)

    with pytest.raises(ValueError, match="Cannot parse CF header"):
        upload.upload_speed_test(
            n_bytes=1_000,
            proxies={"https": "socks5://127.0.0.1:12345"},
            timeout=8,
        )
