#20260818_kpopmodder: Record only the unresolved evidence marker for one live run.
from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from pathlib import Path

from .invocation_fingerprint import invocation_fingerprint


class ReconciliationRequirementRecorder:
    def __init__(
        self,
        repository_root: str,
        *,
        state_directory: str | Path | None = None,
    ):
        self._repository_root = Path(repository_root).resolve()
        candidate = (
            Path(state_directory)
            if state_directory is not None
            else self._repository_root
            / "tests"
            / "tmp_minecraft_chatclef_live_state"
        )
        self._state_directory = candidate.resolve()
        if not self._state_directory.is_relative_to(self._repository_root):
            raise ValueError("reconciliation state must stay inside the repository")

    def record(
        self,
        invocation_id: str,
        command_fingerprint: str,
        reason: str,
    ) -> dict[str, object]:
        command_hash = str(command_fingerprint or "").strip()
        if not command_hash:
            return {"ok": False, "reason": "command fingerprint is missing"}
        marker = self._state_directory / "reconciliation-required.json"
        payload = {
            "invocation_fingerprint": invocation_fingerprint(invocation_id),
            "command_fingerprint": command_hash,
            "reason": str(reason or "live_run_reconciliation_required")[:120],
            "recorded_at_utc": datetime.now(timezone.utc).isoformat(),
        }
        if self._write_exclusive(marker, payload):
            return {"ok": True, "reason": "reconciliation requirement recorded"}
        return {"ok": False, "reason": "reconciliation marker already exists"}

    def _write_exclusive(
        self,
        path: Path,
        payload: dict[str, object],
    ) -> bool:
        self._state_directory.mkdir(parents=True, exist_ok=True)
        if not self._state_directory.resolve().is_relative_to(self._repository_root):
            raise ValueError("reconciliation state escaped the repository")
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
        try:
            descriptor = os.open(path, flags, 0o600)
        except FileExistsError:
            return False
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            json.dump(payload, stream, ensure_ascii=True, sort_keys=True)
            stream.write("\n")
        return True
