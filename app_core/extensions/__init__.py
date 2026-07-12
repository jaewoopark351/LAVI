#20260706_kpopmodder: Added shared extension package for incremental game extension migration.
"""Shared helpers for game extension migration."""

from .extension_registry import ExtensionRegistry
from .game_extension_context import GameExtensionContext
from .game_extension_interface import GameExtensionInterface

__all__ = [
    "ExtensionRegistry",
    "GameExtensionContext",
    "GameExtensionInterface",
]

