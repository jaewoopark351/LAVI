#20260905_kpopmodder: Builds immutable lifecycle component collections without owning event wiring.
from app_core.composition_core.managed_component_wiring_result import (
    ManagedComponentWiringResult,
)


class ManagedComponentCollectionService:
    def build(
        self,
        *,
        input_component,
        llm,
        translate,
        tts,
        vtuber,
        screen_vision=None,
        song_player=None,
        starcraft_plugin=None,
        game_extension_registry=None,
        optional_components=(),
    ):
        managed_components = [
            input_component,
            llm,
            translate,
            tts,
            vtuber,
        ]
        core_components = [
            input_component,
            llm,
            translate,
            tts,
            vtuber,
        ]
        optional_components = list(optional_components or ())
        startup_components = []

        if screen_vision is not None:
            managed_components.insert(0, screen_vision)
        if song_player is not None:
            managed_components.insert(-1, song_player)
        if starcraft_plugin is not None:
            managed_components.insert(-1, starcraft_plugin)
        if (
            game_extension_registry is not None
            and callable(getattr(game_extension_registry, "all", None))
            and game_extension_registry.all()
        ):
            optional_components.append(game_extension_registry)
            startup_components.append(game_extension_registry)
            managed_components.insert(-1, game_extension_registry)

        return ManagedComponentWiringResult(
            managed_components=tuple(managed_components),
            core_components=tuple(core_components),
            optional_components=tuple(optional_components),
            startup_components=tuple(startup_components),
        )


__all__ = ["ManagedComponentCollectionService"]
