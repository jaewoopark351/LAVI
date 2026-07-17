#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260628_kpopmodder: Keep legacy DTO names as compatibility aliases to shared contracts.
from __future__ import annotations

from typing import Any, Dict

from .starcraft2_contracts import (
    LocalMatchLaunchConfigDTO,
    LocalMatchRuntimeStatusDTO,
    StartResultDTO,
    StopResultDTO,
)

from .starcraft2_command_result import StarCraft2CommandResult

class StarCraft2LocalMatchStatus(LocalMatchRuntimeStatusDTO):
    """Compatibility DTO alias for legacy local-match status rendering."""

    @staticmethod
    def _normalize_result(value: Any) -> Dict[str, Any]:
        if isinstance(value, StarCraft2CommandResult):
            return value.to_dict()
        if isinstance(value, dict):
            return dict(value)
        if value is None:
            return {}
        return {"ok": bool(value)}
