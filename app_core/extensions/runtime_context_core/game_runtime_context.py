#20260717_kpopmodder: Isolates per-game runtime state snapshots from the registry.
import time
from dataclasses import dataclass, field
from typing import Any, Dict, Optional

from app_core.extensions.contracts_core.game_command_dto import GameCommandDTO
from app_core.extensions.contracts_core.game_result_dto import GameResultDTO


@dataclass
class GameRuntimeContext:
    name: str
    initialized: bool = False
    started: bool = False
    last_command: Dict[str, Any] = field(default_factory=dict)
    last_result: Dict[str, Any] = field(default_factory=dict)
    status: Dict[str, Any] = field(default_factory=dict)
    resources: Dict[str, Any] = field(default_factory=dict)
    runtime_error: Optional[str] = None
    started_at: Optional[float] = None
    stopped_at: Optional[float] = None

    def mark_initialized(self, value: bool = True) -> None:
        self.initialized = bool(value)

    def mark_started(self, value: bool = True) -> None:
        self.started = bool(value)
        if self.started:
            self.started_at = time.time()
            self.stopped_at = None
        else:
            self.stopped_at = time.time()

    def set_command(self, command: Any) -> GameCommandDTO:
        dto = GameCommandDTO.from_mapping(command)
        self.last_command = dto.to_legacy_dict()
        return dto

    def set_result(self, result: Any, action: str = "") -> GameResultDTO:
        dto = GameResultDTO.from_mapping(result, action=action)
        self.last_result = dto.to_dict()
        self.runtime_error = dto.error
        return dto

    def set_status(self, status: Dict[str, Any] | None = None) -> None:
        self.status = dict(status or {}) if isinstance(status, dict) else {}

    def set_resource(self, key: str, value: Any) -> None:
        name = str(key or "").strip()
        if not name:
            return
        if value is None:
            self.resources.pop(name, None)
            return
        self.resources[name] = value

    def get_resource(self, key: str, default: Any = None) -> Any:
        return self.resources.get(str(key or "").strip(), default)

    def clear_resource(self, key: str) -> None:
        self.resources.pop(str(key or "").strip(), None)

    def snapshot(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "initialized": self.initialized,
            "started": self.started,
            "last_command": dict(self.last_command),
            "last_result": dict(self.last_result),
            "status": dict(self.status),
            "resources": self._resource_snapshot(),
            "runtime_error": self.runtime_error,
            "started_at": self.started_at,
            "stopped_at": self.stopped_at,
        }

    def _resource_snapshot(self) -> Dict[str, Any]:
        summary = {}
        for key, value in self.resources.items():
            summary[key] = {
                "present": value is not None,
                "type": value.__class__.__name__ if value is not None else None,
            }
        return summary
