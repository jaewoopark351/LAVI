#20260821_kpopmodder: Swap command reconciliation state atomically instead of mutating several stores.
from __future__ import annotations

from dataclasses import dataclass, field, replace
from typing import Any

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)

from .active_command_admission_quarantine import ActiveCommandAdmissionQuarantine
from .active_command_tombstone_store import ActiveCommandTombstoneStore
from .reconciliation_candidate import ReconciliationCandidate


@dataclass(frozen=True)
class CommandReconciliationState:
    active_command: FabricChatClefActiveCommand | None = None
    last_java_result: dict[str, Any] | None = None
    local_effective_result: dict[str, Any] | None = None
    candidate: ReconciliationCandidate | None = None
    tombstones: ActiveCommandTombstoneStore = field(
        default_factory=ActiveCommandTombstoneStore
    )
    quarantine: ActiveCommandAdmissionQuarantine = field(
        default_factory=ActiveCommandAdmissionQuarantine.inactive
    )

    def with_active(
        self,
        command: FabricChatClefActiveCommand,
    ) -> "CommandReconciliationState":
        return replace(self, active_command=command, candidate=None)

    def without_active(self) -> "CommandReconciliationState":
        return replace(self, active_command=None, candidate=None)

    def with_java_result(
        self,
        result: CommandResultDTO,
        *,
        local_effective: bool,
        candidate: ReconciliationCandidate | None = None,
    ) -> "CommandReconciliationState":
        result_dict = result.to_dict()
        return replace(
            self,
            last_java_result=result_dict,
            local_effective_result=(
                result_dict if local_effective else self.local_effective_result
            ),
            candidate=candidate,
        )

    def with_reconciled_unknown(
        self,
        *,
        synthetic_result: CommandResultDTO,
        tombstones: ActiveCommandTombstoneStore,
        quarantine: ActiveCommandAdmissionQuarantine,
    ) -> "CommandReconciliationState":
        return replace(
            self,
            active_command=None,
            local_effective_result=synthetic_result.to_dict(),
            candidate=None,
            tombstones=tombstones,
            quarantine=quarantine,
        )

    def with_tombstones(
        self,
        tombstones: ActiveCommandTombstoneStore,
    ) -> "CommandReconciliationState":
        return replace(self, tombstones=tombstones)
