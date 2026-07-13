#20260628_kpopmodder: Keep legacy DTO names as compatibility aliases to shared contracts.
from __future__ import annotations

from typing import Any, Dict

from .starcraft2_contracts import (
    LocalMatchLaunchConfigDTO,
    LocalMatchRuntimeStatusDTO,
    StartResultDTO,
    StopResultDTO,
)


class StarCraft2LocalMatchCommand(LocalMatchLaunchConfigDTO):
    """Compatibility DTO alias for legacy local-match callers."""

    @classmethod
    def from_mapping(cls, value: Any) -> "StarCraft2LocalMatchCommand":
        return cls(**LocalMatchLaunchConfigDTO.from_mapping(value).to_dict())


class StarCraft2CommandResult(StartResultDTO):
    """Compatibility DTO alias for legacy local-match callers."""

    @classmethod
    def from_mapping(cls, value: Any, action: str = "local_match") -> "StarCraft2CommandResult":
        payload = StartResultDTO.from_mapping(value, action=action).to_dict()
        return cls(**payload)


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

