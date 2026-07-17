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

class StarCraft2EngineInterface(ABC):
    engine_name = "unknown"
    # Legacy subclasses remain adapter-backed until each engine is migrated.
    uses_engine_dto_contract = False

    @abstractmethod
    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback: Optional[Callable[[StarCraft2Event], None]] = None,
    ) -> EngineResultDTO:
        raise NotImplementedError

    @abstractmethod
    def stop(self) -> EngineResultDTO:
        raise NotImplementedError

    def shutdown(self) -> EngineResultDTO:
        return self.stop()

    @abstractmethod
    def get_status(self) -> EngineStatusDTO:
        raise NotImplementedError

    @abstractmethod
    def is_running(self) -> bool:
        raise NotImplementedError

    def _result(
        self,
        ok: bool,
        running: Optional[bool] = None,
        status: Optional[Dict[str, Any]] = None,
        error: Optional[Any] = None,
        stopped: bool = False,
    ) -> EngineResultDTO | Dict[str, Any]:
        normalized_running = self.is_running() if running is None else bool(running)
        status_dto = EngineStatusDTO.from_mapping(status, engine=self.engine_name)
        result = EngineResultDTO(
            ok=bool(ok),
            engine=self.engine_name,
            running=normalized_running,
            status=EngineStatusDTO(
                engine=status_dto.engine,
                running=normalized_running,
                status={**status_dto.status, "running": normalized_running},
                error=status_dto.error,
            ),
            error=None if error is None else str(error),
            stopped=bool(stopped),
        )
        if self.uses_engine_dto_contract:
            return result
        return result.to_dict()
