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
        dto = LocalMatchLaunchConfigDTO.from_mapping(value)
        return cls(
            executable_path=dto.executable_path,
            working_directory=dto.working_directory,
            args=dto.args,
            proxy_ports=list(dto.proxy_ports),
            bot_name=dto.bot_name,
            ai_race=dto.ai_race,
            human_race=dto.human_race,
            capture_output=dto.capture_output,
            keep_local_match_identity_args=dto.keep_local_match_identity_args,
            starcraft2_exe_path=dto.starcraft2_exe_path,
            starcraft2_support64_path=dto.starcraft2_support64_path,
            starcraft2_base_path=dto.starcraft2_base_path,
            check_hosts=list(dto.check_hosts),
        )


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
