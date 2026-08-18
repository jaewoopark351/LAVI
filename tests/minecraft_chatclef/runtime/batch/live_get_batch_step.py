#20260819_kpopmodder: Represent one immutable approved GET batch step.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class LiveGetBatchStep:
    command: str
    expected_item_id: str
    expected_item_delta: int
    gameplay_test_objective: str
