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

class LegacyStarCraft2EngineAdapter(StarCraft2EngineInterface):
    """Convert an existing dict-based engine to the typed public contract."""

    uses_engine_dto_contract = True

    def __init__(self, engine: Any):
        self._engine = engine
        self.engine_name = str(getattr(engine, "engine_name", "unknown"))

    def __getattr__(self, name: str) -> Any:
        return getattr(self._engine, name)

    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback: Optional[Callable[[StarCraft2Event], None]] = None,
    ) -> EngineResultDTO:
        normalized = EngineStartCommandDTO.from_mapping(command)
        result = self._engine.start(normalized.to_dict(), event_callback=event_callback)
        return EngineResultDTO.from_mapping(result, engine=self.engine_name)

    def stop(self) -> EngineResultDTO:
        result = self._engine.stop()
        return EngineResultDTO.from_mapping(result, engine=self.engine_name)

    def shutdown(self) -> EngineResultDTO:
        shutdown = getattr(self._engine, "shutdown", None)
        result = shutdown() if callable(shutdown) else self._engine.stop()
        return EngineResultDTO.from_mapping(result, engine=self.engine_name)

    def get_status(self) -> EngineStatusDTO:
        return EngineStatusDTO.from_mapping(
            self._engine.get_status(),
            engine=self.engine_name,
        )

    def is_running(self) -> bool:
        return bool(self._engine.is_running())
