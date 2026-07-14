#20260706_kpopmodder: Added shared extension package for incremental game extension migration.
"""Shared helpers for game extension migration."""

from .extension_registry import ExtensionRegistry
from .game_event_bus import GameEventBus, GameEventDTO
from .game_extension_composition import (
    GameExtensionCompositionResult,
    GameExtensionCompositionService,
)
from .game_extension_context import GameExtensionContext
from .game_extension_contracts import (
    GameCommandDTO,
    GameResultDTO,
    GameStartResultDTO,
    GameStatusDTO,
    GameStopResultDTO,
)
from .game_extension_interface import GameExtensionInterface
from .game_runtime_context import GameRuntimeContext, GameRuntimeContextRegistry

__all__ = [
    "ExtensionRegistry",
    "GameCommandDTO",
    "GameEventBus",
    "GameEventDTO",
    "GameExtensionCompositionResult",
    "GameExtensionCompositionService",
    "GameExtensionContext",
    "GameExtensionInterface",
    "GameResultDTO",
    "GameRuntimeContext",
    "GameRuntimeContextRegistry",
    "GameStartResultDTO",
    "GameStatusDTO",
    "GameStopResultDTO",
]

