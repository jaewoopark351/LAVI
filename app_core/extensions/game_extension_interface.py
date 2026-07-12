#20260706_kpopmodder: Added common contract for game extension adapters.
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict

from app_core.extensions.game_extension_context import GameExtensionContext


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
