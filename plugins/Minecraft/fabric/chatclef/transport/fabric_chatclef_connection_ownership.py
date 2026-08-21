#20260801_kpopmodder: Keep Fabric ChatClef websocket/request ownership out of the server I/O loop.
from __future__ import annotations

from dataclasses import replace
import time
from typing import Any, Mapping

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_admission import (
    FabricChatClefConnectionAdmission,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.active_command_reconciliation_coordinator import (
    ActiveCommandReconciliationCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.active_command_reconciliation_outcome import (
    ActiveCommandReconciliationOutcome,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.command_reconciliation_state import (
    CommandReconciliationState,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.reconciliation_feature_gate import (
    ReconciliationFeatureGate,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.reconciliation_runtime_capability import (
    ReconciliationRuntimeCapability,
)


class FabricChatClefConnectionOwnership:
    TERMINAL_STATUSES = {
        CommandResultStatus.COMPLETED,
        CommandResultStatus.REJECTED,
        CommandResultStatus.FAILED,
        CommandResultStatus.CANCELLED,
        CommandResultStatus.DEADLINE_EXCEEDED,
        CommandResultStatus.UNKNOWN,
    }

    def __init__(
        self,
        *,
        reconcile_stale_deposit_to_unknown_requested: bool = False,
        reconciliation_runtime_capability: (
            ReconciliationRuntimeCapability | None
        ) = None,
    ):
        self._generation = 0
        self._active_websocket: Any = None
        self._active_session_id: str | None = None
        self._command_state = CommandReconciliationState()
        self._reconciliation = ActiveCommandReconciliationCoordinator(
            feature_gate=ReconciliationFeatureGate(
                requested_enabled=reconcile_stale_deposit_to_unknown_requested,
                runtime_capability=(
                    reconciliation_runtime_capability
                    or ReconciliationRuntimeCapability.production_blocked()
                ),
            ),
        )

    @property
    def active_websocket(self) -> Any:
        return self._active_websocket

    @property
    def active_session_id(self) -> str | None:
        return self._active_session_id

    @property
    def active_generation(self) -> int:
        return self._generation if self._active_websocket is not None else 0

    @property
    def active_request_id(self) -> str | None:
        command = self._command_state.active_command
        return None if command is None else command.request_id

    def is_connected(self) -> bool:
        return self._active_websocket is not None and self._active_session_id is not None

    def is_active_websocket(self, websocket: Any) -> bool:
        return self._active_websocket is websocket

    def try_activate(
        self,
        *,
        websocket: Any,
        session_id: str,
    ) -> FabricChatClefConnectionAdmission:
        if self._active_websocket is not None:
            if (
                self._active_websocket is websocket
                and self._active_session_id == session_id
            ):
                return FabricChatClefConnectionAdmission(
                    accepted=True,
                    session_id=session_id,
                    generation=self._generation,
                )
            return FabricChatClefConnectionAdmission(
                accepted=False,
                session_id=session_id,
                generation=self.active_generation,
                reason=(
                    "Fabric ChatClef bridge already has an active Java websocket "
                    f"session: {self._active_session_id}"
                ),
            )

        self._generation += 1
        self._active_websocket = websocket
        self._active_session_id = session_id
        self._command_state = self._command_state.without_active()
        return FabricChatClefConnectionAdmission(
            accepted=True,
            session_id=session_id,
            generation=self._generation,
        )

    def clear_if_active(self, *, websocket: Any, session_id: str | None) -> bool:
        if self._active_websocket is not websocket:
            return False
        if self._active_session_id != session_id:
            return False
        self.clear()
        return True

    def clear(self) -> None:
        self._active_websocket = None
        self._active_session_id = None
        self._command_state = self._command_state.without_active()

    def begin_command(
        self,
        *,
        request_id: str,
        command_message_id: str,
        command: str = "",
        source: str = "",
    ) -> FabricChatClefActiveCommand | None:
        if self._active_websocket is None or self._active_session_id is None:
            return None
        if self._command_state.active_command is not None:
            return None
        if self._command_state.quarantine.active:
            return None
        command = FabricChatClefActiveCommand(
            websocket=self._active_websocket,
            session_id=self._active_session_id,
            generation=self._generation,
            request_id=request_id,
            command_message_id=command_message_id,
            command=str(command or ""),
            source=str(source or ""),
            started_at_ms=self._now_ms(),
        )
        self._command_state = self._command_state.with_active(command)
        return command

    def clear_command_if_current(self, command: FabricChatClefActiveCommand) -> bool:
        if self._command_state.active_command is not command:
            return False
        self._command_state = self._command_state.without_active()
        return True

    def accept_result(
        self,
        *,
        websocket: Any,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
    ) -> tuple[bool, str]:
        outcome = self.accept_result_and_reconcile(
            websocket=websocket,
            envelope=envelope,
            result=result,
            raw_payload=result.to_dict(),
        )
        return outcome.accepted, outcome.reason

    def accept_result_and_reconcile(
        self,
        *,
        websocket: Any,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
        raw_payload: Mapping[str, Any],
    ) -> ActiveCommandReconciliationOutcome:
        before_snapshot = self.audit_snapshot()
        command = self._command_state.active_command
        if self._active_websocket is not websocket:
            return self._rejected_outcome(
                reason="result_websocket_not_active",
                before_snapshot=before_snapshot,
            )
        if command is None:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_without_active_command",
            )
        if command.generation != self._generation:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_generation_mismatch",
            )
        if envelope.session_id != command.session_id:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_session_mismatch",
            )
        if result.request_id != command.request_id:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_request_mismatch",
            )
        if envelope.correlation_id != command.command_message_id:
            return self._audit_late_or_reject(
                envelope=envelope,
                result=result,
                before_snapshot=before_snapshot,
                fallback_reason="result_correlation_mismatch",
            )

        next_state, outcome = self._reconciliation.accept_and_apply(
            state=self._command_state,
            expected_active=command,
            envelope=envelope,
            result=result,
            raw_payload=raw_payload,
            before_snapshot=before_snapshot,
            monotonic_ms=self._now_ms(),
        )
        if self._command_state.active_command is not command:
            return self._rejected_outcome(
                reason="result_active_cas_failed",
                before_snapshot=before_snapshot,
            )
        self._command_state = next_state
        return replace(outcome, after_snapshot=self.audit_snapshot())

    def wire_snapshot(self) -> dict[str, Any]:
        return self._snapshot(
            last_result=self._command_state.last_java_result,
            include_local=False,
            include_audit=False,
        )

    def local_admission_snapshot(self) -> dict[str, Any]:
        return self._snapshot(
            last_result=self._command_state.last_java_result,
            include_local=True,
            include_audit=False,
        )

    def audit_snapshot(self) -> dict[str, Any]:
        return self._snapshot(
            last_result=self._command_state.last_java_result,
            include_local=True,
            include_audit=True,
        )

    def snapshot(self) -> dict[str, Any]:
        return self.local_admission_snapshot()

    def _snapshot(
        self,
        *,
        last_result: dict[str, Any] | None,
        include_local: bool,
        include_audit: bool,
    ) -> dict[str, Any]:
        command = self._command_state.active_command
        active_age_ms = (
            None
            if command is None or command.started_at_ms <= 0
            else max(0, self._now_ms() - command.started_at_ms)
        )
        snapshot = {
            "active_session_id": self._active_session_id,
            "active_generation": self.active_generation,
            "active_request_id": None if command is None else command.request_id,
            "active_command_message_id": (
                None if command is None else command.command_message_id
            ),
            "active_command": None if command is None else command.command,
            "active_command_source": None if command is None else command.source,
            "active_started_at_ms": None if command is None else command.started_at_ms,
            "active_age_ms": active_age_ms,
            "last_result": last_result,
        }
        if include_local:
            snapshot.update(
                {
                    "last_java_result": self._command_state.last_java_result,
                    "local_effective_result": (
                        self._command_state.local_effective_result
                    ),
                    "admission_quarantine": (
                        self._command_state.quarantine.to_dict()
                    ),
                    "reconciliation_feature_gate": (
                        self._reconciliation.feature_gate.to_dict()
                    ),
                }
            )
        if include_audit:
            snapshot.update(
                {
                    "candidate": (
                        None
                        if self._command_state.candidate is None
                        else {
                            "first_sequence": (
                                self._command_state.candidate.first_sequence
                            ),
                            "first_message_id": (
                                self._command_state.candidate.first_message_id
                            ),
                        }
                    ),
                    "tombstones": self._command_state.tombstones.to_list(
                        self._now_ms()
                    ),
                }
            )
        return snapshot

    def _audit_late_or_reject(
        self,
        *,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
        before_snapshot: dict[str, Any],
        fallback_reason: str,
    ) -> ActiveCommandReconciliationOutcome:
        next_state, late_outcome = self._reconciliation.audit_late_result(
            state=self._command_state,
            envelope=envelope,
            result=result,
            before_snapshot=before_snapshot,
            monotonic_ms=self._now_ms(),
        )
        if late_outcome is None:
            return self._rejected_outcome(
                reason=fallback_reason,
                before_snapshot=before_snapshot,
            )
        self._command_state = next_state
        return replace(late_outcome, after_snapshot=self.audit_snapshot())

    def _rejected_outcome(
        self,
        *,
        reason: str,
        before_snapshot: dict[str, Any],
    ) -> ActiveCommandReconciliationOutcome:
        return ActiveCommandReconciliationOutcome(
            accepted=False,
            reason=reason,
            before_snapshot=before_snapshot,
            after_snapshot=self.audit_snapshot(),
            audit={
                "event": "command_result_rejected",
                "reason": reason,
                "active_release_performed": False,
            },
        )

    def _now_ms(self) -> int:
        return int(time.time() * 1000)
