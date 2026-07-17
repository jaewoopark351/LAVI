#20260717_kpopmodder: Compatibility facade for game runtime context classes.
from app_core.extensions.runtime_context_core.game_runtime_context import (
    GameRuntimeContext,
)
from app_core.extensions.runtime_context_core.game_runtime_context_registry import (
    GameRuntimeContextRegistry,
)

__all__ = [
    "GameRuntimeContext",
    "GameRuntimeContextRegistry",
]
