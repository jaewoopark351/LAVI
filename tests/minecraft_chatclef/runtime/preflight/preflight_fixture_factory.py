#20260818_kpopmodder: Build deterministic offline fixtures for live preflight tests.
#20260819_kpopmodder: Emit complete process identity fingerprints in preflight fixtures.
from __future__ import annotations

import json

from .windows_listener.process_identity.process_identity_key import (
    process_identity_fingerprint,
)


def live_environment_fixture() -> dict[str, object]:
    environment: dict[str, object] = {
        "live_opt_in": True,
        "mutating_opt_in": True,
        "gradio_url": "http://127.0.0.1:47860",
        "command": "돌 1개 가져와줘",
        "expected_backend": "fabric_chatclef",
        "expected_instance": "LAVI_TEST_Fabric01",
        "expected_world": "전용월드",
        "log_dir": "C:/fixture/Instances/LAVI_TEST_Fabric01/logs",
        "invocation_id": "one-shot-1",
        "timeout_sec": 60.0,
        "poll_sec": 2.0,
        "fabric_port": 4316,
        "gradio_range_start": 47860,
        "gradio_range_end": 47959,
        "repository_root": "C:/Vtuber_Souorce_Code/LAVI",
    }
    environment["approval_json"] = json.dumps(
        {
            "command": environment["command"],
            "gradio_url": environment["gradio_url"],
            "backend": environment["expected_backend"],
            "instance": environment["expected_instance"],
            "world": environment["expected_world"],
            "invocation_id": environment["invocation_id"],
            "approval_source": "explicit_user_approval",
            "one_shot": True,
            "automatic_rerun_disabled": True,
        },
        ensure_ascii=False,
    )
    return environment


def runtime_status_fixture(
    *,
    backend: str = "fabric_chatclef",
    enabled: bool = True,
    connected: bool = True,
    lifecycle_state: str = "connected",
    active_request_id: str | None = None,
    instance: str = "",
    world: str = "",
) -> dict[str, object]:
    return {
        "details": {
            "backend_id": backend,
            "enabled": enabled,
            "connected": connected,
            "lifecycle_state": lifecycle_state,
            "instance": instance,
            "world": world,
            "details": {
                "commands": {
                    "active_request_id": active_request_id,
                    "last_result": None,
                }
            },
        }
    }


def process_result_fixture(
    process_id: int,
    *,
    creation_date: str = "20260818120000.000000+540",
    creation_time_utc_ticks: int = 638911008000000000,
    executable_path: str = (
        "c:\\vtuber_souorce_code\\lavi\\venv\\scripts\\python.exe"
    ),
) -> dict[str, object]:
    observed: dict[str, object] = {
        "intended_lavi_pid": process_id,
        "intended_lavi_parent_process_id": 0,
        "listener_pid_by_port": {"47860": process_id, "4316": process_id},
        "process_entrypoint": "main.py",
        "process_invocation_mode": "python_script",
        "resolved_entrypoint_path": "c:\\vtuber_souorce_code\\lavi\\main.py",
        "entrypoint_provenance": "exact_repository_script",
        "repository_root": "c:\\vtuber_souorce_code\\lavi",
        "approved_ancestor": None,
        "intended_lavi_creation_date": creation_date,
        "intended_lavi_creation_time_utc_ticks": creation_time_utc_ticks,
        "intended_lavi_executable_path": executable_path,
    }
    observed["process_identity_fingerprint"] = process_identity_fingerprint(observed)
    return {
        "ok": True,
        "reason": "listener_process_identity_validated",
        "observed": observed,
    }


def log_identity_result_fixture(*_args, **_kwargs) -> dict[str, object]:
    return {
        "ok": True,
        "reason": "minecraft_log_identity_validated",
        "observed": {
            "instance": "LAVI_TEST_Fabric01",
            "world": "전용월드",
            "identity_source": "minecraft_latest_log",
            "log_encoding": "cp949",
            "log_snapshot_stable": True,
        },
    }


def log_snapshot_fixture(
    text: str,
    *,
    final_line_complete: bool = True,
) -> dict[str, object]:
    return {
        "ok": True,
        "reason": "stable_log_snapshot",
        "text": text,
        "encoding": "utf-8",
        "size": len(text.encode("utf-8")),
        "age_sec": 0.0,
        "final_line_complete": final_line_complete,
    }


def listener_payload_fixture() -> dict[str, list[dict[str, object]]]:
    return {
        "listeners": [
            {
                "local_address": "127.0.0.1",
                "local_port": 47860,
                "process_id": 4100,
            },
            {
                "local_address": "127.0.0.1",
                "local_port": 4316,
                "process_id": 4100,
            },
            {
                "local_address": "127.0.0.1",
                "local_port": 47861,
                "process_id": 9999,
            },
        ],
        "processes": [
            {
                "process_id": 4100,
                "parent_process_id": 0,
                "name": "python.exe",
                "creation_date": "20260818120000.000000+540",
                "creation_time_utc_ticks": 638911008000000000,
                "executable_path": "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe",
                "command_line": "python.exe C:/Vtuber_Souorce_Code/LAVI/main.py",
            },
            {
                "process_id": 9999,
                "parent_process_id": 0,
                "name": "unrelated.exe",
                "creation_date": "20260818115900.000000+540",
                "creation_time_utc_ticks": 638911007400000000,
                "executable_path": "C:/Other/unrelated.exe",
                "command_line": "C:/Other/unrelated.exe",
            },
        ],
    }
