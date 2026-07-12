#20260707_kpopmodder: Added stable dict-returning contract for StarCraft2 bot engines.
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict, Optional


class StarCraft2EngineInterface(ABC):
    engine_name = "unknown"

    @abstractmethod
    def start(self, config: Dict[str, Any], event_callback=None) -> Dict[str, Any]:
        raise NotImplementedError

    @abstractmethod
    def stop(self) -> Dict[str, Any]:
        raise NotImplementedError

    def shutdown(self) -> Dict[str, Any]:
        return self.stop()

    @abstractmethod
    def get_status(self) -> Dict[str, Any]:
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
    ) -> Dict[str, Any]:
        return {
            "ok": bool(ok),
            "engine": self.engine_name,
            "running": self.is_running() if running is None else bool(running),
            "status": status if isinstance(status, dict) else {},
            "error": None if error is None else str(error),
        }


class InvalidStarCraft2Engine(StarCraft2EngineInterface):
    #20260707_kpopmodder: Return safe errors for bad engine names without crashing UI startup.
    def __init__(self, requested_name: str, valid_names):
        self.engine_name = str(requested_name or "unknown")
        self.valid_names = list(valid_names or [])

    def start(self, config: Dict[str, Any], event_callback=None) -> Dict[str, Any]:
        return self._error()

    def stop(self) -> Dict[str, Any]:
        return self._error()

    def shutdown(self) -> Dict[str, Any]:
        return self._error()

    def get_status(self) -> Dict[str, Any]:
        return {
            "requested_engine": self.engine_name,
            "valid_engines": list(self.valid_names),
        }

    def is_running(self) -> bool:
        return False

    def _error(self) -> Dict[str, Any]:
        return self._result(
            ok=False,
            running=False,
            status=self.get_status(),
            error="unknown_engine",
        )

