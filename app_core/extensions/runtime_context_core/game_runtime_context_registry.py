#20260717_kpopmodder: Isolates runtime context lookup and snapshot behavior.
from typing import Any, Dict

from .game_runtime_context import GameRuntimeContext


class GameRuntimeContextRegistry:
    #20260715_kpopmodder: One registry prevents AppComposer from owning per-game runtime dicts.
    def __init__(self):
        self._contexts: Dict[str, GameRuntimeContext] = {}

    @staticmethod
    def _key(name: str) -> str:
        return str(name or "").strip().lower()

    def get(self, name: str) -> GameRuntimeContext:
        key = self._key(name)
        if key not in self._contexts:
            self._contexts[key] = GameRuntimeContext(name=key)
        return self._contexts[key]

    def snapshot(self) -> Dict[str, Dict[str, Any]]:
        return {name: context.snapshot() for name, context in self._contexts.items()}
