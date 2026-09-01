#20260901_kpopmodder: Keep the targeted cleanup command plan in one contract file.
from dataclasses import dataclass


@dataclass(frozen=True)
class CleanupTargetPlan:
    command: str
    target: str
    count: int
    source_slot: str = ""
