#20260706_kpopmodder: Added common contract for game extension adapters.
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.game_extension_contracts import (
    GameCommandDTO,
    GameResultDTO,
    GameStatusDTO,
)


class GameExtensionInterface(ABC):
    """
    Common protocol for game-specific extensions.

    New game features should implement this interface and be added via
    ExtensionRegistry to keep plugin wiring centralized.
    """

    @property
    @abstractmethod
    def name(self) -> str:
        """Unique game extension name."""
        raise NotImplementedError

    def initialize(self, context: GameExtensionContext) -> None:
        """
        Receive shared runtime resources.
        Existing core components can be passed through `context` as a dict.
        """
        self.context = context
        self.runtime_context = None
        self.event_bus = getattr(context, "event_bus", None)
        get_runtime_context = getattr(context, "get_runtime_context", None)
        if callable(get_runtime_context):
            self.runtime_context = get_runtime_context(self.name)
            marker = getattr(self.runtime_context, "mark_initialized", None)
            if callable(marker):
                marker(True)

    def normalize_command(self, command: Any) -> GameCommandDTO:
        return GameCommandDTO.from_mapping(command)

    def record_command(self, command: Any) -> GameCommandDTO:
        runtime_context = getattr(self, "runtime_context", None)
        setter = getattr(runtime_context, "set_command", None)
        if callable(setter):
            return setter(command)
        return self.normalize_command(command)

    def record_result(self, result: Any, action: str = "") -> GameResultDTO:
        runtime_context = getattr(self, "runtime_context", None)
        setter = getattr(runtime_context, "set_result", None)
        if callable(setter):
            return setter(result, action=action)
        return GameResultDTO.from_mapping(result, action=action)

    def record_status(self, status: Dict[str, Any]) -> GameStatusDTO:
        runtime_context = getattr(self, "runtime_context", None)
        setter = getattr(runtime_context, "set_status", None)
        if callable(setter):
            setter(status)
        return GameStatusDTO.from_mapping(status, name=self.name)

    def apply_status_contract(self, status: Dict[str, Any]) -> Dict[str, Any]:
        payload = dict(status or {})
        dto = self.record_status(payload)
        payload.setdefault("name", dto.name)
        payload.setdefault("initialized", dto.initialized)
        payload.setdefault("started", dto.started)
        if dto.plugin:
            payload.setdefault("plugin", dict(dto.plugin))
        if dto.worker:
            payload.setdefault("worker", dict(dto.worker))
        if dto.runtime:
            payload.setdefault("runtime", dict(dto.runtime))
        if dto.details:
            payload.setdefault("details", dict(dto.details))
        if dto.error is not None:
            payload.setdefault("error", dto.error)
        payload["game_status"] = dto.to_dict()
        return payload

    def mark_started(self, value: bool = True) -> None:
        runtime_context = getattr(self, "runtime_context", None)
        marker = getattr(runtime_context, "mark_started", None)
        if callable(marker):
            marker(value)

    def publish_event(self, event_type: str, details: Dict[str, Any] | None = None) -> bool:
        event_bus = getattr(self, "event_bus", None)
        emitter = getattr(event_bus, "emit", None)
        if not callable(emitter):
            return False
        return bool(
            emitter(
                {
                    "event_type": event_type,
                    "game": self.name,
                    "source": "game_extension",
                    "details": dict(details or {}),
                }
            )
        )

    @abstractmethod
    def start(self) -> None:
        """Start background worker loops, listeners, and external bridges."""

    @abstractmethod
    def stop(self) -> None:
        """Stop workers and release background resources."""

    @abstractmethod
    def handle_command(self, command: Any) -> Any:
        """
        Handle game command payload from user input or bridge events.
        Return result if meaningful, or None.
        """
        raise NotImplementedError

    @abstractmethod
    def get_status(self) -> Dict[str, Any]:
        """
        Return health/state payload for UI, logs, or debugging.
        """
        raise NotImplementedError
