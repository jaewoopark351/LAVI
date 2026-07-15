#20260715_kpopmodder: Promote the StarCraft2 engine boundary to typed DTO contracts.
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Callable, Dict, Optional

from .starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
)


class StarCraft2EngineInterface(ABC):
    engine_name = "unknown"
    # Legacy subclasses remain adapter-backed until each engine is migrated.
    uses_engine_dto_contract = False

    @abstractmethod
    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback: Optional[Callable[[Dict[str, Any]], None]] = None,
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


#20260715_kpopmodder: Keep unmigrated dict engines working behind the DTO boundary.
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
        event_callback: Optional[Callable[[Dict[str, Any]], None]] = None,
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


def adapt_starcraft2_engine(engine: Any) -> StarCraft2EngineInterface:
    if bool(getattr(engine, "uses_engine_dto_contract", False)):
        return engine
    return LegacyStarCraft2EngineAdapter(engine)


class InvalidStarCraft2Engine(StarCraft2EngineInterface):
    #20260715_kpopmodder: Preserve direct registry callers until invalid-engine APIs migrate.
    uses_engine_dto_contract = False
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
