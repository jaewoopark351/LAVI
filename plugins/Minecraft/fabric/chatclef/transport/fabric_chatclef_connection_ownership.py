#20260905_kpopmodder: Preserve the legacy connection-ownership API as a focused delegation facade.
from __future__ import annotations

import time
from typing import Any, Mapping

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus

from .fabric_chatclef_active_command import FabricChatClefActiveCommand
from .command_feedback.crafting import CraftingFeedbackTracker
from .ownership import (
    FabricChatClefConnectionSession,
    FabricChatClefOrdinaryCommandOwner,
    FabricChatClefOwnershipSnapshotBuilder,
)
from .reconciliation.active_command_reconciliation_coordinator import (
    ActiveCommandReconciliationCoordinator,
)
from .reconciliation.active_command_reconciliation_outcome import (
    ActiveCommandReconciliationOutcome,
)
from .reconciliation.reconciliation_feature_gate import ReconciliationFeatureGate
from .reconciliation.reconciliation_runtime_capability import (
    ReconciliationRuntimeCapability,
)


class FabricChatClefConnectionOwnership:
    TERMINAL_STATUSES = frozenset(
        {
            CommandResultStatus.COMPLETED,
            CommandResultStatus.REJECTED,
            CommandResultStatus.FAILED,
            CommandResultStatus.CANCELLED,
            CommandResultStatus.DEADLINE_EXCEEDED,
            CommandResultStatus.UNKNOWN,
        }
    )

    def __init__(
        self,
        *,
        reconcile_stale_deposit_to_unknown_requested: bool = False,
        reconciliation_runtime_capability: (
            ReconciliationRuntimeCapability | None
        ) = None,
        crafting_feedback_tracker: CraftingFeedbackTracker | None = None,
    ) -> None:
        self._connection_session = FabricChatClefConnectionSession()
        self._command_owner = FabricChatClefOrdinaryCommandOwner(
            reconciliation=ActiveCommandReconciliationCoordinator(
                feature_gate=ReconciliationFeatureGate(
                    requested_enabled=reconcile_stale_deposit_to_unknown_requested,
                    runtime_capability=(
                        reconciliation_runtime_capability
                        or ReconciliationRuntimeCapability.production_blocked()
                    ),
                ),
            ),
            now_ms=lambda: self._now_ms(),
        )
        self._snapshot_builder = FabricChatClefOwnershipSnapshotBuilder(
            connection_session=self._connection_session,
            command_owner=self._command_owner,
            now_ms=lambda: self._now_ms(),
        )
        self._crafting_feedback_tracker = (
            crafting_feedback_tracker or CraftingFeedbackTracker()
        )

    @property
    def active_websocket(self) -> Any:
        return self._connection_session.active_websocket

    @property
    def active_session_id(self) -> str | None:
        return self._connection_session.active_session_id

    @property
    def active_generation(self) -> int:
        return self._connection_session.active_generation

    @property
    def active_request_id(self) -> str | None:
        return self._command_owner.active_request_id

    @property
    def active_command_owner(self) -> FabricChatClefActiveCommand | None:
        return self._command_owner.active_command

    @property
    def crafting_feedback_tracker(self) -> CraftingFeedbackTracker:
        return self._crafting_feedback_tracker

    @property
    def command_feedback_tracker(self) -> CraftingFeedbackTracker:
        return self._crafting_feedback_tracker

    @property
    def _reconciliation(self):
        return self._command_owner.reconciliation

    @_reconciliation.setter
    def _reconciliation(self, value) -> None:
        self._command_owner.reconciliation = value

    def is_connected(self) -> bool:
        return self._connection_session.is_connected()

    def is_active_websocket(self, websocket: Any) -> bool:
        return self._connection_session.is_active_websocket(websocket)

    #20260907_kpopmodder: Retire crafting feedback with every connection-owner reset.
    def try_activate(self, *, websocket: Any, session_id: str):
        was_connected = self._connection_session.is_connected()
        admission = self._connection_session.try_activate(
            websocket=websocket,
            session_id=session_id,
        )
        if admission.accepted and not was_connected:
            self._command_owner.clear()
            self._crafting_feedback_tracker.clear()
        return admission

    def clear_if_active(self, *, websocket: Any, session_id: str | None) -> bool:
        cleared = self._connection_session.clear_if_active(
            websocket=websocket,
            session_id=session_id,
        )
        if cleared:
            self._command_owner.clear()
            self._crafting_feedback_tracker.clear()
        return cleared

    def clear(self) -> None:
        self._connection_session.clear()
        self._command_owner.clear()
        self._crafting_feedback_tracker.clear()

    def begin_command(
        self,
        *,
        request_id: str,
        command_message_id: str,
        command: str = "",
        source: str = "",
        metadata: Mapping[str, Any] | None = None,
    ) -> FabricChatClefActiveCommand | None:
        active_command = self._command_owner.begin(
            websocket=self.active_websocket,
            session_id=self.active_session_id,
            generation=self.active_generation,
            request_id=request_id,
            command_message_id=command_message_id,
            command=command,
            source=source,
        )
        if active_command is not None:
            #20260907_kpopmodder: Bind the reserved feedback owner in the command ownership commit.
            self._crafting_feedback_tracker.bind_reserved(
                active_command,
                metadata or {},
            )
        return active_command

    #20260907_kpopmodder: Clear only feedback bound to the ordinary owner being cleared.
    def clear_command_if_current(self, command: FabricChatClefActiveCommand) -> bool:
        cleared = self._command_owner.clear_if_current(command)
        if cleared:
            self._crafting_feedback_tracker.clear_if_owner(command)
        return cleared

    def clear_command_if_identity(
        self,
        *,
        request_id: str,
        command_message_id: str,
        session_id: str,
        server_connection_generation: int,
        owner_token: object,
    ) -> bool:
        command = self._command_owner.active_command
        cleared = self._command_owner.clear_if_identity(
            request_id=request_id,
            command_message_id=command_message_id,
            session_id=session_id,
            server_connection_generation=server_connection_generation,
            owner_token=owner_token,
        )
        if cleared and command is not None:
            self._crafting_feedback_tracker.clear_if_owner(command)
        return cleared

    #20260907_kpopmodder: Expose tracker operations only under the server command lock.
    def reserve_crafting_feedback(self, grant: object) -> bool:
        return self.reserve_command_feedback(grant)

    def abandon_crafting_feedback(self, grant: object) -> bool:
        return self.abandon_command_feedback(grant)

    def claim_crafting_feedback_start(
        self,
        grant: object,
        result: object,
    ):
        return self.claim_command_feedback_start(grant, result)

    def inspect_crafting_feedback_status(self, target_item: str | None):
        return self.inspect_command_feedback_status(target_item=target_item)

    def inspect_crafting_feedback_status_for_publication(
        self,
        target_item: str | None,
    ):
        return self.inspect_command_feedback_status_for_publication(
            target_item=target_item
        )

    def reserve_command_feedback(self, grant: object) -> bool:
        return self._crafting_feedback_tracker.reserve(grant)

    def abandon_command_feedback(self, grant: object) -> bool:
        return self._crafting_feedback_tracker.abandon_reservation(grant)

    def claim_command_feedback_start(self, grant: object, result: object):
        return self._crafting_feedback_tracker.claim_start(grant, result)

    def inspect_command_feedback_status(
        self,
        *,
        query: object = None,
        target_item: str | None = None,
    ):
        return self._crafting_feedback_tracker.inspect(
            active_command=self._command_owner.active_command,
            connected=self._connection_session.is_connected(),
            quarantine_active=self._command_owner.state.quarantine.active,
            query=query,
            target_item=target_item,
        )

    def inspect_command_feedback_status_for_publication(
        self,
        *,
        query: object = None,
        target_item: str | None = None,
    ):
        return self._crafting_feedback_tracker.inspect_for_publication(
            active_command=self._command_owner.active_command,
            connected=self._connection_session.is_connected(),
            quarantine_active=self._command_owner.state.quarantine.active,
            query=query,
            target_item=target_item,
        )

    def acknowledge_crafting_feedback_publication(
        self,
        permit: object,
        published: bool,
    ):
        return self.acknowledge_command_feedback_publication(
            permit,
            published,
        )

    def acknowledge_command_feedback_publication(
        self,
        permit: object,
        published: bool,
    ):
        return self._crafting_feedback_tracker.acknowledge_publication(
            permit,
            published,
        )

    def select_command_feedback_coalesced_terminal(self, permit: object):
        return self._crafting_feedback_tracker.select_coalesced_terminal(
            permit
        )

    def select_crafting_feedback_coalesced_terminal(self, permit: object):
        return self.select_command_feedback_coalesced_terminal(permit)

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
        return self._command_owner.accept_result_and_reconcile(
            websocket=websocket,
            active_websocket=self.active_websocket,
            connection_generation=self.active_generation,
            envelope=envelope,
            result=result,
            raw_payload=raw_payload,
            before_snapshot=self.audit_snapshot(),
            after_snapshot_provider=self.audit_snapshot,
        )

    def wire_snapshot(self) -> dict[str, Any]:
        return self._snapshot_builder.wire_snapshot()

    def local_admission_snapshot(self) -> dict[str, Any]:
        return self._snapshot_builder.local_admission_snapshot()

    def audit_snapshot(self) -> dict[str, Any]:
        return self._snapshot_builder.audit_snapshot()

    def snapshot(self) -> dict[str, Any]:
        return self.local_admission_snapshot()

    @staticmethod
    def _now_ms() -> int:
        return int(time.time() * 1000)
