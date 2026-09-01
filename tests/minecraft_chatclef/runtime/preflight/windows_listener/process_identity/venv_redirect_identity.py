#20260901_kpopmodder: Bind the Windows venv launcher process that redirects to the base Python runtime.
from __future__ import annotations

import ntpath
from typing import Mapping

from .entrypoint_provenance import normalize_windows_path
from .process_evidence import process_evidence_error
from .process_start_time import (
    process_started_no_later_than,
    process_start_time_utc_ticks,
)
from .windows_command_line import parse_windows_command_line


def inspect_repository_venv_redirect(
    owner: Mapping[str, object],
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
) -> dict[str, object]:
    owner_arguments = parse_windows_command_line(owner.get("command_line"))
    if len(owner_arguments) != 2:
        return _failure("redirected Python owner command line is not exact")
    expected_interpreter = normalize_windows_path(
        ntpath.join(
            repository_root,
            "venv",
            "Scripts",
            "python.exe",
        )
    )
    expected_entrypoint = normalize_windows_path(
        ntpath.join(repository_root, "main.py")
    )
    if normalize_windows_path(owner_arguments[0]) != expected_interpreter:
        return _failure("redirected Python argv0 is not the repository venv")
    if normalize_windows_path(owner_arguments[1]) != expected_entrypoint:
        return _failure("redirected Python script is not the repository entrypoint")

    parent_process_id = owner.get("parent_process_id")
    if type(parent_process_id) is not int or parent_process_id <= 0:
        return _failure("redirected Python parent process ID is invalid")
    proxy = processes.get(parent_process_id)
    if proxy is None:
        return _failure("redirected Python parent evidence is missing")
    evidence_error = process_evidence_error(
        proxy,
        expected_process_id=parent_process_id,
    )
    if evidence_error:
        return _failure(
            f"redirected Python parent evidence is incomplete: {evidence_error}"
        )
    if normalize_windows_path(proxy.get("executable_path")) != expected_interpreter:
        return _failure("redirected Python parent is not the repository venv")
    proxy_arguments = parse_windows_command_line(proxy.get("command_line"))
    if len(proxy_arguments) != 2:
        return _failure("redirected Python parent command line is not exact")
    if normalize_windows_path(proxy_arguments[0]) != expected_interpreter:
        return _failure("redirected Python parent argv0 is not the repository venv")
    if normalize_windows_path(proxy_arguments[1]) != expected_entrypoint:
        return _failure("redirected Python parent script is not the repository entrypoint")
    if not process_started_no_later_than(proxy, owner):
        return _failure("redirected Python parent started after the listener owner")

    creation_date = _required_text(proxy.get("creation_date"))
    creation_time_utc_ticks = process_start_time_utc_ticks(proxy)
    parent_of_proxy = proxy.get("parent_process_id")
    if (
        not creation_date
        or creation_time_utc_ticks is None
        or type(parent_of_proxy) is not int
        or parent_of_proxy < 0
    ):
        return _failure("redirected Python parent identity is incomplete")
    return {
        "ok": True,
        "reason": "exact repository venv redirect",
        "invocation": {
            "ok": True,
            "mode": "python_script",
            "operand": expected_entrypoint,
            "executable_name": "python.exe",
            "strict_script_form": True,
        },
        "observed": {
            "process_id": parent_process_id,
            "parent_process_id": parent_of_proxy,
            "creation_date": creation_date,
            "creation_time_utc_ticks": creation_time_utc_ticks,
            "executable_path": expected_interpreter,
            "resolved_entrypoint_path": expected_entrypoint,
            "entrypoint_provenance": "exact_repository_venv_redirect",
        },
    }


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _failure(reason: str) -> dict[str, object]:
    return {
        "ok": False,
        "reason": reason,
        "invocation": {},
        "observed": {},
    }
