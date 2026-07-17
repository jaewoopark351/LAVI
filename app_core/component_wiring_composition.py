#20260717_kpopmodder: Keeps AppComposer focused on assembly order while this service owns component wiring rules.
from dataclasses import dataclass
from typing import Any, Tuple


@dataclass(frozen=True)
class ManagedComponentWiringResult:
    #20260717_kpopmodder: Typed result for RuntimeLifecycle component lists.
    managed_components: Tuple[Any, ...]
    core_components: Tuple[Any, ...]
    optional_components: Tuple[Any, ...]
    startup_components: Tuple[Any, ...] = ()


class AppComponentWiringService:
    #20260717_kpopmodder: Cross-component callback wiring belongs here, not in AppComposer startup flow.
    def wire_optional_plugin_callbacks(self, *, starcraft_plugin=None, screen_vision=None):
        if starcraft_plugin is None:
            return
        starcraft_plugin.set_screen_observation_provider(
            lambda: getattr(screen_vision, "last_screen_observation", "")
            if screen_vision is not None
            else ""
        )

    def wire_event_listeners(
        self,
        *,
        input_component,
        llm,
        translate,
        tts,
        vtuber,
        song_player=None,
        starcraft_plugin=None,
        screen_vision=None,
        screen_vision_input_callback=None,
    ):
        input_component.add_output_event_listener(llm.receive_input)
        llm.add_output_event_listener(translate.receive_input)
        translate.add_output_event_listener(tts.receive_input)
        tts.add_output_event_listener(vtuber.receive_input)
        if song_player is not None:
            song_player.add_output_event_listener(vtuber.receive_input)
            song_player.add_expression_event_listener(
                vtuber.receive_song_expression
            )
        if starcraft_plugin is not None:
            starcraft_plugin.add_output_event_listener(llm.receive_input)
            llm.add_output_event_listener(
                starcraft_plugin.receive_coach_response,
                full_response=True,
            )

        if screen_vision is not None and screen_vision_input_callback is not None:
            screen_vision.add_output_event_listener(screen_vision_input_callback)

    def build_managed_components(
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
