#20260901_kpopmodder: Keep the cleanup admission result in one contract file.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.cleanup_target_plan import (
    CleanupTargetPlan,
)


@dataclass(frozen=True)
class CleanupAdmissionDecision:
    cleanup_allowed: bool
    primary_allowed: bool
    reason: str
    cleanup_plan: CleanupTargetPlan | None = None
