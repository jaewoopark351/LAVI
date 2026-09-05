#20260905_kpopmodder: Own one ordinary Fabric ChatClef command and its reconciliation lifecycle.
from __future__ import annotations

from dataclasses import replace
from typing import Any, Callable, Mapping

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO

from ..fabric_chatclef_active_command import FabricChatClefActiveCommand
from ..reconciliation.active_command_reconciliation_coordinator import (
    ActiveCommandReconciliationCoordinator,
)
from ..reconciliation.active_command_reconciliation_outcome import (
    ActiveCommandReconciliationOutcome,
)
from ..reconciliation.command_reconciliation_state import CommandReconciliationState


class FabricChatClefOrdinaryCommandOwner:
    def __init__(
        self,
        *,
        reconciliation: ActiveCommandReconciliationCoordinator,
        now_ms: Callable[[], int],
    ) -> None:
        self._state = CommandReconciliationState()
        self._reconciliation = reconciliation
        self._now_ms = now_ms

    @property
    def state(self) -> CommandReconciliationState:
        return self._state

    @property
    def active_request_id(self) -> str | None:
        command = self._state.active_command
        return None if command is None else command.request_id

    @property
    def active_command(self) -> FabricChatClefActiveCommand | None:
        return self._state.active_command

    @property
    def reconciliation(self):
        return self._reconciliation

    @reconciliation.setter
    def reconciliation(self, value) -> None:
        self._reconciliation = value

    def clear(self) -> None:
        self._state = self._state.without_active()

    def begin(
        self,
        *,
        websocket: Any,
        session_id: str | None,
        generation: int,
        request_id: str,
        command_message_id: str,
        command: str,
        source: str,
    ) -> FabricChatClefActiveCommand | None:
        if websocket is None or session_id is None:
            return None
        if self._state.active_command is not None:
            return None
        if self._state.quarantine.active:
            return None
        active_command = FabricChatClefActiveCommand(
            websocket=websocket,
            session_id=session_id,
            generation=generation,
            request_id=request_id,
            command_message_id=command_message_id,
            command=str(command or ""),
            source=str(source or ""),
            started_at_ms=self._now_ms(),
        )
        self._state = self._state.with_active(active_command)
        return active_command

    def clear_if_current(self, command: FabricChatClefActiveCommand) -> bool:
        if self._state.active_command is not command:
            return False
        self._state = self._state.without_active()
        return True

    def clear_if_identity(
        self,
        *,
        request_id: str,
        command_message_id: str,
        session_id: str,
        server_connection_generation: int,
        owner_token: object,
    ) -> bool:
        command = self._state.active_command
        if command is None or command is not owner_token:
            return False
        if (
            command.request_id != request_id
            or command.command_message_id != command_message_id
            or command.session_id != session_id
            or command.generation != server_connection_generation
        ):
            return False
        self._state = self._state.without_active()
        return True

    def accept_result_and_reconcile(
        self,
        *,
        websocket: Any,
        active_websocket: Any,
        connection_generation: int,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
        raw_payload: Mapping[str, Any],
        before_snapshot: dict[str, Any],
        after_snapshot_provider: Callable[[], dict[str, Any]],
    ) -> ActiveCommandReconciliationOutcome:
        command = self._state.active_command
        if active_websocket is not websocket:
            return self._rejected_outcome(
                reason="result_websocket_not_active",
                before_snapshot=before_snapshot,
                after_snapshot_provider=after_snapshot_provider,
            )
        if command is None:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_without_active_command",
                after_snapshot_provider=after_snapshot_provider,
            )
        if command.generation != connection_generation:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_generation_mismatch",
                after_snapshot_provider=after_snapshot_provider,
            )
        if envelope.session_id != command.session_id:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_session_mismatch",
                after_snapshot_provider=after_snapshot_provider,
            )
        if result.request_id != command.request_id:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_request_mismatch",
                after_snapshot_provider=after_snapshot_provider,
            )
        if envelope.correlation_id != command.command_message_id:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_correlation_mismatch",
                after_snapshot_provider=after_snapshot_provider,
            )

        next_state, outcome = self._reconciliation.accept_and_apply(
            state=self._state,
            expected_active=command,
            envelope=envelope,
            result=result,
            raw_payload=raw_payload,
            before_snapshot=before_snapshot,
            monotonic_ms=self._now_ms(),
        )
        if self._state.active_command is not command:
            return self._rejected_outcome(
                reason="result_active_cas_failed",
                before_snapshot=before_snapshot,
                after_snapshot_provider=after_snapshot_provider,
            )
        self._state = next_state
        return replace(outcome, after_snapshot=after_snapshot_provider())

    def _audit_late_or_reject(
        self,
        *,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
        before_snapshot: dict[str, Any],
        fallback_reason: str,
        after_snapshot_provider: Callable[[], dict[str, Any]],
    ) -> ActiveCommandReconciliationOutcome:
        next_state, late_outcome = self._reconciliation.audit_late_result(
            state=self._state,
            envelope=envelope,
            result=result,
            before_snapshot=before_snapshot,
            monotonic_ms=self._now_ms(),
        )
        if late_outcome is None:
            return self._rejected_outcome(
                reason=fallback_reason,
                before_snapshot=before_snapshot,
                after_snapshot_provider=after_snapshot_provider,
            )
        self._state = next_state
        return replace(late_outcome, after_snapshot=after_snapshot_provider())

    def _rejected_outcome(
        self,
        *,
        reason: str,
        before_snapshot: dict[str, Any],
        after_snapshot_provider: Callable[[], dict[str, Any]],
    ) -> ActiveCommandReconciliationOutcome:
        return ActiveCommandReconciliationOutcome(
            accepted=False,
            reason=reason,
            before_snapshot=before_snapshot,
            after_snapshot=after_snapshot_provider(),
            audit={
                "event": "command_result_rejected",
                "reason": reason,
                "active_release_performed": False,
            },
        )
