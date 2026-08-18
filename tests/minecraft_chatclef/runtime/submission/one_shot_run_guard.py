#20260818_kpopmodder: Persist one-shot claims and unresolved live-run reconciliation safely.
from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from pathlib import Path

from .invocation_fingerprint import invocation_fingerprint
from .one_shot_invocation_ledger import OneShotInvocationLedger


class OneShotRunGuard:
    def __init__(
        self,
        repository_root: str,
        *,
        state_directory: str | Path | None = None,
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
            raise ValueError("live-run guard state must stay inside the repository")
        self._invocation_ledger = invocation_ledger or OneShotInvocationLedger(
            str(self._repository_root),
            state_directory=self._state_directory,
        )

    def claim(
        self,
        invocation_id: str,
        command_fingerprint: str,
    ) -> dict[str, object]:
        unresolved = self._state_directory / "reconciliation-required.json"
        if unresolved.exists():
            return _result(
                False,
                "a previous live run still requires operator reconciliation; "
                f"guard={self._state_directory}",
            )
        ledger_status = self._invocation_ledger.status(invocation_id)
        if ledger_status.get("ok") is not True:
            return _result(
                False,
                str(ledger_status.get("reason") or "one-shot ledger is unreadable"),
            )
        if ledger_status.get("consumed") is True:
            return _result(
                False,
                "this one-shot invocation was already completed; use a fresh "
                "approved invocation ID",
            )
        invocation_hash = invocation_fingerprint(invocation_id)
        claim_path = self._state_directory / "live-run-block.json"
        payload = {
            "invocation_fingerprint": invocation_hash,
            "command_fingerprint": command_fingerprint,
            "claimed_at_utc": _utc_now(),
        }
        if not self._write_exclusive(claim_path, payload):
            return _result(
                False,
                "a previous one-shot run must be reconciled before another mutating run; "
                f"guard={self._state_directory}",
            )
        return _result(True, "one-shot invocation claimed")

    def _write_exclusive(
        self,
        path: Path,
        payload: dict[str, object],
    ) -> bool:
        self._state_directory.mkdir(parents=True, exist_ok=True)
        if not self._state_directory.resolve().is_relative_to(self._repository_root):
            raise ValueError("live-run guard state escaped the repository")
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
        try:
            descriptor = os.open(path, flags, 0o600)
        except FileExistsError:
            return False
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            json.dump(payload, stream, ensure_ascii=True, sort_keys=True)
            stream.write("\n")
        return True


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _result(ok: bool, reason: str) -> dict[str, object]:
    return {"ok": ok, "reason": reason}
