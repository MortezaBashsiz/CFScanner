import pytest

from cfscanner.utils import os as cf_os


def test_detect_system_unsupported_system_message_is_interpolated(monkeypatch):
    """The error message must contain the actual detected system name.

    Regression test for a missing ``f`` prefix that left the message as the
    literal string ``"Unsupported system: {current_system}"``.
    """
    monkeypatch.setattr(cf_os.platform, "system", lambda: "PlanNine")
    monkeypatch.setattr(cf_os.platform, "machine", lambda: "unknown-arch")

    with pytest.raises(OSError, match="planNine".lower()) as exc_info:
        cf_os.detect_system()

    assert "{current_system}" not in str(exc_info.value)


def test_detect_system_known_systems(monkeypatch):
    monkeypatch.setattr(cf_os.platform, "system", lambda: "Linux")
    monkeypatch.setattr(cf_os.platform, "machine", lambda: "x86_64")
    monkeypatch.delattr(cf_os.sys, "getandroidapilevel", raising=False)

    assert cf_os.detect_system() == ("linux", "64")


def test_create_dir_creates_missing_directory(tmp_path):
    target = tmp_path / "nested" / "dir"
    assert not target.exists()

    cf_os.create_dir(str(target))

    assert target.is_dir()


def test_create_dir_is_noop_when_dir_exists(tmp_path):
    target = tmp_path / "already-there"
    target.mkdir()

    cf_os.create_dir(str(target))  # should not raise

    assert target.is_dir()


def test_get_n_lines_counts_nonblank_lines(tmp_path):
    f = tmp_path / "lines.txt"
    f.write_text("a\n\nb\nc\n\n")

    assert cf_os.get_n_lines(str(f)) == 3
