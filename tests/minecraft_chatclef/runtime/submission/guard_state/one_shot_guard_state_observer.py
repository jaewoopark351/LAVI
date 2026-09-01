#20260901_kpopmodder: Observe one-shot guard files once without changing live state.
from __future__ import annotations

import json
from collections.abc import Callable
from pathlib import Path

from .guard_record_parser import (
    parse_live_run_block,
    parse_reconciliation_requirement,
)
from .one_shot_guard_record_identity import OneShotGuardRecordIdentity
from .one_shot_guard_state import OneShotGuardState
from .one_shot_guard_state_observation import OneShotGuardStateObservation
from .unreadable_guard_record_error import UnreadableGuardRecordError


class OneShotGuardStateObserver:
    def __init__(
        self,
        repository_root: str,
        *,
        state_directory: str | Path | None = None,
        record_exists: Callable[[Path], bool] | None = None,
        record_reader: Callable[[Path], object] | None = None,
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
            raise ValueError("guard observation state must stay inside the repository")
        self._record_exists = record_exists or Path.exists
        self._record_reader = record_reader or _read_json_record

    def observe(self) -> OneShotGuardStateObservation:
        block_path = self._state_directory / "live-run-block.json"
        reconciliation_path = (
            self._state_directory / "reconciliation-required.json"
        )
        try:
            block_present = self._exists(block_path)
            reconciliation_present = self._exists(reconciliation_path)
        except Exception as error:
            return self._unreadable(
                f"guard state existence probe failed: {type(error).__name__}"
            )

        if not block_present and not reconciliation_present:
            return self._observation(
                OneShotGuardState.CLEAR,
                reason="no one-shot guard records are present",
            )

        try:
            block = self._read_block(block_path) if block_present else None
            reconciliation = (
                self._read_reconciliation(reconciliation_path)
                if reconciliation_present
                else None
            )
        except UnreadableGuardRecordError as error:
            return self._unreadable(error.reason)

        if block is not None and reconciliation is not None:
            if not _same_identity(block, reconciliation):
                return self._unreadable(
                    "one-shot block and reconciliation identities do not match"
                )

        if reconciliation is not None:
            return self._observation(
                OneShotGuardState.RECONCILIATION_REQUIRED,
                identity=reconciliation,
                reason=reconciliation.reconciliation_reason,
            )
        if block is None:
            return self._unreadable("live-run block observation is missing")
        return self._observation(
            OneShotGuardState.LIVE_RUN_BLOCK_PRESENT,
            identity=block,
            reason="a live-run block is present",
        )

    def _exists(self, path: Path) -> bool:
        result = self._record_exists(path)
        if type(result) is not bool:
            raise TypeError("guard record existence result is not boolean")
        return result

    def _read_block(self, path: Path) -> OneShotGuardRecordIdentity:
        try:
            return parse_live_run_block(self._record_reader(path))
        except Exception as error:
            raise UnreadableGuardRecordError(
                f"{path.name} is unreadable: {type(error).__name__}"
            ) from error

    def _read_reconciliation(self, path: Path) -> OneShotGuardRecordIdentity:
        try:
            return parse_reconciliation_requirement(self._record_reader(path))
        except Exception as error:
            raise UnreadableGuardRecordError(
                f"{path.name} is unreadable: {type(error).__name__}"
            ) from error

    def _unreadable(self, reason: str) -> OneShotGuardStateObservation:
        return self._observation(
            OneShotGuardState.GUARD_STATE_UNREADABLE,
            reason=reason,
        )

    def _observation(
        self,
        state: OneShotGuardState,
        *,
        identity: OneShotGuardRecordIdentity | None = None,
        reason: str,
    ) -> OneShotGuardStateObservation:
        return OneShotGuardStateObservation(
            state=state,
            state_directory=str(self._state_directory),
            invocation_fingerprint=(
                identity.invocation_fingerprint if identity is not None else None
            ),
            command_fingerprint=(
                identity.command_fingerprint if identity is not None else None
            ),
            reason=reason,
        )


def _same_identity(
    left: OneShotGuardRecordIdentity,
    right: OneShotGuardRecordIdentity,
) -> bool:
    return (
        left.invocation_fingerprint == right.invocation_fingerprint
        and left.command_fingerprint == right.command_fingerprint
    )


def _read_json_record(path: Path) -> object:
    return json.loads(path.read_text(encoding="utf-8"))
