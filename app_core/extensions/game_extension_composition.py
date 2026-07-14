#20260715_kpopmodder: Extract game extension composition out of AppComposer.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable, List, Optional

from core.logger import log_print


@dataclass
class GameExtensionCompositionResult:
    starcraft116_game_extension: Any = None
    starcraft2_game_extension: Any = None
    starcraft2_changeling_observer_extension: Any = None
    chess_game_extension: Any = None
    registered_extensions: List[Any] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)


class GameExtensionCompositionService:
    #20260715_kpopmodder: AppComposer asks for composition; this service owns imports and registration details.
    def __init__(self, registry, logger: Callable[[str], None] = log_print):
        self.registry = registry
        self.logger = logger

    def compose(
        self,
        context,
        starcraft116_plugin=None,
        starcraft2_plugin=None,
        chess_plugin=None,
        starcraft116_game_extension=None,
        starcraft2_game_extension=None,
        starcraft2_changeling_observer_extension=None,
        chess_game_extension=None,
    ) -> GameExtensionCompositionResult:
        result = GameExtensionCompositionResult(
            starcraft116_game_extension=starcraft116_game_extension,
            starcraft2_game_extension=starcraft2_game_extension,
            starcraft2_changeling_observer_extension=starcraft2_changeling_observer_extension,
            chess_game_extension=chess_game_extension,
        )

        result.starcraft116_game_extension = self._register_starcraft116(
            starcraft116_plugin,
            result.starcraft116_game_extension,
            result,
        )
        result.starcraft2_game_extension = self._register_starcraft2(
            starcraft2_plugin,
            result.starcraft2_game_extension,
            result,
        )
        result.starcraft2_changeling_observer_extension = self._register_starcraft2_observer(
            result.starcraft2_changeling_observer_extension,
            result,
        )
        result.chess_game_extension = self._register_chess(
            chess_plugin,
            result.chess_game_extension,
            result,
        )

        if result.registered_extensions:
            self.registry.initialize(context)
        self._log_registry_lookups(result)
        return result

    def _register_starcraft116(self, plugin, existing, result):
        if plugin is None or existing is not None:
            return existing
        try:
            from app_core.extensions.starcraft116_game_extension import (
                StarCraft116GameExtension,
            )

            extension = StarCraft116GameExtension(plugin=plugin)
            self.registry.register(extension)
            result.registered_extensions.append(extension)
            return extension
        except Exception as e:
            self._record_error(
                result,
                "register StarCraft116GameExtension failed",
                e,
            )
            return None

    def _register_starcraft2(self, plugin, existing, result):
        if plugin is None or existing is not None:
            return existing
        try:
            from app_core.extensions.starcraft2_game_extension import (
                StarCraft2GameExtension,
            )

            extension = StarCraft2GameExtension(plugin=plugin)
            self.registry.register(extension)
            result.registered_extensions.append(extension)
            self.logger("[AppComposer] starcraft2 game extension registered")
            return extension
        except Exception as e:
            self._record_error(
                result,
                "register StarCraft2GameExtension failed",
                e,
            )
            return None

    def _register_starcraft2_observer(self, existing, result):
        if existing is not None:
            return existing
        try:
            from plugins.StarCraft2.starcraft2_core.sc2_extension import (
                StarCraft2Extension,
            )

            extension = StarCraft2Extension()
            self.registry.register(extension)
            result.registered_extensions.append(extension)
            self.logger("[AppComposer] starcraft2 Changeling observer extension registered")
            return extension
        except Exception as e:
            self._record_error(
                result,
                "register StarCraft2 Changeling observer failed",
                e,
            )
            return None

    def _register_chess(self, plugin, existing, result):
        if plugin is None or existing is not None:
            return existing
        try:
            from app_core.extensions.chess_game_extension import ChessGameExtension

            extension = ChessGameExtension(plugin=plugin)
            self.registry.register(extension)
            result.registered_extensions.append(extension)
            self.logger("[AppComposer] chess game extension registered")
            return extension
        except Exception as e:
            self._record_error(
                result,
                "register ChessGameExtension failed",
                e,
            )
            return None

    def _record_error(self, result, label: str, error: Exception) -> None:
        message = f"[AppComposer] {label}: {type(error).__name__}: {error}"
        result.errors.append(message)
        self.logger(message)

    def _log_registry_lookups(self, result: GameExtensionCompositionResult) -> None:
        self._log_lookup(result.starcraft116_game_extension, "starcraft116")
        self._log_lookup(result.starcraft2_game_extension, "starcraft2")
        self._log_lookup(
            result.starcraft2_changeling_observer_extension,
            "starcraft2_changeling_observer",
        )
        self._log_lookup(result.chess_game_extension, "chess")

    def _log_lookup(self, extension, name: str) -> None:
        if extension is None:
            return
        lookup = self.registry.get(name) is not None
        self.logger(f"[AppComposer] registry lookup {name}: {lookup}")

