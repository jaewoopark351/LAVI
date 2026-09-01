#20260831_kpopmodder: Bind operator-confirmed world and inventory setup to one row.
from __future__ import annotations

from dataclasses import dataclass

from .carry_on_identity import AutomaticDepositCarryOnIdentity


@dataclass(frozen=True, slots=True)
class AutomaticDepositFixtureManifest:
    schema_version: str
    row_id: str
    fixture_id: str
    fixture_fingerprint: str
    world_snapshot_id: str
    inventory_snapshot_id: str
    operator_confirmed: bool
    carry_on_identity: AutomaticDepositCarryOnIdentity
    container_type: str = ""
    trusted_destination_fingerprint: str = ""
    diagnostics_mode: str = ""
    attributes: tuple[tuple[str, str], ...] = ()

    def as_evidence_mapping(self) -> dict[str, object]:
        evidence: dict[str, object] = {
            "fixture_fingerprint": self.fixture_fingerprint,
            "world_snapshot_id": self.world_snapshot_id,
            "inventory_snapshot_id": self.inventory_snapshot_id,
            "operator_confirmed": self.operator_confirmed,
            "carry_on_state": self.carry_on_identity.presence.value,
            "container_type": self.container_type,
            "trusted_destination_fingerprint": (
                self.trusted_destination_fingerprint
            ),
            "diagnostics_mode": self.diagnostics_mode,
        }
        for key, value in self.attributes:
            if key not in evidence:
                evidence[key] = value
        return evidence
