#20260831_kpopmodder: Keep one immutable contract per automatic-deposit matrix row.
from __future__ import annotations

from dataclasses import dataclass

from .evidence_requirement import AutomaticDepositEvidenceRequirement
from .transport_mode import AutomaticDepositTransportMode


@dataclass(frozen=True, slots=True)
class AutomaticDepositMatrixRow:
    row_id: str
    title: str
    transport_mode: AutomaticDepositTransportMode
    required_fixture_fields: tuple[str, ...]
    evidence_requirements: tuple[AutomaticDepositEvidenceRequirement, ...]
    expected_fixture_values: tuple[tuple[str, str], ...] = ()
    expected_operator_action: str = ""
    exercises_frozen_handoff: bool = False
