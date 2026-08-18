#20260818_kpopmodder: Read only targeted Windows listener and ancestor-process evidence.
from __future__ import annotations

import json
import os
import subprocess
from typing import Callable, Mapping


def read_windows_listener_probe(
    ports: list[int],
    *,
    command_runner: Callable[..., subprocess.CompletedProcess[str]] = subprocess.run,
) -> dict[str, object]:
    if os.name != "nt":
        return _failure("Windows listener identity probe requires Windows")
    script = _powershell_probe_script(sorted(set(ports)))
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
            text=True,
            encoding="utf-8",
            errors="strict",
            timeout=15,
            check=False,
            creationflags=creation_flags,
        )
    except (OSError, subprocess.SubprocessError, UnicodeError) as error:
        return _failure(f"listener probe failed: {type(error).__name__}: {error}")
    if completed.returncode != 0:
        reason = (completed.stderr or completed.stdout or "probe failed").strip()
        return _failure(f"listener probe returned {completed.returncode}: {reason[:300]}")
    try:
        payload = json.loads(completed.stdout)
    except (TypeError, ValueError, json.JSONDecodeError) as error:
        return _failure(f"listener probe returned malformed JSON: {type(error).__name__}")
    if not isinstance(payload, Mapping):
        return _failure("listener probe result must be an object")
    return {"ok": True, "reason": "listener_probe_read", "payload": dict(payload)}


def _powershell_probe_script(ports: list[int]) -> str:
    port_values = ",".join(str(port) for port in ports)
    return (
        "$ErrorActionPreference='Stop';"
        f"$targetPorts=@({port_values});"
        "$listeners=@(Get-NetTCPConnection -State Listen -ErrorAction Stop | "
        "Where-Object { $targetPorts -contains [int]$_.LocalPort } | "
        "ForEach-Object { [pscustomobject]@{local_address=$_.LocalAddress;"
        "local_port=[int]$_.LocalPort;process_id=[int]$_.OwningProcess} });"
        "$queue=New-Object System.Collections.Generic.Queue[int];"
        "$seen=@{};"
        "foreach($item in $listeners){$queue.Enqueue([int]$item.process_id)};"
        "$processes=@();$depth=0;"
        "while($queue.Count -gt 0 -and $depth -lt 1000){"
        "$processId=$queue.Dequeue();$depth++;"
        "if($processId -le 0 -or $seen.ContainsKey($processId)){continue};"
        "$seen[$processId]=$true;"
        "$process=Get-CimInstance Win32_Process -Filter (\"ProcessId = $processId\") "
        "-ErrorAction Stop;"
        "if($null -eq $process){continue};"
        "$processes+=[pscustomobject]@{process_id=[int]$process.ProcessId;"
        "parent_process_id=[int]$process.ParentProcessId;name=$process.Name;"
        "creation_date=$process.CreationDate;executable_path=$process.ExecutablePath;"
        "command_line=$process.CommandLine};"
        "if([int]$process.ParentProcessId -gt 0){"
        "$queue.Enqueue([int]$process.ParentProcessId)}};"
        "[pscustomobject]@{listeners=$listeners;processes=$processes} | "
        "ConvertTo-Json -Compress -Depth 5"
    )


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "payload": {}}
