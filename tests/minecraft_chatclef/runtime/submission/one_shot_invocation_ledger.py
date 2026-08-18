#20260818_kpopmodder: Persist consumed invocation fingerprints to block test reruns.
from __future__ import annotations

import json
import os
from collections.abc import Callable, Mapping
from datetime import datetime, timezone
from pathlib import Path

from .invocation_fingerprint import invocation_fingerprint


class OneShotInvocationLedger:
    def __init__(
        self,
        repository_root: str,
        *,
        state_directory: str | Path | None = None,
        record_exists: Callable[[Path], bool] | None = None,
        record_reader: Callable[[Path], object] | None = None,
        record_writer: Callable[[Path, dict[str, object]], bool] | None = None,
    ):
        repository = Path(repository_root).resolve()
        candidate = (
            Path(state_directory)
            if state_directory is not None
            else repository / "tests" / "tmp_minecraft_chatclef_live_state"
        )
        state_root = candidate.resolve()
        if not state_root.is_relative_to(repository):
            raise ValueError("one-shot ledger state must stay inside the repository")
        self._ledger_directory = state_root / "completed-invocations"
        self._record_exists = record_exists or Path.exists
        self._record_reader = record_reader or _read_json_record
        self._record_writer = record_writer or self._write_exclusive

    def status(self, invocation_id: str) -> dict[str, object]:
        expected = invocation_fingerprint(invocation_id)
        path = self._record_path(expected)
        if not self._record_exists(path):
            return {"ok": True, "consumed": False, "reason": "invocation is fresh"}
        record_error = _record_error(self._record_reader(path), expected)
        if record_error:
            return {"ok": False, "consumed": True, "reason": record_error}
        return {"ok": True, "consumed": True, "reason": "invocation is consumed"}

    def mark_consumed(self, invocation_id: str) -> dict[str, object]:
        expected = invocation_fingerprint(invocation_id)
        path = self._record_path(expected)
        existing = self.status(invocation_id)
        if existing.get("consumed") is True:
            return existing
        if existing.get("ok") is not True:
            return existing
        payload = {
            "invocation_fingerprint": expected,
            "completed_at_utc": datetime.now(timezone.utc).isoformat(),
        }
        if self._record_writer(path, payload):
            return {"ok": True, "consumed": True, "reason": "invocation recorded"}
        return self.status(invocation_id)

    def _record_path(self, fingerprint: str) -> Path:
        return self._ledger_directory / f"{fingerprint}.json"

    def _write_exclusive(
        self,
        path: Path,
        payload: dict[str, object],
    ) -> bool:
        self._ledger_directory.mkdir(parents=True, exist_ok=True)
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
        try:
            descriptor = os.open(path, flags, 0o600)
        except FileExistsError:
            return False
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            json.dump(payload, stream, ensure_ascii=True, sort_keys=True)
            stream.write("\n")
        return True


def _read_json_record(path: Path) -> object:
    return json.loads(path.read_text(encoding="utf-8"))


def _record_error(record: object, expected_fingerprint: str) -> str:
    if not isinstance(record, Mapping):
        return "consumed invocation record is invalid"
    observed = str(record.get("invocation_fingerprint") or "").strip()
    if observed != expected_fingerprint:
        return "consumed invocation fingerprint does not match"
    return ""
