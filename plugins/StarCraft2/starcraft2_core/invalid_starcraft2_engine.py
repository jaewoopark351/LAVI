#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260715_kpopmodder: Promote the StarCraft2 engine boundary to typed DTO contracts.
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Callable, Dict, Optional

from .starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
    StarCraft2Event,
)

from .starcraft2_engine_interface_base import StarCraft2EngineInterface

class InvalidStarCraft2Engine(StarCraft2EngineInterface):
    #20260715_kpopmodder: Preserve direct registry callers until invalid-engine APIs migrate.
    uses_engine_dto_contract = True
    #20260707_kpopmodder: Return safe errors for bad engine names without crashing UI startup.
    def __init__(self, requested_name: str, valid_names):
        self.engine_name = str(requested_name or "unknown")
        self.valid_names = list(valid_names or [])

    def start(self, command: EngineStartCommandDTO, event_callback=None) -> EngineResultDTO:
        return self._error()

    def stop(self) -> EngineResultDTO:
        return self._error()

    def shutdown(self) -> EngineResultDTO:
        return self._error()

    def get_status(self) -> EngineStatusDTO:
        return EngineStatusDTO(
            engine=self.engine_name,
            running=False,
            status={
                "requested_engine": self.engine_name,
                "valid_engines": list(self.valid_names),
                "running": False,
            },
            error="unknown_engine",
        )

    def is_running(self) -> bool:
        return False

    def _error(self) -> EngineResultDTO:
        return self._result(
            ok=False,
            running=False,
            status=self.get_status().to_dict(),
            error="unknown_engine",
        )
