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

class StarCraft2CommandResult(StartResultDTO):
    """Compatibility DTO alias for legacy local-match callers."""

    @classmethod
    def from_mapping(cls, value: Any, action: str = "local_match") -> "StarCraft2CommandResult":
        payload = StartResultDTO.from_mapping(value, action=action).to_dict()
        return cls(**payload)
