#20260818_kpopmodder: Clear only the matching one-shot state after verified reconciliation.
from __future__ import annotations

import json
from collections.abc import Callable, Mapping
from pathlib import Path

from .invocation_fingerprint import invocation_fingerprint
from .one_shot_invocation_ledger import OneShotInvocationLedger


class OneShotRunReconciler:
    def __init__(
        self,
        repository_root: str,
        *,
        state_directory: str | Path | None = None,
        record_exists: Callable[[Path], bool] | None = None,
        record_reader: Callable[[Path], object] | None = None,
        record_deleter: Callable[[Path], None] | None = None,
        invocation_ledger: object | None = None,
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
            raise ValueError("live-run reconciliation state must stay inside the repository")
        self._record_exists = record_exists or Path.exists
        self._record_reader = record_reader or _read_json_record
        self._record_deleter = record_deleter or Path.unlink
        self._invocation_ledger = invocation_ledger or OneShotInvocationLedger(
            str(self._repository_root),
            state_directory=self._state_directory,
        )

    def reconcile(
        self,
        invocation_id: str,
        expected_command_fingerprint: str,
    ) -> dict[str, object]:
        expected = invocation_fingerprint(invocation_id)
        command_expected = str(expected_command_fingerprint or "").strip()
        if not command_expected:
            return _result(False, "expected command fingerprint is missing")
        block = self._state_directory / "live-run-block.json"
        marker = self._state_directory / "reconciliation-required.json"
        if not self._record_exists(block):
            return _result(False, "matching one-shot block is missing")
        block_error = _record_error(
            self._record_reader(block),
            expected,
            command_expected,
        )
        if block_error:
            return _result(False, f"one-shot block {block_error}")
        marker_exists = self._record_exists(marker)
        if marker_exists:
            marker_error = _record_error(
                self._record_reader(marker),
                expected,
                command_expected,
            )
            if marker_error:
                return _result(False, f"reconciliation marker {marker_error}")
        consumed = self._invocation_ledger.mark_consumed(invocation_id)
        if consumed.get("ok") is not True or consumed.get("consumed") is not True:
            return _result(
                False,
                str(consumed.get("reason") or "invocation consumption failed"),
            )
        try:
            if marker_exists:
                self._record_deleter(marker)
            self._record_deleter(block)
        except OSError as error:
            return _result(
                False,
                f"guard record delete failed: {type(error).__name__}: {error}",
            )
        return _result(True, "matching one-shot guard reconciled")


def _read_json_record(path: Path) -> object:
    return json.loads(path.read_text(encoding="utf-8"))


def _record_error(
    record: object,
    expected_fingerprint: str,
    expected_command_fingerprint: str,
) -> str:
    if not isinstance(record, Mapping):
        return "record is invalid"
    observed = str(record.get("invocation_fingerprint") or "").strip()
    if observed != expected_fingerprint:
        return "invocation does not match"
    observed_command = str(record.get("command_fingerprint") or "").strip()
    if observed_command != expected_command_fingerprint:
        return "command does not match"
    return ""


def _result(ok: bool, reason: str) -> dict[str, object]:
    return {"ok": ok, "reason": reason}
