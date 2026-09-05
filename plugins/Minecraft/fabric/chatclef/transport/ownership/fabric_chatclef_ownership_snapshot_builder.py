#20260905_kpopmodder: Project Fabric ChatClef connection ownership into bounded snapshots.
from __future__ import annotations

from typing import Any, Callable


class FabricChatClefOwnershipSnapshotBuilder:
    def __init__(self, *, connection_session, command_owner, now_ms: Callable[[], int]):
        self._connection_session = connection_session
        self._command_owner = command_owner
        self._now_ms = now_ms

    def wire_snapshot(self) -> dict[str, Any]:
        return self._build(include_local=False, include_audit=False)

    def local_admission_snapshot(self) -> dict[str, Any]:
        return self._build(include_local=True, include_audit=False)

    def audit_snapshot(self) -> dict[str, Any]:
        return self._build(include_local=True, include_audit=True)

    def _build(self, *, include_local: bool, include_audit: bool) -> dict[str, Any]:
        state = self._command_owner.state
        command = state.active_command
        active_age_ms = (
            None
            if command is None or command.started_at_ms <= 0
            else max(0, self._now_ms() - command.started_at_ms)
        )
        snapshot = {
            "active_session_id": self._connection_session.active_session_id,
            "active_generation": self._connection_session.active_generation,
            "active_request_id": None if command is None else command.request_id,
            "active_command_message_id": (
                None if command is None else command.command_message_id
            ),
            "active_command": None if command is None else command.command,
            "active_command_source": None if command is None else command.source,
            "active_started_at_ms": None if command is None else command.started_at_ms,
            "active_age_ms": active_age_ms,
            "last_result": state.last_java_result,
        }
        if include_local:
            snapshot.update(
                {
                    "last_java_result": state.last_java_result,
                    "local_effective_result": state.local_effective_result,
                    "admission_quarantine": state.quarantine.to_dict(),
                    "reconciliation_feature_gate": (
                        self._command_owner.reconciliation.feature_gate.to_dict()
                    ),
                }
            )
        if include_audit:
            snapshot.update(
                {
                    "candidate": (
                        None
                        if state.candidate is None
                        else {
                            "first_sequence": state.candidate.first_sequence,
                            "first_message_id": state.candidate.first_message_id,
                        }
                    ),
                    "tombstones": state.tombstones.to_list(self._now_ms()),
                }
            )
        return snapshot
