#20260706_kpopmodder: Added a tiny registry for incremental migration from direct plugin wiring.
from __future__ import annotations

from typing import Any, Dict, Iterable, List, Optional

from core.logger import log_print
from .game_extension_interface import GameExtensionInterface


class ExtensionRegistry:
    """
    Name-based registry for game extensions.
    """

    def __init__(self):
        self._extensions: Dict[str, GameExtensionInterface] = {}

    @staticmethod
    def _normalize_name(name: str) -> str:
        return (name or "").strip().lower()

    def register(self, extension: GameExtensionInterface) -> None:
        if not isinstance(extension, GameExtensionInterface):
            raise TypeError(
                "extension must implement GameExtensionInterface"
            )
        name = self._normalize_name(extension.name)
        if not name:
            raise ValueError("extension.name must be a non-empty string")
        if name in self._extensions:
            log_print(
                f"[GameExtension] overwrite existing registration: {name}"
            )
        self._extensions[name] = extension

    def unregister(self, name: str) -> None:
        key = self._normalize_name(name)
        if key in self._extensions:
            del self._extensions[key]

    def get(self, name: str) -> Optional[GameExtensionInterface]:
        return self._extensions.get(self._normalize_name(name))

    def all(self) -> List[GameExtensionInterface]:
        return list(self._extensions.values())

    def names(self) -> List[str]:
        return list(self._extensions.keys())

    def register_many(self, extensions: Iterable[GameExtensionInterface]) -> None:
        for extension in extensions:
            self.register(extension)

    def initialize(self, context: Any) -> None:
        for extension in self.all():
            try:
                extension.initialize(context)
            except Exception as e:
                log_print(
                    "[GameExtension] initialize failed "
                    f"({extension.name}): {type(e).__name__}: {e}"
                )

    def start_all(self) -> None:
        for extension in self.all():
            try:
                extension.start()
            except Exception as e:
                log_print(
                    "[GameExtension] start failed "
                    f"({extension.name}): {type(e).__name__}: {e}"
                )

    def stop_all(self) -> None:
        for extension in reversed(self.all()):
            try:
                extension.stop()
            except Exception as e:
                log_print(
                    "[GameExtension] stop failed "
                    f"({extension.name}): {type(e).__name__}: {e}"
                )

