#20260715_kpopmodder: Added shared runtime context objects for game extensions.
from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any, Dict, Optional

from .game_extension_contracts import GameCommandDTO, GameResultDTO


@dataclass
class GameRuntimeContext:
    name: str
    initialized: bool = False
    started: bool = False
    last_command: Dict[str, Any] = field(default_factory=dict)
    last_result: Dict[str, Any] = field(default_factory=dict)
    status: Dict[str, Any] = field(default_factory=dict)
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

    def snapshot(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "initialized": self.initialized,
            "started": self.started,
            "last_command": dict(self.last_command),
            "last_result": dict(self.last_result),
            "status": dict(self.status),
            "runtime_error": self.runtime_error,
            "started_at": self.started_at,
            "stopped_at": self.stopped_at,
        }


class GameRuntimeContextRegistry:
    #20260715_kpopmodder: One registry prevents AppComposer from owning per-game runtime dicts.
    def __init__(self):
        self._contexts: Dict[str, GameRuntimeContext] = {}

    @staticmethod
    def _key(name: str) -> str:
        return str(name or "").strip().lower()

    def get(self, name: str) -> GameRuntimeContext:
        key = self._key(name)
        if key not in self._contexts:
            self._contexts[key] = GameRuntimeContext(name=key)
        return self._contexts[key]

    def snapshot(self) -> Dict[str, Dict[str, Any]]:
        return {name: context.snapshot() for name, context in self._contexts.items()}

