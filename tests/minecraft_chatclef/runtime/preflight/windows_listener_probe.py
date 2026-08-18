#20260818_kpopmodder: Read only targeted Windows listener and ancestor-process evidence.
from __future__ import annotations

import json
import os
import subprocess
from typing import Callable, Mapping

from .windows_listener_probe_output import decode_windows_listener_probe_output
from .windows_listener_probe_script import build_windows_listener_probe_script


def read_windows_listener_probe(
    ports: list[int],
    *,
    command_runner: Callable[..., subprocess.CompletedProcess[bytes]] = subprocess.run,
    platform_name: str = os.name,
) -> dict[str, object]:
    if platform_name != "nt":
        return _failure("Windows listener identity probe requires Windows")
    script = build_windows_listener_probe_script(ports)
    creation_flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    try:
        completed = command_runner(
            [
                "powershell.exe",
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script,
            ],
            capture_output=True,
            text=False,
            timeout=15,
            check=False,
            creationflags=creation_flags,
        )
    except (OSError, subprocess.SubprocessError) as error:
        return _failure(f"listener probe failed: {type(error).__name__}: {error}")
    if completed.returncode != 0:
        try:
            reason = decode_windows_listener_probe_output(
                completed.stderr or completed.stdout or b"probe failed",
                stream_name="error",
            ).strip()
        except UnicodeError as error:
            return _failure(str(error))
        return _failure(f"listener probe returned {completed.returncode}: {reason[:300]}")
    try:
        output = decode_windows_listener_probe_output(
            completed.stdout,
            stream_name="stdout",
        )
        payload = json.loads(output)
    except UnicodeError as error:
        return _failure(str(error))
    except (TypeError, ValueError, json.JSONDecodeError) as error:
        return _failure(f"listener probe returned malformed JSON: {type(error).__name__}")
    if not isinstance(payload, Mapping):
        return _failure("listener probe result must be an object")
    return {"ok": True, "reason": "listener_probe_read", "payload": dict(payload)}

def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "payload": {}}
